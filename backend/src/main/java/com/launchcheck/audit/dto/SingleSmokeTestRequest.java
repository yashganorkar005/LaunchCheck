package com.launchcheck.audit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SingleSmokeTestRequest(
        String name,
        @NotBlank(message = "Endpoint URL is required")
        String url,
        @NotNull(message = "Expected status is required")
        Integer expectedStatus,
        String jsonPointer,
        String expectedJsonValue,
        String bearerToken
) {
}
