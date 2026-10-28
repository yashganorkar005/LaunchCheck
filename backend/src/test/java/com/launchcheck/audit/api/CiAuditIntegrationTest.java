package com.launchcheck.audit.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.launchcheck.audit.entity.AuditRecord;
import com.launchcheck.audit.repository.AuditRepository;
import com.launchcheck.audit.service.AuditExecutionService;
import com.launchcheck.audit.service.AuditHistoryService;
import com.launchcheck.auth.dto.AuthResponse;
import com.launchcheck.auth.dto.RegisterRequest;
import com.launchcheck.project.dto.CreateCiTokenRequest;
import com.launchcheck.project.dto.CreateProjectRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class CiAuditIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditRepository auditRepository;

    @MockitoBean
    private AuditExecutionService auditExecutionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    private String getAuthToken(String email) throws Exception {
        RegisterRequest register = new RegisterRequest(email, "Password123!");
        String res = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(res, AuthResponse.class).token();
    }

    private String createProject(String token, String name) throws Exception {
        CreateProjectRequest req = new CreateProjectRequest(name, "https://frontend.example.com", "https://backend.example.com");
        String res = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(res).get("id").asText();
    }

    private String createCiToken(String userToken, String projectId, String tokenName) throws Exception {
        CreateCiTokenRequest req = new CreateCiTokenRequest(tokenName, null);
        String res = mockMvc.perform(post("/api/v1/projects/" + projectId + "/ci-tokens")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(res).get("rawToken").asText();
    }

    private AuditResponse mockAudit(String overallStatus) {
        return new AuditResponse(
                UUID.randomUUID(),
                Instant.now().minusSeconds(1),
                Instant.now(),
                1000L,
                overallStatus,
                List.of(new AuditResponse.TargetResult("FRONTEND", "front", overallStatus, 200, 100L,
                        new AuditResponse.TlsResult("PASS", null, "OK"), List.of(), "Diag", "None", List.of())),
                List.of(),
                null
        );
    }

    @Test
    void releaseDecisionMappingAndAuditPersistenceVerified() throws Exception {
        String token = getAuthToken("ciuser_" + System.currentTimeMillis() + "@example.com");
        String projectId = createProject(token, "Release Gate App");
        String rawCiToken = createCiToken(token, projectId, "Release Pipe");

        // PASS -> PROMOTE
        when(auditExecutionService.executeAudit(any(), any(), any())).thenReturn(mockAudit("PASS"));
        mockMvc.perform(post("/api/v1/projects/" + projectId + "/ci/audits")
                        .header("X-CI-Token", rawCiToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.releaseDecision").value("PROMOTE"))
                .andExpect(jsonPath("$.auditResult.overallStatus").value("PASS"));

        // WARN -> PROMOTE
        when(auditExecutionService.executeAudit(any(), any(), any())).thenReturn(mockAudit("WARN"));
        mockMvc.perform(post("/api/v1/projects/" + projectId + "/ci/audits")
                        .header("X-CI-Token", rawCiToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.releaseDecision").value("PROMOTE"))
                .andExpect(jsonPath("$.auditResult.overallStatus").value("WARN"));

        // FAIL -> BLOCK
        when(auditExecutionService.executeAudit(any(), any(), any())).thenReturn(mockAudit("FAIL"));
        mockMvc.perform(post("/api/v1/projects/" + projectId + "/ci/audits")
                        .header("X-CI-Token", rawCiToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.releaseDecision").value("BLOCK"))
                .andExpect(jsonPath("$.auditResult.overallStatus").value("FAIL"));

        // Verify CI records persisted with correct project ID, audit type, and system user
        UUID pid = UUID.fromString(projectId);
        List<AuditRecord> persisted = auditRepository.findAll().stream()
                .filter(r -> pid.equals(r.getProjectId()))
                .filter(r -> "CI".equals(r.getAuditType()))
                .toList();

        assertEquals(3, persisted.size());
        for (AuditRecord rec : persisted) {
            assertEquals(AuditHistoryService.SYSTEM_USER_ID, rec.getUserId());
            assertEquals("CI", rec.getAuditType());
            assertEquals(pid, rec.getProjectId());
        }
    }

    @Test
    void ciAuditRejectsCrossProjectTokenAndUnknownProject() throws Exception {
        String userA = "ua_" + System.currentTimeMillis() + "@example.com";
        String userB = "ub_" + System.currentTimeMillis() + "@example.com";
        String tokenA = getAuthToken(userA);
        String tokenB = getAuthToken(userB);

        String projectA = createProject(tokenA, "Project A");
        String projectB = createProject(tokenB, "Project B");

        String ciTokenForA = createCiToken(tokenA, projectA, "Pipe A");

        // Token for project A must not authenticate against project B
        mockMvc.perform(post("/api/v1/projects/" + projectB + "/ci/audits")
                        .header("X-CI-Token", ciTokenForA))
                .andExpect(status().isUnauthorized());

        // Token for project A must not authenticate against an unknown project
        mockMvc.perform(post("/api/v1/projects/" + UUID.randomUUID() + "/ci/audits")
                        .header("X-CI-Token", ciTokenForA))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ciAuditEndpointRequiresXCiTokenHeader() throws Exception {
        // Without JWT or X-CI-Token, Spring Security rejects with 401
        String token = getAuthToken("noheader_" + System.currentTimeMillis() + "@example.com");
        String projectId = createProject(token, "No Header Project");

        mockMvc.perform(post("/api/v1/projects/" + projectId + "/ci/audits"))
                .andExpect(status().isUnauthorized());

        // With JWT but without X-CI-Token header, Spring returns 401 because it's caught by security filter
        mockMvc.perform(post("/api/v1/projects/" + projectId + "/ci/audits")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}
