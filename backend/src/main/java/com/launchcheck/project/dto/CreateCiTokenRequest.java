package com.launchcheck.project.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public record CreateCiTokenRequest(
    @NotBlank String tokenName,
    Instant expiresAt
) {}
