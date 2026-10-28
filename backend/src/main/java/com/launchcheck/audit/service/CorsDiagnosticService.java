package com.launchcheck.audit.service;

import com.launchcheck.audit.http.HttpExchangeResult;
import com.launchcheck.audit.http.PinnedHttpRequester;
import com.launchcheck.audit.safety.TargetUrl;
import com.launchcheck.audit.safety.UrlSafetyPolicy;
import com.launchcheck.audit.api.CorsDiagnosticResult;
import com.launchcheck.audit.api.CorsDiagnosticResult.CorsResponseHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CorsDiagnosticService {
    private static final long DEFAULT_CONNECT_TIMEOUT_MS = 3_000;
    private static final long DEFAULT_READ_TIMEOUT_MS = 3_000;
    private static final long DEFAULT_MAX_RESPONSE_BYTES = 64 * 1024;

    private final PinnedHttpRequester requester;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public CorsDiagnosticService(UrlSafetyPolicy policy) {
        this.requester = new PinnedHttpRequester(policy);
    }

    /**
     * Performs a CORS diagnostic on the given target URL.
     * Validates the endpoint path is relative and constrained to the backend origin.
     * Performs a safe OPTIONS probe with an Origin header.
     * Returns CorsDiagnosticResult with header details and honest limitations.
     */
    public CorsDiagnosticResult diagnose(TargetUrl target, String origin, String endpointPath) {
        // 1. Validate endpoint path is relative and constrained to backend origin
        if (endpointPath == null || endpointPath.isEmpty() || !endpointPath.startsWith("/")) {
            return new CorsDiagnosticResult(
                    endpointPath,
                    "OPTIONS",
                    origin,
                    "FAIL",
                    "Endpoint path must be a relative path starting with '/'",
                    false,
                    null
            );
        }

        // Revalidate via existing safety policy - ensure path doesn't escape origin
        try {
            if (!endpointPath.startsWith("/")) {
                throw new IllegalArgumentException("Endpoint path must start with '/'");
            }
        } catch (Exception e) {
            return new CorsDiagnosticResult(
                    endpointPath,
                    "OPTIONS",
                    origin,
                    "FAIL",
                    "Invalid endpoint path: " + e.getMessage(),
                    false,
                    null
            );
        }

        // 2. Perform safe OPTIONS probe with Origin and preflight request headers
        TargetUrl requestTarget = new TargetUrl(target.scheme(), target.host(), target.port(), endpointPath);
        HttpExchangeResult optionsResult = requester.execute(
                requestTarget,
                "OPTIONS",
                List.of(
                        "Origin", origin,
                        "Access-Control-Request-Method", "GET",
                        "Access-Control-Request-Headers", "content-type"
                )
        );

        String status;
        String diagnostic;
        Map<String, String> responseHeaders = new HashMap<>();
        boolean preflightAttempted = true;

        if ("PASS".equals(optionsResult.status())) {
            // Extract relevant CORS headers from the response
            // Note: responseHeaders in HttpExchangeResult is Map<String, List<String>>
            // We convert to Map<String, String> for CorsDiagnosticResult
            Map<String, List<String>> rawHeaders = optionsResult.headers();
            logger.debug("Raw headers received: {}", rawHeaders);
            for (Map.Entry<String, List<String>> entry : rawHeaders.entrySet()) {
                String key = entry.getKey();
                logger.debug("Checking header: {}", key);
                // Try case-insensitive matching manually to be safe
                if (key.equalsIgnoreCase("Access-Control-Allow-Origin") ||
                    key.equalsIgnoreCase("Access-Control-Allow-Methods") ||
                    key.equalsIgnoreCase("Access-Control-Allow-Headers") ||
                    key.equalsIgnoreCase("Vary")) {
                    List<String> values = entry.getValue();
                    if (values != null && !values.isEmpty()) {
                        // Store with normalized key
                        String normalizedKey = key.toLowerCase(Locale.ROOT);
                        responseHeaders.put(normalizedKey, values.get(0));
                        logger.debug("Header mapped: {} = {}", normalizedKey, values.get(0));
                    }
                }
            }

            String allowOrigin = responseHeaders.get("access-control-allow-origin");
            String allowMethods = responseHeaders.get("access-control-allow-methods");
            String allowHeaders = responseHeaders.get("access-control-allow-headers");
            boolean originAllowed = "*".equals(allowOrigin) || origin.equals(allowOrigin);
            boolean methodAllowed = containsToken(allowMethods, "GET");
            boolean requestedHeadersAllowed = containsToken(allowHeaders, "content-type");

            if (originAllowed && methodAllowed && requestedHeadersAllowed) {
                status = "PASS";
                diagnostic = "CORS preflight successful. Origin, GET, and requested headers are allowed.";
            } else {
                status = "WARN";
                diagnostic = "OPTIONS request succeeded but CORS policy is incomplete: "
                        + "originAllowed=" + originAllowed
                        + ", methodAllowed=" + methodAllowed
                        + ", requestedHeadersAllowed=" + requestedHeadersAllowed;
            }
        } else {
            status = "FAIL";
            diagnostic = "CORS preflight failed: " + optionsResult.diagnostic();
        }

        // 3. Return result with observed headers
        CorsResponseHeaders corsHeaders = null;
        String acao = responseHeaders.get("access-control-allow-origin");

        if (acao != null) {
            corsHeaders = new CorsResponseHeaders(
                    acao,
                    responseHeaders.get("access-control-allow-methods"),
                    responseHeaders.get("access-control-allow-headers"),
                    responseHeaders.get("access-control-allow-credentials"),
                    responseHeaders.get("vary")
            );
        }

        return new CorsDiagnosticResult(
                endpointPath,
                "OPTIONS",
                origin,
                status,
                diagnostic,
                preflightAttempted,
                corsHeaders
        );
    }

    private static boolean containsToken(String headerValue, String expectedToken) {
        if (headerValue == null || expectedToken == null) return false;
        for (String token : headerValue.split(",")) {
            if (expectedToken.equalsIgnoreCase(token.trim())) return true;
        }
        return false;
    }
}