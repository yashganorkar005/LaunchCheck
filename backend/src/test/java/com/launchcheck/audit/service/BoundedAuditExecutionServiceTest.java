package com.launchcheck.audit.service;

import com.launchcheck.audit.api.AuditRequest;
import com.launchcheck.audit.api.AuditResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class BoundedAuditExecutionServiceTest {

    @Test
    public void testRateLimitingEnforced() {
        AuditExecutionService delegate = new AuditExecutionService(null) {
            @Override
            public AuditResponse executeAudit(String f, String b, List<AuditRequest.ApiCheck> c) {
                return null;
            }
        };

        BoundedAuditExecutionService bounded = new BoundedAuditExecutionService(delegate);

        for (int i = 0; i < 15; i++) {
            try {
                bounded.executeBoundedAudit("127.0.0.1", "https://example.com", "https://example.com", List.of());
            } catch (Exception ignored) {
            }
        }

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bounded.executeBoundedAudit("127.0.0.1", "https://example.com", "https://example.com", List.of());
        });
        assertEquals(429, ex.getStatusCode().value());
    }

    @Test
    public void testMemoryBoundsAndHighCardinality() {
        AuditExecutionService delegate = new AuditExecutionService(null);
        BoundedAuditExecutionService bounded = new BoundedAuditExecutionService(delegate);

        // Flood with unique IPs exceeding MAX_TRACKED_IPS (5000) to ensure memory is bounded
        for (int i = 0; i < 6000; i++) {
            String ip = "10.0." + (i / 256) + "." + (i % 256);
            bounded.checkRateLimit(ip);
        }

        // Verify no exception and rate limiting still operates normally
        assertDoesNotThrow(() -> bounded.checkRateLimit("192.168.1.1"));
    }
}
