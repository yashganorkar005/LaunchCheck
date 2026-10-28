package com.launchcheck.audit.repository;

import com.launchcheck.audit.entity.CiServiceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CiServiceTokenRepository extends JpaRepository<CiServiceToken, UUID> {
    Optional<CiServiceToken> findByTokenPrefix(String tokenPrefix);

    Optional<CiServiceToken> findByTokenHashAndProjectId(String tokenHash, UUID projectId);

    List<CiServiceToken> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
