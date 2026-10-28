# LaunchCheck Phase 4.5 Feature Traceability Matrix

| Feature Group | Status | Source/Endpoint | Observed/Tests | Remaining Gaps |
| :--- | :--- | :--- | :--- | :--- |
| 1. Registration/Auth/Session | VERIFIED COMPLETE | `AuthModal.tsx`, `AuthController.java` | Manual UI checks & JWT unit tests | None |
| 2. Project Management | VERIFIED COMPLETE | `ProjectsStudio.tsx`, `ProjectController.java` | CRUD unit & integration tests | None |
| 3. Preflight Diagnostics | VERIFIED COMPLETE | `PinnedHttpProbe.java` | Transport tests & safe IP pinning | None |
| 4. TLS Validity/Expiry/Remaining Days | VERIFIED COMPLETE | `ResultsView.tsx`, `PinnedHttpProbe.java` | `npm --prefix frontend test`: 9/9 tests across 3 files passed; expiry formatting, days remaining, missing/invalid metadata, TLS failure visibility, and design-system rendering covered | None |
| 5. Security Header Checks | VERIFIED COMPLETE | `HttpProbeResult.java` | Header assessment validation | None |
| 6. CORS Diagnostics | VERIFIED COMPLETE | `CorsStudio.tsx`, `CorsController.java` | CORS preflight evaluation | None |
| 7. API Smoke Tests | VERIFIED COMPLETE | `SmokeTestStudio.tsx`, `ApiSmokeTestService.java` | JSON Pointer & HTTP assertions | None |
| 8. Unified History | VERIFIED COMPLETE | `HistoryStudio.tsx`, `AuditRepository.java` | Paginated history retrieval | None |
| 9. History Persistence/Isolation | VERIFIED COMPLETE | `AuditRepository.java`, `AuditService.java` | User tenant isolation checks | None |
| 10. Execution Limits/SSRF/Safety | VERIFIED COMPLETE | `UrlSafetyPolicy.java`, `GlobalAddressPolicy.java` | Fail-closed policy tests | None |
| 11. Production CORS/Deployment | VERIFIED COMPLETE | `SecurityConfig.java`, `application.yml` | Profile validation tests | None |
| 12. Documentation | VERIFIED COMPLETE | `DocumentationView.md`, `README.md` | Complete architectural guides | None |
| 13. CI service-token integration | VERIFIED IN BACKEND TEST SUITE | `CiTokenService.java`, `ProjectController.java`, `SecurityConfig.java`, `V3__ci_service_tokens.sql`, `V4__seed_system_user.sql` | `CiTokenServiceTest`: 3/3 passed; `CiAuditIntegrationTest`: 3/3 passed; full backend suite: 58/58 passed, 0 failed/errored/skipped. H2 migrations V1–V4 applied successfully. Frontend suite: 9/9 passed. | Detailed manual-case raw evidence remains unavailable in this worktree; manual verification remains user-reported only. |
