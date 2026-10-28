package com.launchcheck.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.launchcheck.audit.api.AuditRequest;
import com.launchcheck.audit.api.AuditResponse;
import com.launchcheck.audit.service.AuditExecutionService;
import com.launchcheck.auth.dto.AuthResponse;
import com.launchcheck.auth.dto.RegisterRequest;
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

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
public class ProjectAuditHistoryIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuditExecutionService auditExecutionService;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    private String getAuthToken(String email, String password) throws Exception {
        RegisterRequest register = new RegisterRequest(email, password);
        String res = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andReturn().getResponse().getContentAsString();
        AuthResponse auth = objectMapper.readValue(res, AuthResponse.class);
        return auth.token();
    }

    @Test
    void projectAuditExecutesOnlyOnce() throws Exception {
        String token = getAuthToken("useronce_" + System.currentTimeMillis() + "@example.com", "Password123!");

        CreateProjectRequest projectReq = new CreateProjectRequest(
                "Once Project",
                "https://frontend.example.com",
                "https://backend.example.com"
        );

        String createProjectRes = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String projectId = objectMapper.readTree(createProjectRes).get("id").asText();

        UUID mockAuditId = UUID.randomUUID();
        AuditResponse mockResponse = new AuditResponse(
                mockAuditId,
                Instant.now().minusSeconds(5),
                Instant.now(),
                5000L,
                "PASS",
                List.of(
                        new AuditResponse.TargetResult("FRONTEND", "frontend.example.com", "PASS", 200, 120L,
                                new AuditResponse.TlsResult("PASS", "2027-01-01", "Valid"), List.of(), "Healthy", "None", List.of()),
                        new AuditResponse.TargetResult("BACKEND", "backend.example.com", "PASS", 200, 150L,
                                new AuditResponse.TlsResult("PASS", "2027-01-01", "Valid"), List.of(), "Healthy", "None", List.of())
                ),
                List.of(),
                null
        );

        when(auditExecutionService.executeAudit(any(), any(), any()))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/projects/" + projectId + "/audits")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(auditExecutionService, org.mockito.Mockito.times(1)).executeAudit(any(), any(), any());
    }

    @Test
    void projectAuditPersistenceAndHistoryFlowWithIsolation() throws Exception {
        String tokenUser1 = getAuthToken("auditor1_" + System.currentTimeMillis() + "@example.com", "Password123!");
        String tokenUser2 = getAuthToken("auditor2_" + System.currentTimeMillis() + "@example.com", "Password123!");

        // Mock audit execution result
        UUID mockAuditId = UUID.randomUUID();
        AuditResponse mockResponse = new AuditResponse(
                mockAuditId,
                Instant.now().minusSeconds(5),
                Instant.now(),
                5000L,
                "PASS",
                List.of(
                        new AuditResponse.TargetResult("FRONTEND", "frontend.example.com", "PASS", 200, 120L,
                                new AuditResponse.TlsResult("PASS", "2027-01-01", "Valid"), List.of(), "Healthy", "None", List.of()),
                        new AuditResponse.TargetResult("BACKEND", "backend.example.com", "PASS", 200, 150L,
                                new AuditResponse.TlsResult("PASS", "2027-01-01", "Valid"), List.of(), "Healthy", "None", List.of())
                ),
                List.of(),
                null
        );

        when(auditExecutionService.executeAudit(eq("https://frontend.example.com"), eq("https://backend.example.com"), any()))
                .thenReturn(mockResponse);

        // 1. User 1 creates a project
        CreateProjectRequest projectReq = new CreateProjectRequest(
                "Audit Project",
                "https://frontend.example.com",
                "https://backend.example.com"
        );

        String createProjectRes = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + tokenUser1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String projectId = objectMapper.readTree(createProjectRes).get("id").asText();

        // 2. User 1 triggers an audit for the project
        mockMvc.perform(post("/api/v1/projects/" + projectId + "/audits")
                        .header("Authorization", "Bearer " + tokenUser1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auditId").value(mockAuditId.toString()))
                .andExpect(jsonPath("$.overallStatus").value("PASS"));

        // 3. User 1 lists audits for the project
        mockMvc.perform(get("/api/v1/projects/" + projectId + "/audits")
                        .header("Authorization", "Bearer " + tokenUser1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(mockAuditId.toString()))
                .andExpect(jsonPath("$.content[0].projectId").value(projectId))
                .andExpect(jsonPath("$.content[0].overallStatus").value("PASS"));

        // 4. User 1 retrieves full audit history by ID
        mockMvc.perform(get("/api/v1/audits/history/" + mockAuditId)
                        .header("Authorization", "Bearer " + tokenUser1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auditId").value(mockAuditId.toString()))
                .andExpect(jsonPath("$.overallStatus").value("PASS"))
                .andExpect(jsonPath("$.targets", hasSize(2)));

        // 5. Cross-user isolation: User 2 tries to run audit on User 1's project -> 400
        mockMvc.perform(post("/api/v1/projects/" + projectId + "/audits")
                        .header("Authorization", "Bearer " + tokenUser2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        // 6. Cross-user isolation: User 2 tries to list audits for User 1's project -> 400
        mockMvc.perform(get("/api/v1/projects/" + projectId + "/audits")
                        .header("Authorization", "Bearer " + tokenUser2))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        // 7. Cross-user isolation: User 2 tries to get User 1's audit history -> 400
        mockMvc.perform(get("/api/v1/audits/history/" + mockAuditId)
                        .header("Authorization", "Bearer " + tokenUser2))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        // 8. Unauthenticated access is rejected
        mockMvc.perform(get("/api/v1/projects/" + projectId + "/audits"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/audits/history/" + mockAuditId))
                .andExpect(status().isUnauthorized());

        // 9. Public preflight audit still works without authentication
        AuditRequest preflightReq = new AuditRequest("https://frontend.example.com", "https://backend.example.com", List.of());
        mockMvc.perform(post("/api/v1/audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(preflightReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auditId").value(mockAuditId.toString()));
    }
}
