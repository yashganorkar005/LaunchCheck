package com.launchcheck.audit.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AuditResponse(
    UUID auditId,
    Instant startedAt,
    Instant completedAt,
    long durationMs,
    String overallStatus,
    List<TargetResult> targets,
    List<ApiSmokeTestResult> apiTests,
    CorsDiagnosticResult cors
) {
    public record TargetResult(
        String role,
        String host,
        String status,
        Integer httpStatus,
        Long responseTimeMs,
        TlsResult tls,
        List<RedirectInfo> redirects,
        String diagnostic,
        String nextStep,
        List<SecurityHeaderAssessment> securityHeaders
    ) {}

    public record SecurityHeaderAssessment(
        String headerName,
        String status,
        String value,
        String diagnostic
    ) {}

    public record TlsResult(String status, String certificateExpiresAt, String diagnostic) {}
    public record RedirectInfo(int statusCode, String host, String scheme) {}
}
