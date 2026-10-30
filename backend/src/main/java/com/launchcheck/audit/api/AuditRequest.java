package com.launchcheck.audit.api;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record AuditRequest(
    @NotBlank String frontendUrl,
    @NotBlank String backendUrl,
    List<ApiCheck> apiChecks
) {
    public record ApiCheck(
        String path,
        String method,
        Integer expectedStatus,
        List<HeaderAssertion> headerAssertions,
        List<JsonAssertion> jsonAssertions
    ) {}

    public record HeaderAssertion(String name, String value) {}
    public record JsonAssertion(String jsonPointer, String expectedValue) {}
}
