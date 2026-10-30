package com.launchcheck.audit.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Locale;

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

    public record HeaderAssertion(String name, String value, HeaderMatchCondition condition) {
        public HeaderAssertion(String name, String value) {
            this(name, value, HeaderMatchCondition.CONTAINS);
        }
    }

    public enum HeaderMatchCondition {
        CONTAINS,
        EQUALS,
        STARTS_WITH,
        ENDS_WITH;

        @JsonCreator
        public static HeaderMatchCondition fromJson(String value) {
            if (value == null || value.isBlank()) return CONTAINS;
            return switch (value.trim().toLowerCase(Locale.ROOT).replace("-", "").replace("_", "").replace(" ", "")) {
                case "equals", "equal" -> EQUALS;
                case "startswith" -> STARTS_WITH;
                case "endswith" -> ENDS_WITH;
                case "contains", "contain" -> CONTAINS;
                default -> throw new IllegalArgumentException("Invalid header match condition: " + value);
            };
        }
    }

    public record JsonAssertion(String jsonPointer, String expectedValue) {}
}
