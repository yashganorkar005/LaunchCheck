package com.launchcheck.audit.safety;

import java.net.InetAddress;

public class GlobalAddressPolicy {
    public boolean isPermitted(InetAddress address) {
        return !address.isLoopbackAddress()
                && !address.isLinkLocalAddress()
                && !address.isSiteLocalAddress()
                && !address.isMulticastAddress()
                && !address.isAnyLocalAddress();
    }
}
