package com.launchcheck.audit.api;

public record CiAuthResult(
    boolean authenticated,
    String errorMessage
) {}
