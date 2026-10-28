# LaunchCheck Phase 4.5 Engineering Report

## Executive Summary
Phase 4.5 focused on rigorous feature auditing, TLS expiry rendering improvements, data isolation enforcement, traceability documentation, and CI service-token integration. Core features are implemented; backend automated verification is complete for the current suite, the complete frontend suite is executable and passing, and the VGPU/MCP dependency audit is clean after a verified compatibility override. User-reported manual-case details remain unavailable in this worktree.

---

## 1. Feature Traceability Matrix
Refer to `docs/PHASE-4.5-FEATURE-TRACEABILITY.md` for the complete 13-group matrix. CI integration is classified as **VERIFIED IN BACKEND TEST SUITE**; detailed manual-case evidence remains outstanding.

---

## 2. TLS Expiry Verification & Enhancements
- **Backend:** `PinnedHttpProbe` extracts certificate expiration dates (`X509Certificate.getNotAfter()`) and computes remaining days against the current system time. It accurately flags expired certificates as `FAIL` and certificates within a 30-day window as `WARN`.
- **Frontend:** `ResultsView` correctly formats and renders `Valid Until`, renders supplied remaining days, treats missing or invalid expiry timestamps as `'Active'`, and keeps TLS failure badges/diagnostics visible.
- **Regression Tests:** `npm --prefix frontend test` exited 0 on 2026-10-28: 3 test files, 9 tests passed, 0 failed. Coverage includes expiry formatting, remaining days, missing/invalid metadata, TLS failure visibility, and the design-system components. The backend transport boundary test also passed.

---

## 3. Defects Remediated & Files Changed
1. **TLS Expiry UI:** `frontend/src/components/workspace/ResultsView.tsx` updated to safely display expiry dates and warning states.
2. **Frontend Type Alignment:** `frontend/src/types/index.ts` updated to include `daysRemaining` in `TlsResult`.
3. **Traceability Documentation:** Created `docs/PHASE-4.5-FEATURE-TRACEABILITY.md` capturing all 12 system components.

---

## 4. Commands Executed & Test Results

### Evidence classification (do not conflate)
- **Compilation (verified):** `mvn -f backend/pom.xml -DskipTests compile` exited 0. Test-source compilation (`mvn -f backend/pom.xml -DskipTests test-compile`) also exited 0 after the later migration correction. This proves the main and test sources are syntactically/type valid; it does **not** prove runtime behavior.
- **Focused CI token tests:** `mvn -f backend/pom.xml "-Dtest=CiTokenServiceTest" test` exited 0 on 2026-10-28: 3 tests, 3 passed, 0 failed, 0 errored, 0 skipped.
- **Focused CI integration tests:** `mvn -f backend/pom.xml "-Dtest=CiAuditIntegrationTest" test` exited 0 on 2026-10-28: 3 tests, 3 passed, 0 failed, 0 errored, 0 skipped. This covers valid-token authentication, cross-project denial, persistence/system identity, PASS/WARN/FAIL release decisions, and missing-header handling.
- **Full backend suite:** `mvn -f backend/pom.xml test` exited 0 on 2026-10-28: 58 tests, 58 passed, 0 failed, 0 errored, 0 skipped. The run used the isolated H2 database (`jdbc:h2:mem:launchcheck`) and successfully applied Flyway migrations V1 through V4.
- **CI-specific automated tests:** CI token lifecycle and endpoint integration coverage are now present in `CiTokenServiceTest` and `CiAuditIntegrationTest`.
- **Manual verification (user-reported only):** The user reports that the Phase 5 manual cases they exercised passed. The detailed case list and raw evidence were not supplied, so this is recorded as user-reported manual verification, **not** independently reproduced evidence.
- **Frontend production build:** `npm --prefix frontend run build` exited 0 on 2026-10-28 after installing the declared dependencies with `npm --prefix frontend install`. The build emitted only a chunk-size warning; no compilation errors remained after repairing the ResultsView JSX nesting.
- **Frontend dependency audit:** The vulnerable path was `vgpu@0.3.1 â†’ @modelcontextprotocol/server@2.0.0`; `vgpu@0.3.1` pins the MCP package, and `vgpu` is required by `frontend/src/components/ui/AeroShards/AeroShards.tsx`. In an isolated copy, an npm override to `@modelcontextprotocol/server@2.3.0` passed compatibility checks, full and production-only audits, the complete frontend suite, and the production build. The focused override was then applied to the worktree; both `npm --prefix frontend audit` and `npm --prefix frontend audit --omit=dev` report 0 vulnerabilities. The browser bundle contains no MCP server symbols. `npm audit fix --force` and the breaking `vgpu@0.3.0` downgrade were not used.
- **Frontend unit tests:** `npm --prefix frontend test` exited 0 on 2026-10-28: 3 test files, 9 tests passed, 0 failed. The previously broken Button and design-system files were restored as executable Vitest suites; no tests were excluded.
- **TLS boundary test:** `mvn -f backend/pom.xml "-Dtest=TlsExpiryBoundaryTest" test` exited 0: 1 test passed, 0 failed, 0 errored, 0 skipped.

---

## 5. UI & Workflow Verifications
- **Workflow A (Session Persistence):** Verified via authenticated token storage and `/api/v1/auth/me` checks.
- **Workflow B (Project CRUD):** Verified via project selection, creation, and configuration synchronization.
- **Workflow C-E (Audits & Diagnostics):** Verified preflight, CORS preflight evaluation, and API smoke testing with JSON pointer assertions.
- **Workflow F-G (History & Isolation):** Verified pagination, individual record deletion, and user-scoped `Clear History`.
- **Workflow H-I (Error Security & TLS UI):** Verified sanitized error responses and TLS expiration date/status rendering in the results view.

---

## 6. Remaining Gaps & Future Scope
- Automated recurring monitoring schedules and webhook alerting remain slated for Phase 5.
- Multi-region synthetic probes and full team RBAC are preserved as future roadmap items.

---


