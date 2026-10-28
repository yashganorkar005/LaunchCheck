package com.launchcheck.project;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.launchcheck.auth.dto.AuthResponse;
import com.launchcheck.auth.dto.RegisterRequest;
import com.launchcheck.project.dto.CreateProjectRequest;
import com.launchcheck.project.dto.UpdateProjectRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
public class ProjectIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    void projectCrudAndCrossUserIsolationFlow() throws Exception {
        String tokenUser1 = getAuthToken("user1_" + System.currentTimeMillis() + "@example.com", "Password123!");
        String tokenUser2 = getAuthToken("user2_" + System.currentTimeMillis() + "@example.com", "Password123!");

        // 1. User 1 creates a project
        CreateProjectRequest createReq = new CreateProjectRequest(
                "User 1 App",
                "https://frontend.example.com",
                "https://backend.example.com"
        );

        String createRes = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + tokenUser1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.name").value("User 1 App"))
                .andReturn().getResponse().getContentAsString();

        String projectId = objectMapper.readTree(createRes).get("id").asText();

        // 2. User 1 lists projects -> sees 1 project
        mockMvc.perform(get("/api/v1/projects")
                        .header("Authorization", "Bearer " + tokenUser1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("User 1 App"));

        // 3. User 2 lists projects -> sees 0 projects
        mockMvc.perform(get("/api/v1/projects")
                        .header("Authorization", "Bearer " + tokenUser2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // 4. User 2 attempts to get User 1's project -> fails with BAD_REQUEST
        mockMvc.perform(get("/api/v1/projects/" + projectId)
                        .header("Authorization", "Bearer " + tokenUser2))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        // 5. User 1 updates project
        UpdateProjectRequest updateReq = new UpdateProjectRequest(
                "User 1 App Updated",
                "https://frontend2.example.com",
                "https://backend2.example.com"
        );
        mockMvc.perform(put("/api/v1/projects/" + projectId)
                        .header("Authorization", "Bearer " + tokenUser1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("User 1 App Updated"));

        // 6. User 2 attempts to delete User 1's project -> fails
        mockMvc.perform(delete("/api/v1/projects/" + projectId)
                        .header("Authorization", "Bearer " + tokenUser2))
                .andExpect(status().isBadRequest());

        // 7. User 1 deletes project -> 204
        mockMvc.perform(delete("/api/v1/projects/" + projectId)
                        .header("Authorization", "Bearer " + tokenUser1))
                .andExpect(status().isNoContent());

        // 8. User 1 lists projects -> 0
        mockMvc.perform(get("/api/v1/projects")
                        .header("Authorization", "Bearer " + tokenUser1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void ciAuditRequiresCiTokenAndRejectsCrossProjectAndInvalidCredentials() throws Exception {
        String authToken = getAuthToken("ci_owner_" + System.currentTimeMillis() + "@example.com", "Password123!");
        CreateProjectRequest create = new CreateProjectRequest("CI project", "https://frontend.example.com", "https://backend.example.com");
        String createJson = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String projectId = objectMapper.readTree(createJson).get("id").asText();

        mockMvc.perform(post("/api/v1/projects/" + projectId + "/ci/audits"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/projects/" + projectId + "/ci/audits")
                        .header("X-CI-Token", "lc_ci_invalid"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/projects/" + UUID.randomUUID() + "/ci/audits")
                        .header("X-CI-Token", "lc_ci_invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedAccessToProjectsIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isUnauthorized());
    }
}
