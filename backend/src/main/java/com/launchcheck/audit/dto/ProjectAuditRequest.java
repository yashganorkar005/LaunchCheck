package com.launchcheck.audit.dto;

import com.launchcheck.audit.api.AuditRequest;
import java.util.List;

public record ProjectAuditRequest(
    String frontendUrl,
    String backendUrl,
    List<AuditRequest.ApiCheck> apiChecks
) {
    public ProjectAuditRequest(List<AuditRequest.ApiCheck> apiChecks) {
        this(null, null, apiChecks);
    }
}
