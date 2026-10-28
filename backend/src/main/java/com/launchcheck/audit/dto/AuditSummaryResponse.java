package com.launchcheck.audit.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditSummaryResponse(
    UUID id,
    UUID projectId,
    String auditType,
    String targetLabel,
    Instant startedAt,
    Instant completedAt,
    long durationMs,
    String overallStatus,
    String frontendStatus,
    Integer frontendHttpStatus,
    String backendStatus,
    Integer backendHttpStatus,
    String corsStatus,
    int apiSmokeTestCount,
    int apiSmokeTestPassedCount
) {}
