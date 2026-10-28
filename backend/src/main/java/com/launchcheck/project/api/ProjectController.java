package com.launchcheck.project.api;

import com.launchcheck.audit.api.AuditResponse;
import com.launchcheck.audit.api.CiAuditResponse;
import com.launchcheck.audit.service.AuditExecutionService;
import com.launchcheck.audit.service.AuditHistoryService;
import com.launchcheck.audit.service.BoundedAuditExecutionService;
import com.launchcheck.audit.service.CiTokenService;
import com.launchcheck.audit.dto.AuditSummaryResponse;
import com.launchcheck.audit.dto.ProjectAuditRequest;
import com.launchcheck.project.dto.*;
import com.launchcheck.project.service.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final AuditHistoryService auditHistoryService;
    private final BoundedAuditExecutionService boundedAuditExecutionService;
    private final CiTokenService ciTokenService;

    public ProjectController(ProjectService projectService,
                             AuditHistoryService auditHistoryService,
                             AuditExecutionService auditExecutionService,
                             CiTokenService ciTokenService) {
        this.projectService = projectService;
        this.auditHistoryService = auditHistoryService;
        this.boundedAuditExecutionService = new BoundedAuditExecutionService(auditExecutionService);
        this.ciTokenService = ciTokenService;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.ok(projectService.createProject(userId, request));
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> listProjects(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(projectService.listProjects(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProject(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(projectService.getProject(userId, id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProjectRequest request) {
        return ResponseEntity.ok(projectService.updateProject(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        projectService.deleteProject(userId, id);
        return ResponseEntity.noContent().build();
    }

    // --- CI Token Management ---

    @PostMapping("/{id}/ci-tokens")
    public ResponseEntity<CiTokenCreateResponse> createCiToken(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody CreateCiTokenRequest request) {
        // Validate project ownership
        projectService.getProject(userId, id);

        String rawToken = ciTokenService.generateToken(id, userId, request.tokenName(), request.expiresAt());
        return ResponseEntity.ok(new CiTokenCreateResponse(rawToken));
    }

    @PostMapping("/{id}/ci/audits")
    public ResponseEntity<CiAuditResponse> triggerCiAudit(
            @RequestHeader(value = "X-CI-Token", required = false) String ciToken,
            @PathVariable UUID id) {

        if (ciToken == null || ciToken.isBlank() || !ciTokenService.validateToken(ciToken, id)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Project exists and token is valid for it.
        // Audit execution uses stored project URLs (no overrides allowed).
        ProjectResponse project = projectService.getProjectInternal(id);

        AuditResponse audit = boundedAuditExecutionService.executeBoundedAudit(
                "ci-runner",
                project.frontendUrl(),
                project.backendUrl(),
                null
        );

        // Determine release decision
        String decision = "PASS".equals(audit.overallStatus()) || "WARN".equals(audit.overallStatus())
                ? "PROMOTE" : "BLOCK";

        auditHistoryService.recordCiAudit(id, "CI-Runner", audit, decision);

        return ResponseEntity.ok(new CiAuditResponse(audit, audit.auditId().toString(), decision));
    }

    // --- Existing project audit methods ---

    @PostMapping("/{id}/audits")
    public ResponseEntity<AuditResponse> triggerAudit(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id,
            @RequestBody(required = false) ProjectAuditRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr() != null ? httpRequest.getRemoteAddr() : "unknown";
        return ResponseEntity.ok(auditHistoryService.triggerProjectAudit(userId, id, request, clientIp));
    }

    @GetMapping("/{id}/audits")
    public ResponseEntity<Page<AuditSummaryResponse>> getProjectAudits(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int boundedSize = Math.min(Math.max(size, 1), 100);
        int boundedPage = Math.max(page, 0);
        return ResponseEntity.ok(auditHistoryService.getProjectAudits(userId, id, PageRequest.of(boundedPage, boundedSize)));
    }
}
