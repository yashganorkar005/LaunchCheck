package com.launchcheck.audit.api;

import com.launchcheck.audit.api.AuditResponse;

public record CiAuditResponse(
    AuditResponse auditResult,
    String executionId,
    String releaseDecision
) {}
