package com.launchcheck.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.launchcheck.audit.api.AuditResponse;
import com.launchcheck.audit.api.AuditRequest;
import com.launchcheck.audit.http.HttpProbeResult;
import com.launchcheck.audit.http.PinnedHttpProbe;
import com.launchcheck.audit.safety.TargetUrl;
import com.launchcheck.audit.safety.UrlSafetyPolicy;
import com.sun.net.httpserver.HttpServer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuditExecutionServiceSecurityHeadersTest {
    private static final TargetUrl TARGET = new TargetUrl("https", "example.com", 443, "/");

    @Test
    void mainAuditFlowDoesNotSendAuthorizationToSmokeRequest() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        server.createContext("/posts/1", exchange -> {
            assertEquals(null, exchange.getRequestHeaders().getFirst("Authorization"));
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] body = "{\"id\":1}".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.getResponseBody().close();
        });
        server.start();

        try {
            int port = server.getAddress().getPort();
            UrlSafetyPolicy smokePolicy = new UrlSafetyPolicy(addr -> true, host -> List.of(InetAddress.getByName("127.0.0.1")));
            UrlSafetyPolicy parsePolicy = mock(UrlSafetyPolicy.class);
            PinnedHttpProbe probe = mock(PinnedHttpProbe.class);
            when(parsePolicy.parseAndValidate("https://example.com")).thenReturn(new TargetUrl("http", "example.com", port, "/"));
            when(parsePolicy.parseAndValidate("https://api.example.com")).thenReturn(new TargetUrl("http", "api.example.com", port, "/"));
            when(probe.check(any(TargetUrl.class))).thenReturn(new HttpProbeResult(
                    "PASS", 1, 200, "Responded with HTTP 200",
                    new HttpProbeResult.TlsDetails("SKIPPED", null, "HTTP fixture")
            ));

            ApiSmokeTestService smokeService = new ApiSmokeTestService(smokePolicy);
            AuditResponse response = new AuditExecutionService(parsePolicy, probe, smokeService).executeAudit(
                    "https://example.com",
                    "https://api.example.com",
                    List.of(new AuditRequest.ApiCheck(
                            "/posts/1", "GET", 200, List.of(),
                            List.of(new AuditRequest.JsonAssertion("/id", "1"))
                    ))
            );

            assertEquals("PASS", response.apiTests().get(0).status());
            assertEquals(200, response.apiTests().get(0).actualStatus());
            assertTrue(response.apiTests().get(0).jsonAssertionPassed());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void mainAuditFlowWithoutTokenRetainsPrimaryRateLimitFailure() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        server.createContext("/posts/1", exchange -> {
            assertEquals(null, exchange.getRequestHeaders().getFirst("Authorization"));
            exchange.getResponseHeaders().set("X-RateLimit-Remaining", "0");
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] body = "{\"message\":\"API rate limit exceeded\"}".getBytes();
            exchange.sendResponseHeaders(403, body.length);
            exchange.getResponseBody().write(body);
            exchange.getResponseBody().close();
        });
        server.start();

        try {
            int port = server.getAddress().getPort();
            UrlSafetyPolicy smokePolicy = new UrlSafetyPolicy(addr -> true, host -> List.of(InetAddress.getByName("127.0.0.1")));
            UrlSafetyPolicy parsePolicy = mock(UrlSafetyPolicy.class);
            PinnedHttpProbe probe = mock(PinnedHttpProbe.class);
            when(parsePolicy.parseAndValidate("https://example.com")).thenReturn(new TargetUrl("http", "example.com", port, "/"));
            when(parsePolicy.parseAndValidate("https://api.example.com")).thenReturn(new TargetUrl("http", "api.example.com", port, "/"));
            when(probe.check(any(TargetUrl.class))).thenReturn(new HttpProbeResult(
                    "PASS", 1, 200, "Responded with HTTP 200",
                    new HttpProbeResult.TlsDetails("SKIPPED", null, "HTTP fixture")
            ));

            ApiSmokeTestService smokeService = new ApiSmokeTestService(smokePolicy);
            AuditResponse response = new AuditExecutionService(parsePolicy, probe, smokeService).executeAudit(
                    "https://example.com",
                    "https://api.example.com",
                    List.of(new AuditRequest.ApiCheck(
                            "/posts/1", "GET", 200, List.of(),
                            List.of(new AuditRequest.JsonAssertion("/id", "1"))
                    ))
            );

            assertEquals("FAIL", response.apiTests().get(0).status());
            assertTrue(response.apiTests().get(0).diagnostic().contains("primary rate limit"));
            assertTrue(response.apiTests().get(0).jsonAssertionDiagnostic().contains("skipped"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void propagatesPresentAndMissingSecurityHeaderAssessmentsToAuditResponse() {
        UrlSafetyPolicy policy = mock(UrlSafetyPolicy.class);
        when(policy.parseAndValidate(any())).thenReturn(TARGET);
        PinnedHttpProbe probe = mock(PinnedHttpProbe.class);
        when(probe.check(TARGET)).thenReturn(new HttpProbeResult(
                "PASS",
                15,
                200,
                "Responded with HTTP 200",
                new HttpProbeResult.TlsDetails("PASS", "2027-01-01T00:00:00Z", "TLS handshake successful"),
                List.of(
                        new HttpProbeResult.SecurityHeaderResult("Strict-Transport-Security", "PASS", "max-age=31536000", "HSTS is properly configured with max-age."),
                        new HttpProbeResult.SecurityHeaderResult("Content-Security-Policy", "WARN", null, "Missing Content-Security-Policy header.")
                )
        ));

        AuditResponse response = new AuditExecutionService(policy, probe, mock(ApiSmokeTestService.class))
                .executeAudit("https://example.com", "https://example.com", List.of());

        List<AuditResponse.SecurityHeaderAssessment> headers = response.targets().get(0).securityHeaders();
        assertEquals(2, headers.size());
        assertEquals("Strict-Transport-Security", headers.get(0).headerName());
        assertEquals("PASS", headers.get(0).status());
        assertEquals("Content-Security-Policy", headers.get(1).headerName());
        assertEquals("WARN", headers.get(1).status());
        assertEquals("Missing Content-Security-Policy header.", headers.get(1).diagnostic());

        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        String json = mapper.valueToTree(response).toString();
        assertTrue(json.contains("Strict-Transport-Security"));
        assertTrue(json.contains("Missing Content-Security-Policy header."));
    }
}
