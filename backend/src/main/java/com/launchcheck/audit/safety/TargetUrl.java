package com.launchcheck.audit.safety;

public record TargetUrl(String scheme, String host, int port, String path) {}
