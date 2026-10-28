package com.launchcheck.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audits")
public class AuditRecord {

    @Id
    private UUID id;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "audit_type", nullable = false, length = 32)
    private String auditType = "PREFLIGHT";

    @Column(name = "target_label", length = 1024)
    private String targetLabel;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    @Column(name = "overall_status", nullable = false, length = 32)
    private String overallStatus;

    @Column(name = "frontend_status", length = 32)
    private String frontendStatus;

    @Column(name = "frontend_http_status")
    private Integer frontendHttpStatus;

    @Column(name = "frontend_tls_status", length = 32)
    private String frontendTlsStatus;

    @Column(name = "frontend_diagnostic", columnDefinition = "TEXT")
    private String frontendDiagnostic;

    @Column(name = "backend_status", length = 32)
    private String backendStatus;

    @Column(name = "backend_http_status")
    private Integer backendHttpStatus;

    @Column(name = "backend_tls_status", length = 32)
    private String backendTlsStatus;

    @Column(name = "backend_diagnostic", columnDefinition = "TEXT")
    private String backendDiagnostic;

    @Column(name = "cors_status", length = 32)
    private String corsStatus;

    @Column(name = "cors_diagnostic", columnDefinition = "TEXT")
    private String corsDiagnostic;

    @Column(name = "api_smoke_test_count", nullable = false)
    private int apiSmokeTestCount;

    @Column(name = "api_smoke_test_passed_count", nullable = false)
    private int apiSmokeTestPassedCount;

    @Column(name = "details_json", columnDefinition = "TEXT")
    private String detailsJson;

    public AuditRecord() {
    }

    public AuditRecord(UUID id, UUID projectId, UUID userId, String auditType, String targetLabel,
                       Instant startedAt, Instant completedAt, long durationMs, String overallStatus,
                       String frontendStatus, Integer frontendHttpStatus, String frontendTlsStatus,
                       String frontendDiagnostic, String backendStatus, Integer backendHttpStatus,
                       String backendTlsStatus, String backendDiagnostic, String corsStatus,
                       String corsDiagnostic, int apiSmokeTestCount, int apiSmokeTestPassedCount,
                       String detailsJson) {
        this.id = id;
        this.projectId = projectId;
        this.userId = userId;
        this.auditType = auditType != null ? auditType : "PREFLIGHT";
        this.targetLabel = targetLabel;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.durationMs = durationMs;
        this.overallStatus = overallStatus;
        this.frontendStatus = frontendStatus;
        this.frontendHttpStatus = frontendHttpStatus;
        this.frontendTlsStatus = frontendTlsStatus;
        this.frontendDiagnostic = frontendDiagnostic;
        this.backendStatus = backendStatus;
        this.backendHttpStatus = backendHttpStatus;
        this.backendTlsStatus = backendTlsStatus;
        this.backendDiagnostic = backendDiagnostic;
        this.corsStatus = corsStatus;
        this.corsDiagnostic = corsDiagnostic;
        this.apiSmokeTestCount = apiSmokeTestCount;
        this.apiSmokeTestPassedCount = apiSmokeTestPassedCount;
        this.detailsJson = detailsJson;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getAuditType() { return auditType; }
    public void setAuditType(String auditType) { this.auditType = auditType; }

    public String getTargetLabel() { return targetLabel; }
    public void setTargetLabel(String targetLabel) { this.targetLabel = targetLabel; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    public String getOverallStatus() { return overallStatus; }
    public void setOverallStatus(String overallStatus) { this.overallStatus = overallStatus; }

    public String getFrontendStatus() { return frontendStatus; }
    public void setFrontendStatus(String frontendStatus) { this.frontendStatus = frontendStatus; }

    public Integer getFrontendHttpStatus() { return frontendHttpStatus; }
    public void setFrontendHttpStatus(Integer frontendHttpStatus) { this.frontendHttpStatus = frontendHttpStatus; }

    public String getFrontendTlsStatus() { return frontendTlsStatus; }
    public void setFrontendTlsStatus(String frontendTlsStatus) { this.frontendTlsStatus = frontendTlsStatus; }

    public String getFrontendDiagnostic() { return frontendDiagnostic; }
    public void setFrontendDiagnostic(String frontendDiagnostic) { this.frontendDiagnostic = frontendDiagnostic; }

    public String getBackendStatus() { return backendStatus; }
    public void setBackendStatus(String backendStatus) { this.backendStatus = backendStatus; }

    public Integer getBackendHttpStatus() { return backendHttpStatus; }
    public void setBackendHttpStatus(Integer backendHttpStatus) { this.backendHttpStatus = backendHttpStatus; }

    public String getBackendTlsStatus() { return backendTlsStatus; }
    public void setBackendTlsStatus(String backendTlsStatus) { this.backendTlsStatus = backendTlsStatus; }

    public String getBackendDiagnostic() { return backendDiagnostic; }
    public void setBackendDiagnostic(String backendDiagnostic) { this.backendDiagnostic = backendDiagnostic; }

    public String getCorsStatus() { return corsStatus; }
    public void setCorsStatus(String corsStatus) { this.corsStatus = corsStatus; }

    public String getCorsDiagnostic() { return corsDiagnostic; }
    public void setCorsDiagnostic(String corsDiagnostic) { this.corsDiagnostic = corsDiagnostic; }

    public int getApiSmokeTestCount() { return apiSmokeTestCount; }
    public void setApiSmokeTestCount(int apiSmokeTestCount) { this.apiSmokeTestCount = apiSmokeTestCount; }

    public int getApiSmokeTestPassedCount() { return apiSmokeTestPassedCount; }
    public void setApiSmokeTestPassedCount(int apiSmokeTestPassedCount) { this.apiSmokeTestPassedCount = apiSmokeTestPassedCount; }

    public String getDetailsJson() { return detailsJson; }
    public void setDetailsJson(String detailsJson) { this.detailsJson = detailsJson; }
}
