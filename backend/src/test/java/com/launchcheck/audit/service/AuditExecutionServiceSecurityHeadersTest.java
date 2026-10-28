package com.launchcheck.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.launchcheck.audit.api.AuditResponse;
import com.launchcheck.audit.http.HttpProbeResult;
import com.launchcheck.audit.http.PinnedHttpProbe;
import com.launchcheck.audit.safety.TargetUrl;
import com.launchcheck.audit.safety.UrlSafetyPolicy;
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
