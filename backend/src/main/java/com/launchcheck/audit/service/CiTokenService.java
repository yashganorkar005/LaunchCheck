package com.launchcheck.audit.service;

import com.launchcheck.audit.entity.CiServiceToken;
import com.launchcheck.audit.repository.CiServiceTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class CiTokenService {

    private final CiServiceTokenRepository ciServiceTokenRepository;
    private final SecureRandom secureRandom;
    private static final String TOKEN_PREFIX = "lc_ci_";

    public CiTokenService(CiServiceTokenRepository ciServiceTokenRepository) {
        this.ciServiceTokenRepository = ciServiceTokenRepository;
        this.secureRandom = new SecureRandom();
    }

    @Transactional
    public String generateToken(UUID projectId, UUID ownerUserId, String tokenName, Instant expiresAt) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String rawToken = TOKEN_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        // Hashing the raw token using SHA-256 for persistent storage
        String tokenHash = computeHash(rawToken);

        CiServiceToken tokenEntity = new CiServiceToken(
                UUID.randomUUID(),
                projectId,
                ownerUserId,
                tokenName,
                tokenHash,
                TOKEN_PREFIX,
                expiresAt
        );

        ciServiceTokenRepository.save(tokenEntity);
        return rawToken; // Return raw token only once to the controller
    }

    @Transactional(readOnly = true)
    public boolean validateToken(String rawToken, UUID projectId) {
        if (rawToken == null || !rawToken.startsWith(TOKEN_PREFIX)) {
            return false;
        }

        Optional<CiServiceToken> tokenOpt = ciServiceTokenRepository.findByTokenHashAndProjectId(
                computeHash(rawToken), projectId);
        if (tokenOpt.isEmpty()) {
            return false;
        }

        CiServiceToken token = tokenOpt.get();

        // Check expiration and revocation
        if (token.getExpiresAt() != null && token.getExpiresAt().isBefore(Instant.now())) {
            return false;
        }
        if (token.getRevokedAt() != null) {
            return false;
        }

        // Validate hash
        return computeHash(rawToken).equals(token.getTokenHash());
    }

    @Transactional
    public void revokeToken(UUID tokenId, UUID ownerUserId) {
        CiServiceToken token = ciServiceTokenRepository.findById(tokenId)
                .orElseThrow(() -> new IllegalArgumentException("Token not found"));

        if (!token.getOwnerUserId().equals(ownerUserId)) {
            throw new IllegalArgumentException("Unauthorized");
        }

        token.setRevokedAt(Instant.now());
        ciServiceTokenRepository.save(token);
    }

    private String computeHash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encodedHash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
