package com.launchcheck.auth.dto;

public record AuthResponse(
        String token,
        UserSummary user
) {
}
