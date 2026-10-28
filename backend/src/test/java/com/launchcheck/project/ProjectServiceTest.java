package com.launchcheck.project;

import com.launchcheck.audit.safety.TargetUrl;
import com.launchcheck.audit.safety.UnsafeTargetException;
import com.launchcheck.audit.safety.UrlSafetyPolicy;
import com.launchcheck.project.dto.CreateProjectRequest;
import com.launchcheck.project.dto.ProjectResponse;
import com.launchcheck.project.dto.UpdateProjectRequest;
import com.launchcheck.project.entity.Project;
import com.launchcheck.project.repository.ProjectRepository;
import com.launchcheck.project.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ProjectServiceTest {

    private ProjectRepository projectRepository;
    private UrlSafetyPolicy urlSafetyPolicy;
    private ProjectService projectService;

    private final UUID user1 = UUID.randomUUID();
    private final UUID user2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectRepository.class);
        urlSafetyPolicy = mock(UrlSafetyPolicy.class);
        projectService = new ProjectService(projectRepository, urlSafetyPolicy);

        when(urlSafetyPolicy.parseAndValidate(anyString())).thenAnswer(inv -> {
            String url = inv.getArgument(0);
            if (url.contains("127.0.0.1") || url.contains("localhost")) {
                throw new UnsafeTargetException("UNSAFE_DESTINATION");
            }
            return new TargetUrl("https", "example.com", 443, "/");
        });
    }

    @Test
    void createProjectSuccessfullyWhenUrlsSafe() {
        CreateProjectRequest req = new CreateProjectRequest(
                "My Launch App",
                "https://frontend.example.com",
                "https://api.example.com"
        );

        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        ProjectResponse res = projectService.createProject(user1, req);

        assertNotNull(res);
        assertNotNull(res.id());
        assertEquals("My Launch App", res.name());
        assertEquals("https://frontend.example.com", res.frontendUrl());
        assertEquals("https://api.example.com", res.backendUrl());
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    void createProjectRejectsUnsafeUrls() {
        CreateProjectRequest req = new CreateProjectRequest(
                "Malicious App",
                "http://127.0.0.1:8080",
                "https://api.example.com"
        );

        assertThrows(UnsafeTargetException.class, () -> projectService.createProject(user1, req));
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void listProjectsReturnsOnlyOwnedProjects() {
        Project p1 = new Project(UUID.randomUUID(), user1, "P1", "https://app1.example.com", "https://api1.example.com", Instant.now(), Instant.now());
        when(projectRepository.findByUserIdOrderByCreatedAtDesc(user1)).thenReturn(List.of(p1));

        List<ProjectResponse> result = projectService.listProjects(user1);
        assertEquals(1, result.size());
        assertEquals("P1", result.get(0).name());
    }

    @Test
    void getProjectEnforcesUserIsolation() {
        UUID projectId = UUID.randomUUID();
        when(projectRepository.findByIdAndUserId(projectId, user2)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                projectService.getProject(user2, projectId)
        );
        assertTrue(ex.getMessage().contains("Project not found"));
    }

    @Test
    void updateProjectUpdatesFieldsAndValidatesUrls() {
        UUID projectId = UUID.randomUUID();
        Project existing = new Project(projectId, user1, "Old Name", "https://old.example.com", "https://old-api.example.com", Instant.now(), Instant.now());
        when(projectRepository.findByIdAndUserId(projectId, user1)).thenReturn(Optional.of(existing));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProjectRequest req = new UpdateProjectRequest(
                "Updated Name",
                "https://new.example.com",
                "https://new-api.example.com"
        );

        ProjectResponse updated = projectService.updateProject(user1, projectId, req);
        assertEquals("Updated Name", updated.name());
        assertEquals("https://new.example.com", updated.frontendUrl());
        assertEquals("https://new-api.example.com", updated.backendUrl());
    }

    @Test
    void deleteProjectDeletesWhenOwned() {
        UUID projectId = UUID.randomUUID();
        Project existing = new Project(projectId, user1, "To Delete", "https://del.example.com", "https://del-api.example.com", Instant.now(), Instant.now());
        when(projectRepository.findByIdAndUserId(projectId, user1)).thenReturn(Optional.of(existing));

        projectService.deleteProject(user1, projectId);
        verify(projectRepository).delete(existing);
    }

    @Test
    void deleteProjectThrowsWhenNotOwned() {
        UUID projectId = UUID.randomUUID();
        when(projectRepository.findByIdAndUserId(projectId, user2)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> projectService.deleteProject(user2, projectId));
        verify(projectRepository, never()).delete(any());
    }
}
