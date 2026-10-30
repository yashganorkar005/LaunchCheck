package com.launchcheck.audit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.launchcheck.audit.api.ApiSmokeTestResult;
import com.launchcheck.audit.api.AuditRequest.ApiCheck;
import com.launchcheck.audit.api.AuditRequest.HeaderAssertion;
import com.launchcheck.audit.api.AuditRequest.HeaderMatchCondition;
import com.launchcheck.audit.api.AuditRequest.JsonAssertion;
import com.launchcheck.audit.http.HttpExchangeResult;
import com.launchcheck.audit.http.PinnedHttpRequester;
import com.launchcheck.audit.safety.TargetUrl;
import com.launchcheck.audit.safety.UrlSafetyPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ApiSmokeTestService {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final PinnedHttpRequester requester;

    public ApiSmokeTestService(UrlSafetyPolicy policy) {
        this.requester = new PinnedHttpRequester(policy);
    }

    public List<ApiSmokeTestResult> runTests(TargetUrl target, List<ApiCheck> checks) {
        List<ApiSmokeTestResult> results = new ArrayList<>();
        if (checks == null) return results;

        // Enforce max 5 tests
        int limit = Math.min(checks.size(), 5);
        for (int i = 0; i < limit; i++) {
            results.add(runSingleTest(target, checks.get(i)));
        }
        return results;
    }

    /**
     * Header names are matched case-insensitively during lookup; header values use case-sensitive String comparisons.
     */
    private static boolean matchesHeaderValue(String actual, String expected, HeaderMatchCondition condition) {
        if (actual == null) return false;
        String safeExpected = expected == null ? "" : expected;
        HeaderMatchCondition safeCondition = condition == null ? HeaderMatchCondition.CONTAINS : condition;
        return switch (safeCondition) {
            case CONTAINS -> actual.contains(safeExpected);
            case EQUALS -> actual.equals(safeExpected);
            case STARTS_WITH -> actual.startsWith(safeExpected);
            case ENDS_WITH -> actual.endsWith(safeExpected);
        };
    }

    private static String formatDiagnosticHeaderName(String name) {
        return switch (name.toLowerCase(java.util.Locale.ROOT)) {
            case "x-ratelimit-limit" -> "X-RateLimit-Limit";
            case "x-ratelimit-remaining" -> "X-RateLimit-Remaining";
            case "x-ratelimit-reset" -> "X-RateLimit-Reset";
            case "retry-after" -> "Retry-After";
            case "x-request-id" -> "X-Request-Id";
            default -> name;
        };
    }

    private String buildHttpFailureDiagnostic(HttpExchangeResult result) {
        String body = result.bodyExcerpt() == null ? "" : result.bodyExcerpt();
        String lowerBody = body.toLowerCase(java.util.Locale.ROOT);
        boolean secondaryRateLimit = lowerBody.contains("secondary rate limit") || result.diagnosticHeaders().containsKey("retry-after");
        boolean primaryRateLimit = "0".equals(result.diagnosticHeaders().get("x-ratelimit-remaining"))
                || lowerBody.contains("api rate limit exceeded");

        StringBuilder diagnostic = new StringBuilder("Status mismatch: expected HTTP response ").append(result.httpStatus());
        if (primaryRateLimit) {
            diagnostic.append(" primary rate limit response");
        } else if (secondaryRateLimit) {
            diagnostic.append(" secondary rate limit response");
        } else {
            diagnostic.append(" response");
        }
        if (!body.isBlank()) {
            diagnostic.append("; body: ").append(body);
        }
        if (!result.diagnosticHeaders().isEmpty()) {
            diagnostic.append("; diagnostics: ");
            result.diagnosticHeaders().forEach((name, value) -> diagnostic.append(formatDiagnosticHeaderName(name)).append('=').append(value).append(' '));
        }
        return diagnostic.toString().trim();
    }

    private ApiSmokeTestResult runSingleTest(TargetUrl target, ApiCheck check) {
        // Enforce method restriction
        String method = check.method().toUpperCase();
        if (!List.of("GET", "HEAD", "OPTIONS").contains(method)) {
            return new ApiSmokeTestResult(check.path(), method, check.expectedStatus(), null, "FAIL", "Unsupported method: " + method, "Use GET, HEAD, or OPTIONS", true, null);
        }

        // Enforce path safety
        if (check.path() == null || !check.path().startsWith("/")) {
            return new ApiSmokeTestResult(check.path(), method, check.expectedStatus(), null, "FAIL", "Invalid path", "Path must start with '/'", true, null);
        }

        // Construct request from the configured backend target and relative check path.
        String basePath = target.path() == null || target.path().isBlank() ? "/" : target.path();
        int queryIndex = basePath.indexOf('?');
        if (queryIndex >= 0) {
            basePath = basePath.substring(0, queryIndex);
        }
        String normalizedBasePath = basePath.endsWith("/") ? basePath.substring(0, basePath.length() - 1) : basePath;
        String requestPath = (normalizedBasePath.isEmpty() ? "" : normalizedBasePath) + check.path();
        TargetUrl requestTarget = new TargetUrl(target.scheme(), target.host(), target.port(), requestPath);
        List<String> headers = new ArrayList<>();
        if (check.headerAssertions() != null) {
            for (HeaderAssertion assertion : check.headerAssertions()) {
                if (assertion.name() != null && !assertion.name().isEmpty()) {
                    headers.add(assertion.name());
                    headers.add(assertion.value() == null ? "" : assertion.value());
                }
            }
        }

        HttpExchangeResult result = requester.execute(requestTarget, method, headers);

        // If connection failed or httpStatus is 0, treat as transport failure FAIL rather than status mismatch
        if (!"PASS".equals(result.status()) && result.httpStatus() == 0) {
            return new ApiSmokeTestResult(
                    check.path(),
                    method,
                    check.expectedStatus(),
                    null,
                    "FAIL",
                    "Transport failure / unreachable: " + result.diagnostic(),
                    "Check target reachability and network connection",
                    true,
                    null
            );
        }

        boolean statusMatch = check.expectedStatus() == null || check.expectedStatus().equals(result.httpStatus());
        if (!statusMatch) {
            String diagnostic = buildHttpFailureDiagnostic(result);
            return new ApiSmokeTestResult(
                    check.path(), method, check.expectedStatus(), result.httpStatus(), "FAIL", diagnostic,
                    "Review the HTTP response and any rate-limit diagnostics",
                    false, "JSON assertion skipped because the HTTP status did not match"
            );
        }

        // Evaluate Header Assertions
        boolean headerPass = true;
        String headerDiag = "All header assertions passed";
        if (check.headerAssertions() != null && !check.headerAssertions().isEmpty()) {
            Map<String, List<String>> respHeaders = result.headers();
            for (HeaderAssertion ha : check.headerAssertions()) {
                if (ha.name() == null || ha.name().isEmpty()) continue;
                boolean found = false;
                String matchedVal = null;
                for (Map.Entry<String, List<String>> entry : respHeaders.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase(ha.name())) {
                        found = true;
                        List<String> vals = entry.getValue();
                        if (vals != null && !vals.isEmpty()) {
                            matchedVal = vals.get(0);
                        }
                        break;
                    }
                }
                if (!found) {
                    headerPass = false;
                    headerDiag = "Header '" + ha.name() + "' missing from response";
                    break;
                }
                if (ha.value() != null && !ha.value().isEmpty()) {
                    HeaderMatchCondition condition = ha.condition() == null ? HeaderMatchCondition.CONTAINS : ha.condition();
                    if (!matchesHeaderValue(matchedVal, ha.value(), condition)) {
                        headerPass = false;
                        headerDiag = "Header '" + ha.name() + "' value mismatch: expected " + condition.name().toLowerCase(java.util.Locale.ROOT) + " '" + ha.value() + "', got '" + matchedVal + "'";
                        break;
                    }
                }
            }
        }

        // Evaluate JSON Assertions if present
        boolean jsonPass = true;
        String jsonDiag = "No JSON assertions evaluated";
        if (check.jsonAssertions() != null && !check.jsonAssertions().isEmpty() && result.body() != null) {
            try {
                JsonNode root = objectMapper.readTree(result.body());
                for (JsonAssertion ja : check.jsonAssertions()) {
                    if (ja.jsonPointer() == null || ja.jsonPointer().isEmpty()) continue;
                    String pointer = ja.jsonPointer();
                    String expectedVal = ja.expectedValue();

                    JsonNode node = root.at(pointer);
                    if (node.isMissingNode()) {
                        jsonPass = false;
                        jsonDiag = "JSON pointer " + pointer + " not found in response body";
                        break;
                    }

                    String actualVal = node.isTextual() ? node.asText() : node.toString();
                    if (expectedVal != null && !expectedVal.equals(actualVal)) {
                        jsonPass = false;
                        jsonDiag = "Expected JSON value " + expectedVal + "; actual value " + actualVal;
                        break;
                    } else {
                        jsonDiag = "JSON assertion passed for " + pointer;
                    }
                }
            } catch (Exception e) {
                jsonPass = false;
                jsonDiag = "Failed to parse JSON response: " + e.getMessage();
            }
        }

        boolean overallPass = statusMatch && headerPass && jsonPass;
        String status = overallPass ? "PASS" : "FAIL";

        String diagnostic;
        if (!statusMatch) {
            diagnostic = "Status mismatch: expected " + check.expectedStatus() + ", got " + result.httpStatus();
        } else if (!headerPass) {
            diagnostic = headerDiag;
        } else if (!jsonPass) {
            diagnostic = jsonDiag;
        } else {
            diagnostic = "Test passed successfully";
        }

        return new ApiSmokeTestResult(
                check.path(),
                method,
                check.expectedStatus(),
                result.httpStatus(),
                status,
                diagnostic,
                overallPass ? "No action required" : "Check endpoint assertions or payload",
                jsonPass,
                jsonDiag
        );
    }
}
