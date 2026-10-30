package com.launchcheck.audit.service;

import com.launchcheck.audit.api.ApiSmokeTestResult;
import com.launchcheck.audit.api.AuditRequest.ApiCheck;
import com.launchcheck.audit.api.AuditRequest.HeaderAssertion;
import com.launchcheck.audit.api.AuditRequest.HeaderMatchCondition;
import com.launchcheck.audit.api.AuditRequest.JsonAssertion;
import com.launchcheck.audit.api.CorsDiagnosticResult;
import com.launchcheck.audit.safety.TargetUrl;
import com.launchcheck.audit.safety.UrlSafetyPolicy;
import com.sun.net.httpserver.HttpServer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Phase2ServiceTest {

    @Test
    void corsDiagnosticServiceEvaluatesHeadersCorrectly() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        server.createContext("/api/data", exchange -> {
            String origin = exchange.getRequestHeaders().getFirst("Origin");
            if ("https://allowed.example.com".equals(origin)) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "https://allowed.example.com");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
                exchange.getResponseHeaders().set("Vary", "Origin");
            }
            exchange.sendResponseHeaders(200, -1);
            exchange.getResponseBody().close();
        });
        server.createContext("/api/v1/data", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.getResponseBody().close();
        });
        server.start();

        try {
            int port = server.getAddress().getPort();
            UrlSafetyPolicy policy = new UrlSafetyPolicy(addr -> true, host -> List.of(InetAddress.getByName("127.0.0.1")));
            CorsDiagnosticService service = new CorsDiagnosticService(policy);
            TargetUrl target = new TargetUrl("http", "localhost", port, "/");

            // Test allowed origin
            CorsDiagnosticResult passResult = service.diagnose(target, "https://allowed.example.com", "/api/data");
            assertEquals("PASS", passResult.status());
            assertNotNull(passResult.responseHeaders());
            assertEquals("https://allowed.example.com", passResult.responseHeaders().allowOrigin());
            assertNull(passResult.responseHeaders().allowCredentials());

            // Test OPTIONS method and actual CORS preflight header evaluation
            assertEquals("OPTIONS", passResult.method());
            assertTrue(passResult.diagnostic().contains("requested headers are allowed"));

            // Test rejected origin (missing ACAO header)
            CorsDiagnosticResult warnResult = service.diagnose(target, "https://unallowed.example.com", "/api/data");
            assertEquals("WARN", warnResult.status());
            assertNull(warnResult.responseHeaders());

            // Test 404 endpoint response on /api/v1/data
            CorsDiagnosticResult notFoundResult = service.diagnose(target, "https://allowed.example.com", "/api/v1/data");
            assertEquals("FAIL", notFoundResult.status());
            assertTrue(notFoundResult.diagnostic().contains("404") || notFoundResult.diagnostic().contains("failed"));

            // Test invalid endpoint path (must start with '/')
            CorsDiagnosticResult invalidPathResult = service.diagnose(target, "https://allowed.example.com", "api/data");
            assertEquals("FAIL", invalidPathResult.status());
            assertTrue(invalidPathResult.diagnostic().contains("relative path starting with '/'"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void smokeTestSendsDefaultHeadersAndPassesAssertions() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        server.createContext("/posts/1", exchange -> {
            assertEquals("LaunchCheck/0.1", exchange.getRequestHeaders().getFirst("User-Agent"));
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] bytes = "{\"id\":1,\"title\":\"fixture\"}".getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().close();
        });
        server.start();

        try {
            int port = server.getAddress().getPort();
            UrlSafetyPolicy policy = new UrlSafetyPolicy(addr -> true, host -> List.of(InetAddress.getByName("127.0.0.1")));
            ApiSmokeTestService service = new ApiSmokeTestService(policy);
            ApiCheck check = new ApiCheck("/posts/1", "GET", 200, List.of(), List.of(new JsonAssertion("/id", "1")));

            ApiSmokeTestResult result = service.runTests(new TargetUrl("http", "jsonplaceholder.typicode.com", port, "/"), List.of(check)).get(0);

            assertEquals("PASS", result.status());
            assertTrue(result.jsonAssertionPassed());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void headerAssertionsSupportEveryComparisonConditionAndDefaultSafely() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        server.createContext("/headers", exchange -> {
            exchange.getResponseHeaders().set("X-Release", "release/v1.2.3");
            exchange.sendResponseHeaders(200, -1);
            exchange.getResponseBody().close();
        });
        server.start();

        try {
            int port = server.getAddress().getPort();
            UrlSafetyPolicy policy = new UrlSafetyPolicy(addr -> true, host -> List.of(InetAddress.getByName("127.0.0.1")));
            ApiSmokeTestService service = new ApiSmokeTestService(policy);
            TargetUrl target = new TargetUrl("http", "headers.example.test", port, "/");

            List<ApiCheck> passingChecks = List.of(
                    new ApiCheck("/headers", "GET", 200, List.of(new HeaderAssertion("x-release", "lease/v1", HeaderMatchCondition.CONTAINS)), List.of()),
                    new ApiCheck("/headers", "GET", 200, List.of(new HeaderAssertion("X-Release", "release/v1.2.3", HeaderMatchCondition.EQUALS)), List.of()),
                    new ApiCheck("/headers", "GET", 200, List.of(new HeaderAssertion("X-RELEASE", "release/", HeaderMatchCondition.STARTS_WITH)), List.of()),
                    new ApiCheck("/headers", "GET", 200, List.of(new HeaderAssertion("x-release", ".2.3", HeaderMatchCondition.ENDS_WITH)), List.of())
            );
            assertEquals(List.of("PASS", "PASS", "PASS", "PASS"), service.runTests(target, passingChecks).stream().map(ApiSmokeTestResult::status).toList());

            List<ApiCheck> failingChecks = List.of(
                    new ApiCheck("/headers", "GET", 200, List.of(new HeaderAssertion("x-release", "hotfix", HeaderMatchCondition.CONTAINS)), List.of()),
                    new ApiCheck("/headers", "GET", 200, List.of(new HeaderAssertion("x-release", "release/v1.2", HeaderMatchCondition.EQUALS)), List.of()),
                    new ApiCheck("/headers", "GET", 200, List.of(new HeaderAssertion("x-release", "hotfix/", HeaderMatchCondition.STARTS_WITH)), List.of()),
                    new ApiCheck("/headers", "GET", 200, List.of(new HeaderAssertion("x-release", ".9", HeaderMatchCondition.ENDS_WITH)), List.of())
            );
            assertEquals(List.of("FAIL", "FAIL", "FAIL", "FAIL"), service.runTests(target, failingChecks).stream().map(ApiSmokeTestResult::status).toList());

            ApiCheck legacyCheck = new ApiCheck("/headers", "GET", 200, List.of(new HeaderAssertion("x-release", "lease/v1")), List.of());
            assertEquals("PASS", service.runTests(target, List.of(legacyCheck)).get(0).status());
            ApiCheck nullConditionCheck = new ApiCheck("/headers", "GET", 200, List.of(new HeaderAssertion("x-release", "lease/v1", null)), List.of());
            assertEquals("PASS", service.runTests(target, List.of(nullConditionCheck)).get(0).status());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void smokeTestDoesNotSendAuthorizationHeader() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        server.createContext("/posts/1", exchange -> {
            assertNull(exchange.getRequestHeaders().getFirst("Authorization"));
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] bytes = "{\"id\":1}".getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().close();
        });
        server.start();

        try {
            int port = server.getAddress().getPort();
            UrlSafetyPolicy policy = new UrlSafetyPolicy(addr -> true, host -> List.of(InetAddress.getByName("127.0.0.1")));
            ApiSmokeTestService service = new ApiSmokeTestService(policy);
            ApiCheck check = new ApiCheck("/posts/1", "GET", 200, List.of(), List.of(new JsonAssertion("/id", "1")));

            ApiSmokeTestResult result = service.runTests(
                    new TargetUrl("http", "jsonplaceholder.typicode.com", port, "/"),
                    List.of(check)
            ).get(0);

            assertEquals("PASS", result.status());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void smokeTestDoesNotSendAuthorizationHeaderToAnyTarget() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        server.createContext("/health", exchange -> {
            assertNull(exchange.getRequestHeaders().getFirst("Authorization"));
            exchange.sendResponseHeaders(200, -1);
            exchange.getResponseBody().close();
        });
        server.start();

        try {
            int port = server.getAddress().getPort();
            UrlSafetyPolicy policy = new UrlSafetyPolicy(addr -> true, host -> List.of(InetAddress.getByName("127.0.0.1")));
            ApiSmokeTestService service = new ApiSmokeTestService(policy);
            ApiCheck check = new ApiCheck("/health", "GET", 200, List.of(), List.of());

            ApiSmokeTestResult result = service.runTests(
                    new TargetUrl("http", "unrelated.example.test", port, "/"),
                    List.of(check)
            ).get(0);

            assertEquals("PASS", result.status());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void primaryRateLimit403IncludesSafeDiagnosticsAndSkipsJsonAssertion() throws Exception {
        ApiSmokeTestResult result = runRateLimit403Fixture("{\"message\":\"API rate limit exceeded\",\"token\":\"secret-token\"}",
                Map.of("X-RateLimit-Limit", "60", "X-RateLimit-Remaining", "0", "X-RateLimit-Reset", "1700000000"));

        assertEquals(403, result.actualStatus());
        assertEquals("FAIL", result.status());
        assertFalse(result.jsonAssertionPassed());
        assertTrue(result.jsonAssertionDiagnostic().contains("skipped"));
        assertTrue(result.diagnostic().contains("rate limit"));
        assertTrue(result.diagnostic().contains("X-RateLimit-Remaining=0"));
        assertFalse(result.diagnostic().contains("secret-token"));
    }

    @Test
    void secondaryRateLimit403IncludesRetryDiagnostics() throws Exception {
        ApiSmokeTestResult result = runRateLimit403Fixture("{\"message\":\"You have exceeded a secondary rate limit.\"}",
                Map.of("Retry-After", "30", "X-Request-Id", "req-123"));

        assertEquals(403, result.actualStatus());
        assertTrue(result.diagnostic().contains("secondary rate limit"));
        assertTrue(result.diagnostic().contains("Retry-After=30"));
        assertTrue(result.diagnostic().contains("X-Request-Id=req-123"));
    }

    @Test
    void ordinary403RetainsHttpFailureWithoutRateLimitClassification() throws Exception {
        ApiSmokeTestResult result = runRateLimit403Fixture("{\"message\":\"Resource forbidden\"}", Map.of());

        assertEquals(403, result.actualStatus());
        assertTrue(result.diagnostic().contains("HTTP response 403"));
        assertFalse(result.diagnostic().contains("rate limit"));
        assertTrue(result.jsonAssertionDiagnostic().contains("skipped"));
    }

    private ApiSmokeTestResult runRateLimit403Fixture(String body, Map<String, String> responseHeaders) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        server.createContext("/posts/1", exchange -> {
            responseHeaders.forEach((name, value) -> exchange.getResponseHeaders().set(name, value));
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] bytes = body.getBytes();
            exchange.sendResponseHeaders(403, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().close();
        });
        server.start();
        try {
            int port = server.getAddress().getPort();
            UrlSafetyPolicy policy = new UrlSafetyPolicy(addr -> true, host -> List.of(InetAddress.getByName("127.0.0.1")));
            ApiSmokeTestService service = new ApiSmokeTestService(policy);
            ApiCheck check = new ApiCheck("/posts/1", "GET", 200, List.of(), List.of(new JsonAssertion("/full_name", "octocat/Spoon-Knife")));
            return service.runTests(new TargetUrl("http", "jsonplaceholder.typicode.com", port, "/"), List.of(check)).get(0);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void apiSmokeTestUsesBackendBasePathWhenCombiningRelativeCheckPath() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        server.createContext("/api/posts/1", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] bytes = "{\"id\":1}".getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().close();
        });
        server.start();

        try {
            int port = server.getAddress().getPort();
            UrlSafetyPolicy policy = new UrlSafetyPolicy(addr -> true, host -> List.of(InetAddress.getByName("127.0.0.1")));
            ApiSmokeTestService service = new ApiSmokeTestService(policy);
            TargetUrl backendTarget = new TargetUrl("http", "localhost", port, "/api");
            ApiCheck sampleCheck = new ApiCheck(
                    "/posts/1",
                    "GET",
                    200,
                    List.of(),
                    List.of(new JsonAssertion("/id", "1"))
            );

            ApiSmokeTestResult result = service.runTests(backendTarget, List.of(sampleCheck)).get(0);

            assertEquals("PASS", result.status());
            assertEquals(200, result.actualStatus());
            assertTrue(result.jsonAssertionPassed());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void apiSmokeTestServiceEnforcesLimitsAndAssertions() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        server.createContext("/health", exchange -> {
            exchange.getResponseHeaders().set("X-Custom-Header", "Value123");
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            String body = "{\"status\":\"UP\",\"code\":200,\"validId\":99}";
            byte[] bytes = body.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().close();
        });
        server.createContext("/escaped", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            String body = "{\"a/b\":10,\"c~d\":20}";
            byte[] bytes = body.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().close();
        });
        server.createContext("/malformed", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            String body = "{malformed-json";
            byte[] bytes = body.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().close();
        });
        server.createContext("/posts/1", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] bytes = "{\"id\":1,\"title\":\"fixture\"}".getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().close();
        });
        server.createContext("/posts/1/comments", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] bytes = "[{\"postId\":1,\"id\":1}]".getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().close();
        });
        server.createContext("/status/404", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.getResponseBody().close();
        });
        server.start();

        try {
            int port = server.getAddress().getPort();
            UrlSafetyPolicy policy = new UrlSafetyPolicy(addr -> true, host -> List.of(InetAddress.getByName("127.0.0.1")));
            ApiSmokeTestService service = new ApiSmokeTestService(policy);
            TargetUrl target = new TargetUrl("http", "localhost", port, "/");

            // Test matching JSON assertion: HTTP 200 and configured assertion both pass
            ApiCheck matchingJsonCheck = new ApiCheck(
                    "/health",
                    "GET",
                    200,
                    List.of(),
                    List.of(new com.launchcheck.audit.api.AuditRequest.JsonAssertion("/status", "UP"))
            );
            List<ApiSmokeTestResult> matchingJsonResults = service.runTests(target, List.of(matchingJsonCheck));
            assertEquals(1, matchingJsonResults.size());
            assertEquals("PASS", matchingJsonResults.get(0).status());
            assertEquals(200, matchingJsonResults.get(0).actualStatus());
            assertTrue(matchingJsonResults.get(0).jsonAssertionPassed());
            assertTrue(matchingJsonResults.get(0).jsonAssertionDiagnostic().contains("passed"));

            // Test mismatching JSON assertion: HTTP 200 passes, but the smoke test must fail
            ApiCheck mismatchingJsonCheck = new ApiCheck(
                    "/health",
                    "GET",
                    200,
                    List.of(),
                    List.of(new com.launchcheck.audit.api.AuditRequest.JsonAssertion("/code", "201"))
            );
            List<ApiSmokeTestResult> mismatchingJsonResults = service.runTests(target, List.of(mismatchingJsonCheck));
            assertEquals(1, mismatchingJsonResults.size());
            assertEquals("FAIL", mismatchingJsonResults.get(0).status());
            assertEquals(200, mismatchingJsonResults.get(0).actualStatus());
            assertFalse(mismatchingJsonResults.get(0).jsonAssertionPassed());
            assertTrue(mismatchingJsonResults.get(0).diagnostic().contains("Expected JSON value 201; actual value 200"));
            assertTrue(mismatchingJsonResults.get(0).jsonAssertionDiagnostic().contains("Expected JSON value 201; actual value 200"));

            // Test valid smoke test without assertions
            ApiCheck validCheck = new ApiCheck("/health", "GET", 200, List.of(), List.of());
            List<ApiSmokeTestResult> results = service.runTests(target, List.of(validCheck));
            assertEquals(1, results.size());
            assertEquals("PASS", results.get(0).status());
            assertEquals(200, results.get(0).actualStatus());

            // Test status mismatch / intentional mismatch (expecting 200, getting 404)
            ApiCheck mismatchCheck = new ApiCheck("/status/404", "GET", 200, List.of(), List.of());
            List<ApiSmokeTestResult> mismatchResults = service.runTests(target, List.of(mismatchCheck));
            assertEquals(1, mismatchResults.size());
            assertEquals("FAIL", mismatchResults.get(0).status());
            assertEquals(404, mismatchResults.get(0).actualStatus());
            assertTrue(mismatchResults.get(0).diagnostic().contains("Status mismatch"));

            ApiCheck matching404 = new ApiCheck("/status/404", "GET", 404, List.of(), List.of());
            ApiSmokeTestResult matching404Result = service.runTests(target, List.of(matching404)).get(0);
            assertEquals("PASS", matching404Result.status());
            assertEquals(404, matching404Result.actualStatus());

            ApiCheck expected299 = new ApiCheck("/status/404", "GET", 299, List.of(), List.of());
            ApiSmokeTestResult expected299Result = service.runTests(target, List.of(expected299)).get(0);
            assertEquals(Integer.valueOf(299), expected299Result.expectedStatus());
            assertEquals(404, expected299Result.actualStatus());
            assertEquals("FAIL", expected299Result.status());

            ApiCheck matchingId = new ApiCheck("/posts/1", "GET", 200, List.of(), List.of(new JsonAssertion("/id", "1")));
            ApiSmokeTestResult matchingIdResult = service.runTests(target, List.of(matchingId)).get(0);
            assertEquals("PASS", matchingIdResult.status());
            assertTrue(matchingIdResult.jsonAssertionPassed());

            ApiCheck mismatchingId = new ApiCheck("/posts/1", "GET", 200, List.of(), List.of(new JsonAssertion("/id", "2")));
            ApiSmokeTestResult mismatchingIdResult = service.runTests(target, List.of(mismatchingId)).get(0);
            assertEquals(200, mismatchingIdResult.actualStatus());
            assertEquals("FAIL", mismatchingIdResult.status());
            assertFalse(mismatchingIdResult.jsonAssertionPassed());
            assertTrue(mismatchingIdResult.jsonAssertionDiagnostic().contains("actual value 1"));

            ApiCheck nestedJson = new ApiCheck("/posts/1/comments", "GET", 200, List.of(), List.of(new JsonAssertion("/0/postId", "1")));
            ApiSmokeTestResult nestedResult = service.runTests(target, List.of(nestedJson)).get(0);
            assertEquals("PASS", nestedResult.status());
            assertTrue(nestedResult.jsonAssertionPassed());

            ApiCheck headerMismatch = new ApiCheck("/posts/1", "GET", 200, List.of(new HeaderAssertion("Content-Type", "text/plain")), List.of());
            ApiSmokeTestResult headerResult = service.runTests(target, List.of(headerMismatch)).get(0);
            assertEquals(200, headerResult.actualStatus());
            assertEquals("FAIL", headerResult.status());
            assertTrue(headerResult.diagnostic().contains("value mismatch"));

            // Test JSON Pointer escaping (~1 for '/' and ~0 for '~')
            ApiCheck escapedCheck1 = new ApiCheck("/escaped", "GET", 200, List.of(), List.of(new JsonAssertion("/a~1b", "10")));
            ApiSmokeTestResult escapedRes1 = service.runTests(target, List.of(escapedCheck1)).get(0);
            assertEquals("PASS", escapedRes1.status());

            ApiCheck escapedCheck2 = new ApiCheck("/escaped", "GET", 200, List.of(), List.of(new JsonAssertion("/c~0d", "20")));
            ApiSmokeTestResult escapedRes2 = service.runTests(target, List.of(escapedCheck2)).get(0);
            assertEquals("PASS", escapedRes2.status());

            // Test malformed JSON response
            ApiCheck malformedCheck = new ApiCheck("/malformed", "GET", 200, List.of(), List.of(new JsonAssertion("/id", "1")));
            ApiSmokeTestResult malformedRes = service.runTests(target, List.of(malformedCheck)).get(0);
            assertEquals("FAIL", malformedRes.status());
            assertTrue(malformedRes.jsonAssertionDiagnostic().contains("Failed to parse JSON response"));

            // Test missing path
            ApiCheck missingPathCheck = new ApiCheck("/health", "GET", 200, List.of(), List.of(new JsonAssertion("/nonexistent", "1")));
            ApiSmokeTestResult missingRes = service.runTests(target, List.of(missingPathCheck)).get(0);
            assertEquals("FAIL", missingRes.status());
            assertTrue(missingRes.jsonAssertionDiagnostic().contains("not found in response body"));

            // Test similar field name collision avoided
            ApiCheck similarFieldCheck = new ApiCheck("/health", "GET", 200, List.of(), List.of(new JsonAssertion("/validId", "1")));
            ApiSmokeTestResult similarRes = service.runTests(target, List.of(similarFieldCheck)).get(0);
            assertEquals("FAIL", similarRes.status());

            List<ApiCheck> methods = List.of(
                    new ApiCheck("/posts/1", "HEAD", 200, List.of(), List.of()),
                    new ApiCheck("/posts/1", "OPTIONS", 200, List.of(), List.of())
            );
            List<ApiSmokeTestResult> methodResults = service.runTests(target, methods);
            assertEquals(List.of("HEAD", "OPTIONS"), methodResults.stream().map(ApiSmokeTestResult::method).toList());

            // Test transport failure / unreachable endpoint (actualStatus is 0 or null)
            ApiCheck unreachableCheck = new ApiCheck("/nonexistent-endpoint-xyz", "GET", 200, List.of(), List.of());
            TargetUrl deadTarget = new TargetUrl("http", "localhost", 1, "/");
            List<ApiSmokeTestResult> deadResults = service.runTests(deadTarget, List.of(unreachableCheck));
            assertEquals(1, deadResults.size());
            assertEquals("FAIL", deadResults.get(0).status());
            assertTrue(deadResults.get(0).actualStatus() == null || deadResults.get(0).actualStatus() == 0);
            assertTrue(deadResults.get(0).diagnostic().contains("Transport failure") || deadResults.get(0).diagnostic().contains("Connection failed"));

            // Test invalid method restriction
            ApiCheck invalidMethodCheck = new ApiCheck("/health", "POST", 200, List.of(), List.of());
            List<ApiSmokeTestResult> restrictedMethodResults = service.runTests(target, List.of(invalidMethodCheck));
            assertEquals(1, restrictedMethodResults.size());
            assertEquals("FAIL", restrictedMethodResults.get(0).status());
            assertTrue(restrictedMethodResults.get(0).diagnostic().contains("Unsupported method"));

            // Test invalid path restriction
            ApiCheck invalidPathCheck = new ApiCheck("health", "GET", 200, List.of(), List.of());
            List<ApiSmokeTestResult> pathResults = service.runTests(target, List.of(invalidPathCheck));
            assertEquals(1, pathResults.size());
            assertEquals("FAIL", pathResults.get(0).status());
            assertTrue(pathResults.get(0).diagnostic().contains("Invalid path"));

            // Test 5-check limit enforcement
            List<ApiCheck> manyChecks = List.of(
                    new ApiCheck("/health", "GET", 200, List.of(), List.of()),
                    new ApiCheck("/health", "GET", 200, List.of(), List.of()),
                    new ApiCheck("/health", "GET", 200, List.of(), List.of()),
                    new ApiCheck("/health", "GET", 200, List.of(), List.of()),
                    new ApiCheck("/health", "GET", 200, List.of(), List.of()),
                    new ApiCheck("/health", "GET", 200, List.of(), List.of())
            );
            List<ApiSmokeTestResult> limitedResults = service.runTests(target, manyChecks);
            assertEquals(5, limitedResults.size());
        } finally {
            server.stop(0);
        }
    }
}
