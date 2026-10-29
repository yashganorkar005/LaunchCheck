package com.launchcheck.audit.api;

import com.launchcheck.audit.dto.AuditSummaryResponse;
import com.launchcheck.audit.dto.SingleSmokeTestRequest;
import com.launchcheck.audit.dto.SingleSmokeTestResponse;
import com.launchcheck.audit.safety.TargetUrl;
import com.launchcheck.audit.safety.UrlSafetyPolicy;
import com.launchcheck.audit.service.ApiSmokeTestService;
import com.launchcheck.audit.service.AuditExecutionService;
import com.launchcheck.audit.service.AuditHistoryService;
import com.launchcheck.audit.service.BoundedAuditExecutionService;
import com.launchcheck.audit.service.CorsDiagnosticService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/audits")
public class AuditController {
    private final UrlSafetyPolicy policy;
    private final CorsDiagnosticService corsService;
    private final ApiSmokeTestService smokeTestService;
    private final BoundedAuditExecutionService boundedAuditExecutionService;
    private final AuditHistoryService auditHistoryService;

    public AuditController(UrlSafetyPolicy policy,
                           AuditExecutionService auditExecutionService,
                           AuditHistoryService auditHistoryService) {
        this.policy = policy;
        this.corsService = new CorsDiagnosticService(policy);
        this.smokeTestService = new ApiSmokeTestService(policy);
        this.boundedAuditExecutionService = new BoundedAuditExecutionService(auditExecutionService);
        this.auditHistoryService = auditHistoryService;
    }

    @PostMapping
    public ResponseEntity<AuditResponse> createAudit(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody AuditRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        AuditResponse response = boundedAuditExecutionService.executeBoundedAudit(
                clientIp,
                request.frontendUrl(),
                request.backendUrl(),
                request.apiChecks(),
                request.bearerToken()
        );

        if (userId != null) {
            try {
                auditHistoryService.recordPreflightAudit(userId, null, request.backendUrl(), response);
            } catch (Exception ignored) {
                // Do not fail execution response if optional history recording hits transient storage issue
            }
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/smoke")
    public ResponseEntity<SingleSmokeTestResponse> executeSmokeTest(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody SingleSmokeTestRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        boundedAuditExecutionService.checkRateLimit("smoke:" + clientIp);

        TargetUrl target = policy.parseAndValidate(request.url());

        List<AuditRequest.JsonAssertion> jsonAssertions = new ArrayList<>();
        if (request.jsonPointer() != null && !request.jsonPointer().isBlank()) {
            jsonAssertions.add(new AuditRequest.JsonAssertion(request.jsonPointer().trim(), request.expectedJsonValue()));
        }

        AuditRequest.ApiCheck check = new AuditRequest.ApiCheck(
                target.path(),
                "GET",
                request.expectedStatus(),
                List.of(),
                jsonAssertions
        );

        long start = System.currentTimeMillis();
        var results = smokeTestService.runTests(target, List.of(check), request.bearerToken());
        long duration = System.currentTimeMillis() - start;

        SingleSmokeTestResponse response;
        if (results == null || results.isEmpty()) {
            response = new SingleSmokeTestResponse(
                    false,
                    "FAIL",
                    request.expectedStatus(),
                    null,
                    duration,
                    "No check was executed",
                    "No check was executed",
                    false,
                    null
            );
        } else {
            var r = results.get(0);
            boolean isPass = "PASS".equals(r.status());
            response = new SingleSmokeTestResponse(
                    isPass,
                    r.status(),
                    r.expectedStatus(),
                    r.actualStatus(),
                    duration,
                    r.diagnostic(),
                    isPass ? null : r.diagnostic(),
                    r.jsonAssertionPassed(),
                    r.jsonAssertionDiagnostic()
            );
        }

        if (userId != null) {
            try {
                auditHistoryService.recordSmokeAudit(userId, request.url(), request, response);
            } catch (Exception ignored) {
            }
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<Page<AuditSummaryResponse>> getUserAudits(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        int boundedSize = Math.min(Math.max(size, 1), 100);
        int boundedPage = Math.max(page, 0);
        return ResponseEntity.ok(auditHistoryService.getUserAudits(userId, type, PageRequest.of(boundedPage, boundedSize)));
    }

    @GetMapping("/history/{id}")
    public ResponseEntity<AuditResponse> getAuditHistory(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(auditHistoryService.getAuditById(userId, id));
    }

    @DeleteMapping("/history/{id}")
    public ResponseEntity<Void> deleteAuditHistoryRecord(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            auditHistoryService.deleteAuditRecord(userId, id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/history")
    public ResponseEntity<Void> clearUserHistory(
            @AuthenticationPrincipal UUID userId) {
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        auditHistoryService.clearUserHistory(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/cors")
    public ResponseEntity<CorsDiagnosticResult> performCorsDiagnostics(
            @AuthenticationPrincipal UUID userId,
            @RequestParam String backendUrl,
            @RequestParam String origin,
            @RequestParam String endpointPath,
            HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        boundedAuditExecutionService.checkRateLimit("cors:" + clientIp);

        long start = System.currentTimeMillis();
        TargetUrl target = policy.parseAndValidate(backendUrl);
        CorsDiagnosticResult result = corsService.diagnose(target, origin, endpointPath);
        long duration = System.currentTimeMillis() - start;

        if (userId != null) {
            try {
                auditHistoryService.recordCorsAudit(userId, backendUrl + endpointPath, result, duration);
            } catch (Exception ignored) {
            }
        }

        return ResponseEntity.ok(result);
    }

    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "unknown";
    }
}
