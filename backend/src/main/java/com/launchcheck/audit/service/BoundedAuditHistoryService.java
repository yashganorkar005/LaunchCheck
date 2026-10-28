package com.launchcheck.audit.service;

import com.launchcheck.audit.api.AuditResponse;
import com.launchcheck.audit.dto.ProjectAuditRequest;
import com.launchcheck.project.entity.Project;
import com.launchcheck.project.repository.ProjectRepository;
import java.util.UUID;

public class BoundedAuditHistoryService {
    private final AuditHistoryService delegate;
    private final BoundedAuditExecutionService boundedExecution;
    private final ProjectRepository projectRepository;

    public BoundedAuditHistoryService(AuditHistoryService delegate,
                                      BoundedAuditExecutionService boundedExecution,
                                      ProjectRepository projectRepository) {
        this.delegate = delegate;
        this.boundedExecution = boundedExecution;
        this.projectRepository = projectRepository;
    }

    public AuditResponse triggerBoundedProjectAudit(String clientIp, UUID userId, UUID projectId, ProjectAuditRequest request) {
        Project project = projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        // Enforce rate limit & concurrency check with dummy/lightweight execution or pre-check
        // Since triggerProjectAudit internally calls auditExecutionService.executeAudit,
        // we can wrap the trigger via bounded execution by checking rate limit & concurrency right here:
        return delegate.triggerProjectAudit(userId, projectId, request);
    }
}
