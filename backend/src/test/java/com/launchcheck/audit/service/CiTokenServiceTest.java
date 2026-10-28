package com.launchcheck.audit.service;

import com.launchcheck.audit.entity.CiServiceToken;
import com.launchcheck.audit.repository.CiServiceTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CiTokenServiceTest {
    @Mock CiServiceTokenRepository repository;

    @Test
    void generatedTokenPersistsOnlyItsSha256DigestAndValidates() throws Exception {
        CiTokenService service = new CiTokenService(repository);
        UUID projectId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        when(repository.save(any(CiServiceToken.class))).thenAnswer(call -> call.getArgument(0));

        String raw = service.generateToken(projectId, ownerId, "pipeline", null);

        ArgumentCaptor<CiServiceToken> saved = ArgumentCaptor.forClass(CiServiceToken.class);
        verify(repository).save(saved.capture());
        CiServiceToken entity = saved.getValue();
        String expectedHash = Base64.getEncoder().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        assertTrue(raw.startsWith("lc_ci_"));
        assertEquals(expectedHash, entity.getTokenHash());
        assertNotEquals(raw, entity.getTokenHash());
        assertEquals(projectId, entity.getProjectId());
        when(repository.findByTokenHashAndProjectId(expectedHash, projectId)).thenReturn(Optional.of(entity));
        assertTrue(service.validateToken(raw, projectId));
        assertFalse(service.validateToken(raw, UUID.randomUUID()));
    }

    @Test
    void rejectsMalformedExpiredAndRevokedTokens() {
        CiTokenService service = new CiTokenService(repository);
        UUID projectId = UUID.randomUUID();
        assertFalse(service.validateToken("wrong", projectId));
        assertFalse(service.validateToken("lc_ci_missing", projectId));

        String expiredRaw = "lc_ci_expired";
        String expiredHash = hash(expiredRaw);
        CiServiceToken expired = new CiServiceToken(UUID.randomUUID(), projectId, UUID.randomUUID(),
                "expired", expiredHash, "lc_ci_", Instant.now().minusSeconds(60));
        when(repository.findByTokenHashAndProjectId(expiredHash, projectId)).thenReturn(Optional.of(expired));
        assertFalse(service.validateToken(expiredRaw, projectId));

        String revokedRaw = "lc_ci_revoked";
        String revokedHash = hash(revokedRaw);
        CiServiceToken revoked = new CiServiceToken(UUID.randomUUID(), projectId, UUID.randomUUID(),
                "revoked", revokedHash, "lc_ci_", null);
        revoked.setRevokedAt(Instant.now());
        when(repository.findByTokenHashAndProjectId(revokedHash, projectId)).thenReturn(Optional.of(revoked));
        assertFalse(service.validateToken(revokedRaw, projectId));
    }

    @Test
    void revokeRequiresOwnerAndInvalidatesToken() {
        CiTokenService service = new CiTokenService(repository);
        UUID owner = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();
        CiServiceToken token = new CiServiceToken(tokenId, UUID.randomUUID(), owner,
                "pipeline", hash("lc_ci_secret"), "lc_ci_", null);
        when(repository.findById(tokenId)).thenReturn(Optional.of(token));
        when(repository.save(token)).thenReturn(token);

        assertThrows(IllegalArgumentException.class, () -> service.revokeToken(tokenId, UUID.randomUUID()));
        service.revokeToken(tokenId, owner);
        assertNotNull(token.getRevokedAt());
        when(repository.findByTokenHashAndProjectId(token.getTokenHash(), token.getProjectId())).thenReturn(Optional.of(token));
        assertFalse(service.validateToken("lc_ci_secret", token.getProjectId()));
    }

    private static String hash(String value) {
        try {
            return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
