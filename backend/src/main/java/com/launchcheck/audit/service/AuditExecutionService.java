package com.launchcheck.audit.service;

import com.launchcheck.audit.api.ApiSmokeTestResult;
import com.launchcheck.audit.api.AuditRequest;
import com.launchcheck.audit.api.AuditResponse;
import com.launchcheck.audit.http.PinnedHttpProbe;
import com.launchcheck.audit.safety.TargetUrl;
import com.launchcheck.audit.safety.UrlSafetyPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AuditExecutionService {

    private final UrlSafetyPolicy policy;
    private final PinnedHttpProbe probe;
    private final ApiSmokeTestService smokeTestService;

    @Autowired
    public AuditExecutionService(UrlSafetyPolicy policy) {
        this(policy, new PinnedHttpProbe(policy), new ApiSmokeTestService(policy));
    }

    AuditExecutionService(UrlSafetyPolicy policy, PinnedHttpProbe probe, ApiSmokeTestService smokeTestService) {
        this.policy = policy;
        this.probe = probe;
        this.smokeTestService = smokeTestService;
    }

    public AuditResponse executeAudit(String frontendUrlStr, String backendUrlStr, List<AuditRequest.ApiCheck> apiChecks) {
        Instant start = Instant.now();
        TargetUrl frontend = policy.parseAndValidate(frontendUrlStr);
        TargetUrl backend = policy.parseAndValidate(backendUrlStr);

        AuditResponse.TargetResult frontendResult = runCheck("FRONTEND", frontend);
        AuditResponse.TargetResult backendResult = runCheck("BACKEND", backend);

        List<ApiSmokeTestResult> apiTestResults = null;
        if (apiChecks != null && !apiChecks.isEmpty()) {
            apiTestResults = smokeTestService.runTests(backend, apiChecks);
        }

        Instant end = Instant.now();

        boolean hasFailures = "FAIL".equals(frontendResult.status()) ||
                              "FAIL".equals(backendResult.status()) ||
                              (apiTestResults != null && apiTestResults.stream().anyMatch(t -> "FAIL".equals(t.status())));

        boolean hasWarnings = "WARN".equals(frontendResult.status()) ||
                              "WARN".equals(backendResult.status()) ||
                              (apiTestResults != null && apiTestResults.stream().anyMatch(t -> "WARN".equals(t.status())));

        String overall = hasFailures ? "FAIL" : (hasWarnings ? "WARN" : "PASS");

        return new AuditResponse(
                UUID.randomUUID(),
                start,
                end,
                end.toEpochMilli() - start.toEpochMilli(),
                overall,
                List.of(frontendResult, backendResult),
                apiTestResults,
                null
        );
    }

    private AuditResponse.TargetResult runCheck(String role, TargetUrl target) {
        try {
            var result = probe.check(target);

            String status = switch (result.status()) {
                case "PASS" -> "PASS";
                case "FAIL" -> "FAIL";
                default -> "WARN";
            };

            Integer httpStatus = result.httpStatus() == 0 ? null : result.httpStatus();
            Long responseTimeMs = result.elapsedMs() == 0 ? null : result.elapsedMs();

            List<AuditResponse.SecurityHeaderAssessment> securityHeaders = result.securityHeaders() == null
                    ? List.of()
                    : result.securityHeaders().stream()
                            .map(header -> new AuditResponse.SecurityHeaderAssessment(
                                    header.headerName(),
                                    header.status(),
                                    header.value(),
                                    header.diagnostic()
                            ))
                            .toList();

            return new AuditResponse.TargetResult(
                    role,
                    target.host(),
                    status,
                    httpStatus,
                    responseTimeMs,
                    new AuditResponse.TlsResult(
                            result.tls().status(),
                            result.tls().certificateExpiresAt(),
                            result.tls().diagnostic()
                    ),
                    List.of(),
                    result.diagnostic(),
                    "No action required",
                    securityHeaders
            );
        } catch (com.launchcheck.audit.safety.UnsafeTargetException ex) {
            return new AuditResponse.TargetResult(
                    role,
                    target.host(),
                    "FAIL",
                    null,
                    null,
                    new AuditResponse.TlsResult("FAIL", null, "Unsafe destination"),
                    List.of(),
                    "Blocked by safety policy: " + ex.code(),
                    "Provide a public, globally routable HTTP/HTTPS target",
                    List.of()
            );
        } catch (Exception e) {
            return new AuditResponse.TargetResult(
                    role,
                    target.host(),
                    "FAIL",
                    null,
                    null,
                    new AuditResponse.TlsResult("FAIL", null, "Connection failed"),
                    List.of(),
                    "Probe failed: " + e.getMessage(),
                    "Check reachability and DNS",
                    List.of()
            );
        }
    }
}
