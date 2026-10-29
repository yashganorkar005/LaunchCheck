package com.launchcheck.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.launchcheck.audit.api.ApiSmokeTestResult;
import com.launchcheck.audit.api.AuditResponse;
import com.launchcheck.audit.api.CorsDiagnosticResult;
import com.launchcheck.audit.dto.AuditSummaryResponse;
import com.launchcheck.audit.dto.ProjectAuditRequest;
import com.launchcheck.audit.dto.SingleSmokeTestRequest;
import com.launchcheck.audit.dto.SingleSmokeTestResponse;
import com.launchcheck.audit.entity.AuditRecord;
import com.launchcheck.audit.repository.AuditRepository;
import com.launchcheck.project.entity.Project;
import com.launchcheck.project.repository.ProjectRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AuditHistoryService {

    public static final UUID SYSTEM_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final AuditRepository auditRepository;
    private final ProjectRepository projectRepository;
    private final AuditExecutionService auditExecutionService;
    private final ObjectMapper objectMapper;

    public AuditHistoryService(AuditRepository auditRepository,
                               ProjectRepository projectRepository,
                               AuditExecutionService auditExecutionService,
                               ObjectMapper objectMapper) {
        this.auditRepository = auditRepository;
        this.projectRepository = projectRepository;
        this.auditExecutionService = auditExecutionService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AuditRecord recordCiAudit(UUID projectId, String targetLabel, AuditResponse response, String decision) {
        int apiSmokeTestCount = response.apiTests() != null ? response.apiTests().size() : 0;
        int apiSmokeTestPassedCount = response.apiTests() != null ?
                (int) response.apiTests().stream().filter(t -> "PASS".equals(t.status())).count() : 0;

        String detailsJson;
        try {
            detailsJson = objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize CI audit response", e);
        }

        // CI Audits are associated with the project, but have no specific human user owner.
        // We use a dedicated system user ID to maintain database FK integrity.
        // In a production environment, this should be a properly seeded system principal.
        UUID systemUserId = SYSTEM_USER_ID;

        AuditRecord record = new AuditRecord(
                response.auditId(),
                projectId,
                systemUserId,
                "CI",
                targetLabel,
                response.startedAt(),
                response.completedAt(),
                response.durationMs(),
                response.overallStatus(),
                null, null, null, null,
                null, null, null, null,
                null, null,
                apiSmokeTestCount,
                apiSmokeTestPassedCount,
                detailsJson
        );
        return auditRepository.save(record);
    }

    @Transactional
    public AuditResponse triggerProjectAudit(UUID userId, UUID projectId, ProjectAuditRequest request, String clientIp) {
        Project project = projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        String frontendUrl = (request != null && request.frontendUrl() != null && !request.frontendUrl().isBlank())
                ? request.frontendUrl().trim() : project.getFrontendUrl();
        String backendUrl = (request != null && request.backendUrl() != null && !request.backendUrl().isBlank())
                ? request.backendUrl().trim() : project.getBackendUrl();

        BoundedAuditExecutionService boundedService = new BoundedAuditExecutionService(auditExecutionService);
        AuditResponse response = boundedService.executeBoundedAudit(
                clientIp != null ? clientIp : "unknown",
                frontendUrl,
                backendUrl,
                request != null ? request.apiChecks() : null,
                request != null ? request.bearerToken() : null
        );

        recordPreflightAudit(userId, projectId, project.getName(), response);
        return response;
    }

    @Transactional
    public AuditResponse triggerProjectAudit(UUID userId, UUID projectId, ProjectAuditRequest request) {
        return triggerProjectAudit(userId, projectId, request, "unknown");
    }

    @Transactional
    public AuditRecord recordPreflightAudit(UUID userId, UUID projectId, String targetLabel, AuditResponse response) {
        AuditResponse.TargetResult frontendTarget = response.targets().stream()
                .filter(t -> "FRONTEND".equalsIgnoreCase(t.role()))
                .findFirst()
                .orElse(null);

        AuditResponse.TargetResult backendTarget = response.targets().stream()
                .filter(t -> "BACKEND".equalsIgnoreCase(t.role()))
                .findFirst()
                .orElse(null);

        int apiSmokeTestCount = response.apiTests() != null ? response.apiTests().size() : 0;
        int apiSmokeTestPassedCount = response.apiTests() != null ?
                (int) response.apiTests().stream().filter(t -> "PASS".equals(t.status())).count() : 0;

        String detailsJson;
        try {
            detailsJson = objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize audit response", e);
        }

        AuditRecord record = new AuditRecord(
                response.auditId(),
                projectId,
                userId,
                "PREFLIGHT",
                targetLabel,
                response.startedAt(),
                response.completedAt(),
                response.durationMs(),
                response.overallStatus(),
                frontendTarget != null ? frontendTarget.status() : null,
                frontendTarget != null ? frontendTarget.httpStatus() : null,
                frontendTarget != null && frontendTarget.tls() != null ? frontendTarget.tls().status() : null,
                frontendTarget != null ? frontendTarget.diagnostic() : null,
                backendTarget != null ? backendTarget.status() : null,
                backendTarget != null ? backendTarget.httpStatus() : null,
                backendTarget != null && backendTarget.tls() != null ? backendTarget.tls().status() : null,
                backendTarget != null ? backendTarget.diagnostic() : null,
                response.cors() != null ? response.cors().status() : null,
                response.cors() != null ? response.cors().diagnostic() : null,
                apiSmokeTestCount,
                apiSmokeTestPassedCount,
                detailsJson
        );

        return auditRepository.save(record);
    }

    @Transactional
    public AuditRecord recordCorsAudit(UUID userId, String targetLabel, CorsDiagnosticResult result, long durationMs) {
        Instant now = Instant.now();
        Instant startedAt = now.minusMillis(Math.max(durationMs, 10));

        String detailsJson;
        try {
            detailsJson = objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize CORS diagnostic result", e);
        }

        AuditRecord record = new AuditRecord(
                UUID.randomUUID(),
                null,
                userId,
                "CORS",
                targetLabel,
                startedAt,
                now,
                durationMs,
                result.status(),
                null, null, null, null,
                result.status(), null, null, null,
                result.status(),
                result.diagnostic(),
                0,
                0,
                detailsJson
        );

        return auditRepository.save(record);
    }

    @Transactional
    public AuditRecord recordSmokeAudit(UUID userId, String targetLabel, SingleSmokeTestRequest request, SingleSmokeTestResponse response) {
        Instant now = Instant.now();
        Instant startedAt = now.minusMillis(Math.max(response.latencyMs(), 10));

        String detailsJson;
        try {
            detailsJson = objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize smoke test response", e);
        }

        int passedCount = "PASS".equals(response.status()) ? 1 : 0;

        AuditRecord record = new AuditRecord(
                UUID.randomUUID(),
                null,
                userId,
                "SMOKE",
                targetLabel,
                startedAt,
                now,
                response.latencyMs(),
                response.status(),
                null, null, null, null,
                response.status(),
                response.actualStatus(),
                null,
                response.diagnostic(),
                null, null,
                1,
                passedCount,
                detailsJson
        );

        return auditRepository.save(record);
    }

    @Transactional(readOnly = true)
    public Page<AuditSummaryResponse> getUserAudits(UUID userId, String auditType, Pageable pageable) {
        Page<AuditRecord> page;
        if (auditType != null && !auditType.isBlank() && !"ALL".equalsIgnoreCase(auditType)) {
            page = auditRepository.findByUserIdAndAuditTypeOrderByStartedAtDesc(userId, auditType.toUpperCase(), pageable);
        } else {
            page = auditRepository.findByUserIdOrderByStartedAtDesc(userId, pageable);
        }

        return page.map(r -> new AuditSummaryResponse(
                r.getId(),
                r.getProjectId(),
                r.getAuditType(),
                r.getTargetLabel(),
                r.getStartedAt(),
                r.getCompletedAt(),
                r.getDurationMs(),
                r.getOverallStatus(),
                r.getFrontendStatus(),
                r.getFrontendHttpStatus(),
                r.getBackendStatus(),
                r.getBackendHttpStatus(),
                r.getCorsStatus(),
                r.getApiSmokeTestCount(),
                r.getApiSmokeTestPassedCount()
        ));
    }

    @Transactional(readOnly = true)
    public Page<AuditSummaryResponse> getUserAudits(UUID userId, Pageable pageable) {
        return getUserAudits(userId, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditSummaryResponse> getProjectAudits(UUID userId, UUID projectId, Pageable pageable) {
        if (!projectRepository.existsByIdAndUserId(projectId, userId)) {
            throw new IllegalArgumentException("Project not found: " + projectId);
        }

        return auditRepository.findByProjectIdAndUserIdOrderByStartedAtDesc(projectId, userId, pageable)
                .map(r -> new AuditSummaryResponse(
                        r.getId(),
                        r.getProjectId(),
                        r.getAuditType(),
                        r.getTargetLabel(),
                        r.getStartedAt(),
                        r.getCompletedAt(),
                        r.getDurationMs(),
                        r.getOverallStatus(),
                        r.getFrontendStatus(),
                        r.getFrontendHttpStatus(),
                        r.getBackendStatus(),
                        r.getBackendHttpStatus(),
                        r.getCorsStatus(),
                        r.getApiSmokeTestCount(),
                        r.getApiSmokeTestPassedCount()
                ));
    }

    @Transactional(readOnly = true)
    public AuditResponse getAuditById(UUID userId, UUID auditId) {
        AuditRecord record = auditRepository.findByIdAndUserId(auditId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Audit record not found: " + auditId));

        try {
            if ("CORS".equalsIgnoreCase(record.getAuditType())) {
                CorsDiagnosticResult corsResult = objectMapper.readValue(record.getDetailsJson(), CorsDiagnosticResult.class);
                return new AuditResponse(
                        record.getId(),
                        record.getStartedAt(),
                        record.getCompletedAt(),
                        record.getDurationMs(),
                        record.getOverallStatus(),
                        List.of(new AuditResponse.TargetResult(
                                "BACKEND",
                                record.getTargetLabel() != null ? record.getTargetLabel() : "CORS Target",
                                record.getOverallStatus(),
                                200,
                                record.getDurationMs(),
                                new AuditResponse.TlsResult("PASS", null, "OK"),
                                List.of(),
                                record.getCorsDiagnostic() != null ? record.getCorsDiagnostic() : "CORS preflight simulated",
                                "Inspect CORS headers",
                                List.of()
                        )),
                        List.of(),
                        corsResult
                );
            } else if ("SMOKE".equalsIgnoreCase(record.getAuditType())) {
                SingleSmokeTestResponse smokeRes = objectMapper.readValue(record.getDetailsJson(), SingleSmokeTestResponse.class);
                return new AuditResponse(
                        record.getId(),
                        record.getStartedAt(),
                        record.getCompletedAt(),
                        record.getDurationMs(),
                        record.getOverallStatus(),
                        List.of(new AuditResponse.TargetResult(
                                "BACKEND",
                                record.getTargetLabel() != null ? record.getTargetLabel() : "API Smoke Target",
                                record.getOverallStatus(),
                                smokeRes.actualStatus(),
                                record.getDurationMs(),
                                new AuditResponse.TlsResult("PASS", null, "OK"),
                                List.of(),
                                smokeRes.diagnostic() != null ? smokeRes.diagnostic() : "Smoke test verified",
                                "Review status code and assertions",
                                List.of()
                        )),
                        List.of(new ApiSmokeTestResult(
                                record.getTargetLabel() != null ? record.getTargetLabel() : "/api/check",
                                "GET",
                                smokeRes.expectedStatus(),
                                smokeRes.actualStatus(),
                                smokeRes.status(),
                                smokeRes.diagnostic() != null ? smokeRes.diagnostic() : "Checked",
                                "None",
                                smokeRes.jsonAssertionPassed(),
                                smokeRes.jsonAssertionDiagnostic()
                        )),
                        null
                );
            } else {
                return objectMapper.readValue(record.getDetailsJson(), AuditResponse.class);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize audit record", e);
        }
    }

    @Transactional
    public void deleteAuditRecord(UUID userId, UUID auditId) {
        AuditRecord record = auditRepository.findByIdAndUserId(auditId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Audit record not found: " + auditId));
        auditRepository.delete(record);
    }

    @Transactional
    public void clearUserHistory(UUID userId) {
        auditRepository.deleteByUserId(userId);
    }
}
