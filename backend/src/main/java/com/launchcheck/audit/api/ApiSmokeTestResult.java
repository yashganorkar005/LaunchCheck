package com.launchcheck.audit.api;

public record ApiSmokeTestResult(
    String path,
    String method,
    Integer expectedStatus,
    Integer actualStatus,
    String status,
    String diagnostic,
    String nextStep,
    boolean jsonAssertionPassed,
    String jsonAssertionDiagnostic
) {}
