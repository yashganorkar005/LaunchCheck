package com.launchcheck.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record UserSummary(
        UUID id,
        String email,
        Instant createdAt
) {
}
