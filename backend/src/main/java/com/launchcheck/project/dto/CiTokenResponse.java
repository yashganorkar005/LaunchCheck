package com.launchcheck.project.dto;

public record CiTokenResponse(
    String id,
    String tokenName,
    String tokenPrefix,
    String expiresAt,
    String createdAt
) {}
