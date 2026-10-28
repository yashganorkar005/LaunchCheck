package com.launchcheck.audit.repository;

import com.launchcheck.audit.entity.AuditRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

public interface AuditRepository extends JpaRepository<AuditRecord, UUID> {
    Page<AuditRecord> findByProjectIdOrderByStartedAtDesc(UUID projectId, Pageable pageable);
    Page<AuditRecord> findByUserIdOrderByStartedAtDesc(UUID userId, Pageable pageable);
    Page<AuditRecord> findByUserIdAndAuditTypeOrderByStartedAtDesc(UUID userId, String auditType, Pageable pageable);
    Optional<AuditRecord> findByIdAndUserId(UUID id, UUID userId);
    Page<AuditRecord> findByProjectIdAndUserIdOrderByStartedAtDesc(UUID projectId, UUID userId, Pageable pageable);

    @Modifying
    @Transactional
    void deleteByIdAndUserId(UUID id, UUID userId);

    @Modifying
    @Transactional
    void deleteByUserId(UUID userId);
}
