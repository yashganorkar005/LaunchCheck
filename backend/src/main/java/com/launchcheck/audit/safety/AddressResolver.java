package com.launchcheck.audit.safety;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

@FunctionalInterface
public interface AddressResolver {
    List<InetAddress> resolve(String host) throws UnknownHostException;
}
