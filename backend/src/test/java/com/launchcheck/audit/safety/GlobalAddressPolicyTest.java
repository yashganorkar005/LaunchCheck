package com.launchcheck.audit.safety;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import org.junit.jupiter.api.Test;

class GlobalAddressPolicyTest {
    private final GlobalAddressPolicy policy = new GlobalAddressPolicy();

    @Test
    void permitsPublicGlobalAddress() throws Exception {
        assertTrue(policy.isPermitted(InetAddress.getByName("8.8.8.8")));
    }

    @Test
    void rejectsLoopbackPrivateLinkLocalAndMetadataAddresses() throws Exception {
        assertFalse(policy.isPermitted(InetAddress.getByName("127.0.0.1")));
        assertFalse(policy.isPermitted(InetAddress.getByName("10.1.2.3")));
        assertFalse(policy.isPermitted(InetAddress.getByName("169.254.169.254")));
        assertFalse(policy.isPermitted(InetAddress.getByName("fe80::1")));
        assertFalse(policy.isPermitted(InetAddress.getByName("::1")));
    }
}
