package com.launchcheck.auth.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

    private final byte[] secretKey;
    private final long expirationMs;
    private final ObjectMapper objectMapper;

    public JwtService(
            @Value("${launchcheck.security.jwt-secret:0123456789012345678901234567890123456789012345678901234567890123}") String secret,
            @Value("${launchcheck.security.jwt-expiration-ms:86400000}") long expirationMs
    ) {
        this.secretKey = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationMs = expirationMs;
        this.objectMapper = new ObjectMapper();
    }

    public String generateToken(UUID userId, String email) {
        try {
            long now = Instant.now().getEpochSecond();
            long exp = now + (expirationMs / 1000);

            String headerB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(HEADER_JSON.getBytes(StandardCharsets.UTF_8));

            Map<String, Object> payloadMap = Map.of(
                    "sub", userId.toString(),
                    "email", email,
                    "iat", now,
                    "exp", exp
            );
            byte[] payloadBytes = objectMapper.writeValueAsBytes(payloadMap);
            String payloadB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadBytes);

            String contentToSign = headerB64 + "." + payloadB64;
            byte[] signature = sign(contentToSign.getBytes(StandardCharsets.UTF_8));
            String signatureB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(signature);

            return contentToSign + "." + signatureB64;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate JWT", e);
        }
    }

    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return false;
        }

        try {
            String contentToSign = parts[0] + "." + parts[1];
            byte[] expectedSignature = sign(contentToSign.getBytes(StandardCharsets.UTF_8));
            byte[] actualSignature = Base64.getUrlDecoder().decode(parts[2]);

            if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
                return false;
            }

            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode payload = objectMapper.readTree(payloadBytes);

            long exp = payload.path("exp").asLong(0);
            long now = Instant.now().getEpochSecond();
            return exp > now;
        } catch (Exception e) {
            return false;
        }
    }

    public UUID extractUserId(String token) {
        JsonNode payload = parsePayload(token);
        String sub = payload.path("sub").asText(null);
        if (sub == null) {
            throw new IllegalArgumentException("Token missing subject");
        }
        return UUID.fromString(sub);
    }

    public String extractEmail(String token) {
        JsonNode payload = parsePayload(token);
        String email = payload.path("email").asText(null);
        if (email == null) {
            throw new IllegalArgumentException("Token missing email");
        }
        return email;
    }

    private JsonNode parsePayload(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid token format");
        }
        try {
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            return objectMapper.readTree(payloadBytes);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid token payload", e);
        }
    }

    private byte[] sign(byte[] data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretKey, HMAC_ALGORITHM));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign token", e);
        }
    }
}
