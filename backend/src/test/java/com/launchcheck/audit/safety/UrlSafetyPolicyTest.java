package com.launchcheck.audit.safety;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.InetAddress;
import java.util.List;
import org.junit.jupiter.api.Test;

class UrlSafetyPolicyTest {
    private final UrlSafetyPolicy policy = new UrlSafetyPolicy(new GlobalAddressPolicy(), host -> List.of(InetAddress.getByName("8.8.8.8")));

    @Test
    void acceptsAbsoluteHttpAndHttpsUrls() {
        TargetUrl target = policy.parseAndValidate("https://example.com/status");
        assertEquals("https", target.scheme());
        assertEquals("example.com", target.host());
        assertEquals(443, target.port());
    }

    @Test
    void rejectsUnsafeUrlForms() {
        assertThrows(UnsafeTargetException.class, () -> policy.parseAndValidate("http://127.0.0.1/"));
        assertThrows(UnsafeTargetException.class, () -> policy.parseAndValidate("http://user:pass@example.com/"));
        assertThrows(UnsafeTargetException.class, () -> policy.parseAndValidate("ftp://example.com/"));
        assertThrows(UnsafeTargetException.class, () -> policy.parseAndValidate("https://example.com:8443/"));
    }

    @Test
    void rejectsAnyForbiddenAddressInResolution() throws Exception {
        UrlSafetyPolicy mixed = new UrlSafetyPolicy(new GlobalAddressPolicy(), host -> List.of(InetAddress.getByName("8.8.8.8"), InetAddress.getByName("10.0.0.1")));
        TargetUrl target = mixed.parseAndValidate("https://example.com/");
        assertThrows(UnsafeTargetException.class, () -> mixed.resolveAndValidate(target));
    }
}
