package com.launchcheck.project.dto;

import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String name,
        String frontendUrl,
        String backendUrl,
        Instant createdAt,
        Instant updatedAt
) {
}
