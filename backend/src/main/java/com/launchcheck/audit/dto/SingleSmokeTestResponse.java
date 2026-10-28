package com.launchcheck.audit.dto;

public record SingleSmokeTestResponse(
        boolean success,
        String status,
        Integer expectedStatus,
        Integer actualStatus,
        Long latencyMs,
        String diagnostic,
        String errorMessage,
        boolean jsonAssertionPassed,
        String jsonAssertionDiagnostic
) {
}
