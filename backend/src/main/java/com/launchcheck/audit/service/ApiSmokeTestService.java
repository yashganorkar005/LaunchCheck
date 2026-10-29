package com.launchcheck.audit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.launchcheck.audit.api.ApiSmokeTestResult;
import com.launchcheck.audit.api.AuditRequest.ApiCheck;
import com.launchcheck.audit.api.AuditRequest.HeaderAssertion;
import com.launchcheck.audit.api.AuditRequest.JsonAssertion;
import com.launchcheck.audit.http.HttpExchangeResult;
import com.launchcheck.audit.http.PinnedHttpRequester;
import com.launchcheck.audit.safety.TargetUrl;
import com.launchcheck.audit.safety.UrlSafetyPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        Map<String, String> headerMap = check.headerAssertions() != null ?
                check.headerAssertions().stream().filter(a -> a.name() != null && !a.name().isEmpty()).collect(Collectors.toMap(a -> a.name(), a -> a.value() == null ? "" : a.value(), (v1, v2) -> v1)) :
                Map.of();

        List<String> headers = new ArrayList<>();
        for (Map.Entry<String, String> entry : headerMap.entrySet()) {
            headers.add(entry.getKey());
            headers.add(entry.getValue());
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
                    if (matchedVal == null || !matchedVal.contains(ha.value())) {
                        headerPass = false;
                        headerDiag = "Header '" + ha.name() + "' value mismatch: expected substring '" + ha.value() + "', got '" + matchedVal + "'";
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
