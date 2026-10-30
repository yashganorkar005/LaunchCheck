package com.launchcheck.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditHistoryServiceTest {

    @Mock
    private AuditRepository auditRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private AuditExecutionService auditExecutionService;

    private ObjectMapper objectMapper;
    private AuditHistoryService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        service = new AuditHistoryService(auditRepository, projectRepository, auditExecutionService, objectMapper);
    }

    @Test
    void triggerProjectAudit_persistsAndReturnsAuditResponse() {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Project project = new Project(projectId, userId, "Test", "https://front.com", "https://back.com", Instant.now(), Instant.now());

        when(projectRepository.findByIdAndUserId(projectId, userId)).thenReturn(Optional.of(project));

        UUID auditId = UUID.randomUUID();
        AuditResponse mockResponse = new AuditResponse(
                auditId,
                Instant.now().minusSeconds(2),
                Instant.now(),
                2000L,
                "PASS",
                List.of(
                        new AuditResponse.TargetResult("FRONTEND", "front.com", "PASS", 200, 100L,
                                new AuditResponse.TlsResult("PASS", "2027-01-01", "OK"), List.of(), "Diag", "None", List.of()),
                        new AuditResponse.TargetResult("BACKEND", "back.com", "PASS", 200, 120L,
                                new AuditResponse.TlsResult("PASS", "2027-01-01", "OK"), List.of(), "Diag", "None", List.of())
                ),
                List.of(),
                null
        );

        when(auditExecutionService.executeAudit(eq("https://front.com"), eq("https://back.com"), any()))
                .thenReturn(mockResponse);

        AuditResponse result = service.triggerProjectAudit(userId, projectId, new ProjectAuditRequest(null));

        assertNotNull(result);
        assertEquals(auditId, result.auditId());
        assertEquals("PASS", result.overallStatus());

        ArgumentCaptor<AuditRecord> captor = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditRepository).save(captor.capture());

        AuditRecord saved = captor.getValue();
        assertEquals(auditId, saved.getId());
        assertEquals(projectId, saved.getProjectId());
        assertEquals(userId, saved.getUserId());
        assertEquals("PREFLIGHT", saved.getAuditType());
        assertEquals("Test", saved.getTargetLabel());
        assertEquals("PASS", saved.getOverallStatus());
        assertEquals("PASS", saved.getFrontendStatus());
        assertEquals("PASS", saved.getBackendStatus());
        assertNotNull(saved.getDetailsJson());
    }

    @Test
    void recordCorsAudit_persistsCorrectRecord() {
        UUID userId = UUID.randomUUID();
        CorsDiagnosticResult corsResult = new CorsDiagnosticResult(
                "/api/test", "POST", "https://example.com", "PASS", "Origin allowed", true, null
        );

        when(auditRepository.save(any(AuditRecord.class))).thenAnswer(i -> i.getArgument(0));

        AuditRecord record = service.recordCorsAudit(userId, "https://api.com/api/test", corsResult, 150L);

        assertNotNull(record);
        assertEquals(userId, record.getUserId());
        assertNull(record.getProjectId());
        assertEquals("CORS", record.getAuditType());
        assertEquals("https://api.com/api/test", record.getTargetLabel());
        assertEquals("PASS", record.getOverallStatus());
        assertEquals("PASS", record.getCorsStatus());
    }

    @Test
    void recordSmokeAudit_persistsCorrectRecord() {
        UUID userId = UUID.randomUUID();
        SingleSmokeTestRequest req = new SingleSmokeTestRequest("Smoke", "https://api.com/status", 200, null, null);
        SingleSmokeTestResponse res = new SingleSmokeTestResponse(
                true, "PASS", 200, 200, 80L, "Healthy", null, true, null
        );

        when(auditRepository.save(any(AuditRecord.class))).thenAnswer(i -> i.getArgument(0));

        AuditRecord record = service.recordSmokeAudit(userId, "https://api.com/status", req, res);

        assertNotNull(record);
        assertEquals(userId, record.getUserId());
        assertNull(record.getProjectId());
        assertEquals("SMOKE", record.getAuditType());
        assertEquals("https://api.com/status", record.getTargetLabel());
        assertEquals("PASS", record.getOverallStatus());
        assertEquals(1, record.getApiSmokeTestCount());
        assertEquals(1, record.getApiSmokeTestPassedCount());
    }

    @Test
    void getUserAudits_withType_filtersCorrectly() {
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        AuditRecord record = new AuditRecord(
                UUID.randomUUID(), null, userId, "SMOKE", "https://api.com/status",
                Instant.now().minusSeconds(5), Instant.now(), 50L, "PASS",
                null, null, null, null, "PASS", 200, null, "Healthy", null, null, 1, 1, "{}"
        );

        when(auditRepository.findByUserIdAndAuditTypeOrderByStartedAtDesc(userId, "SMOKE", pageable))
                .thenReturn(new PageImpl<>(List.of(record), pageable, 1));

        Page<AuditSummaryResponse> page = service.getUserAudits(userId, "SMOKE", pageable);

        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
        assertEquals("SMOKE", page.getContent().get(0).auditType());
        assertEquals("https://api.com/status", page.getContent().get(0).targetLabel());
    }

    @Test
    void getProjectAudits_returnsMappedPage() {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        when(projectRepository.existsByIdAndUserId(projectId, userId)).thenReturn(true);

        AuditRecord record = new AuditRecord(
                UUID.randomUUID(),
                projectId,
                userId,
                "PREFLIGHT",
                "Prod API",
                Instant.now().minusSeconds(10),
                Instant.now(),
                10000L,
                "WARN",
                "PASS", 200, "PASS", "Diag",
                "WARN", 500, "PASS", "Diag",
                null, null,
                2, 1,
                "{}"
        );

        when(auditRepository.findByProjectIdAndUserIdOrderByStartedAtDesc(projectId, userId, pageable))
                .thenReturn(new PageImpl<>(List.of(record), pageable, 1));

        Page<AuditSummaryResponse> page = service.getProjectAudits(userId, projectId, pageable);

        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
        AuditSummaryResponse summary = page.getContent().get(0);
        assertEquals(record.getId(), summary.id());
        assertEquals("WARN", summary.overallStatus());
        assertEquals("PASS", summary.frontendStatus());
        assertEquals("WARN", summary.backendStatus());
        assertEquals(2, summary.apiSmokeTestCount());
        assertEquals(1, summary.apiSmokeTestPassedCount());
    }

    @Test
    void getAuditById_deserializesDetailsJson() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID auditId = UUID.randomUUID();

        AuditResponse original = new AuditResponse(
                auditId,
                Instant.now().minusSeconds(1),
                Instant.now(),
                1000L,
                "PASS",
                List.of(),
                List.of(),
                null
        );

        String json = objectMapper.writeValueAsString(original);

        AuditRecord record = new AuditRecord();
        record.setId(auditId);
        record.setUserId(userId);
        record.setAuditType("PREFLIGHT");
        record.setDetailsJson(json);

        when(auditRepository.findByIdAndUserId(auditId, userId)).thenReturn(Optional.of(record));

        AuditResponse result = service.getAuditById(userId, auditId);

        assertNotNull(result);
        assertEquals(auditId, result.auditId());
        assertEquals("PASS", result.overallStatus());
    }

    @Test
    void recordCiAudit_persistsCorrectRecord() {
        UUID projectId = UUID.randomUUID();
        UUID auditId = UUID.randomUUID();
        AuditResponse mockResponse = new AuditResponse(
                auditId,
                Instant.now().minusSeconds(2),
                Instant.now(),
                2000L,
                "PASS",
                List.of(),
                List.of(),
                null
        );

        when(auditRepository.save(any(AuditRecord.class))).thenAnswer(i -> i.getArgument(0));

        AuditRecord record = service.recordCiAudit(projectId, "CI-Runner", mockResponse, "PROMOTE");

        assertNotNull(record);
        assertEquals(projectId, record.getProjectId());
        assertEquals("CI", record.getAuditType());
        assertEquals("CI-Runner", record.getTargetLabel());
        assertEquals("PASS", record.getOverallStatus());
        // Verify system user ID
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), record.getUserId());
    }

    @Test
    void deleteAuditRecord_success() {
        UUID userId = UUID.randomUUID();
        UUID auditId = UUID.randomUUID();
        AuditRecord record = new AuditRecord();
        record.setId(auditId);
        record.setUserId(userId);

        when(auditRepository.findByIdAndUserId(auditId, userId)).thenReturn(Optional.of(record));

        service.deleteAuditRecord(userId, auditId);

        verify(auditRepository).delete(record);
    }

    @Test
    void deleteAuditRecord_notFoundOrUnauthorized_throws() {
        UUID userId = UUID.randomUUID();
        UUID auditId = UUID.randomUUID();

        when(auditRepository.findByIdAndUserId(auditId, userId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.deleteAuditRecord(userId, auditId));
    }

    @Test
    void clearUserHistory_callsRepository() {
        UUID userId = UUID.randomUUID();

        service.clearUserHistory(userId);

        verify(auditRepository).deleteByUserId(userId);
    }
}
