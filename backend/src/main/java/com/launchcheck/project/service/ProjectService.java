package com.launchcheck.project.service;

import com.launchcheck.audit.safety.UrlSafetyPolicy;
import com.launchcheck.project.dto.CreateProjectRequest;
import com.launchcheck.project.dto.ProjectResponse;
import com.launchcheck.project.dto.UpdateProjectRequest;
import com.launchcheck.project.entity.Project;
import com.launchcheck.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UrlSafetyPolicy urlSafetyPolicy;

    public ProjectService(ProjectRepository projectRepository, UrlSafetyPolicy urlSafetyPolicy) {
        this.projectRepository = projectRepository;
        this.urlSafetyPolicy = urlSafetyPolicy;
    }

    @Transactional
    public ProjectResponse createProject(UUID userId, CreateProjectRequest request) {
        urlSafetyPolicy.parseAndValidate(request.frontendUrl());
        urlSafetyPolicy.parseAndValidate(request.backendUrl());

        Instant now = Instant.now();
        Project project = new Project(
                UUID.randomUUID(),
                userId,
                request.name().trim(),
                request.frontendUrl().trim(),
                request.backendUrl().trim(),
                now,
                now
        );

        Project saved = projectRepository.save(project);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listProjects(UUID userId) {
        return projectRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(UUID userId, UUID projectId) {
        Project project = projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        return mapToResponse(project);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectInternal(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        return mapToResponse(project);
    }

    @Transactional
    public ProjectResponse updateProject(UUID userId, UUID projectId, UpdateProjectRequest request) {
        Project project = projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        urlSafetyPolicy.parseAndValidate(request.frontendUrl());
        urlSafetyPolicy.parseAndValidate(request.backendUrl());

        project.setName(request.name().trim());
        project.setFrontendUrl(request.frontendUrl().trim());
        project.setBackendUrl(request.backendUrl().trim());
        project.setUpdatedAt(Instant.now());

        Project saved = projectRepository.save(project);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteProject(UUID userId, UUID projectId) {
        Project project = projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        projectRepository.delete(project);
    }

    private ProjectResponse mapToResponse(Project p) {
        return new ProjectResponse(
                p.getId(),
                p.getName(),
                p.getFrontendUrl(),
                p.getBackendUrl(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
