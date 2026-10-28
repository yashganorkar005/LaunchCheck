# LaunchCheck CI Integration Guide

## Overview
LaunchCheck supports project-scoped CI service tokens for machine-to-machine integration, release gating, and automated pipeline verification. CI integration allows continuous deployment and build pipelines to trigger preflight checks and enforce quality/security gates automatically without exposing human user credentials.

---

## 1. Token Creation & Management
CI tokens are project-scoped and managed by authenticated project owners via the project settings or API.

### Endpoint
`POST /api/v1/projects/{projectId}/ci-tokens`

### Request Body
```json
{
  "tokenName": "GitHub Actions Release Pipeline",
  "expiresAt": "2027-01-01T00:00:00Z"
}
```

### Response
```json
{
  "rawToken": "lc_ci_a1b2c3d4e5f6g7h8i9j0..."
}
```
> **IMPORTANT:** The raw token is displayed **only once** upon creation. LaunchCheck stores strictly the SHA-256 hash and the 16-character prefix (`lc_ci_`) in the database. Raw tokens cannot be retrieved later.

---

## 2. Secure Secret Configuration in CI
Store the raw token securely as a repository or organization secret in your CI provider (e.g., GitHub Actions Secrets, GitLab CI/CD Variables).

- **Secret Name Example:** `LAUNCHCHECK_CI_TOKEN`
- Never hardcode tokens or commit them to source control.
- Never output the raw token or its hash in build logs.

---

## 3. Triggering CI Audits & Release Gating
Pipelines invoke the CI audit endpoint to evaluate the project's frontend and backend targets using stored project configurations.

### Endpoint
`POST /api/v1/projects/{projectId}/ci/audits`

### Headers
- `X-CI-Token: lc_ci_...`
- `Content-Type: application/json`

### Response
```json
{
  "auditResult": {
    "auditId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "startedAt": "2026-10-27T12:00:00Z",
    "completedAt": "2026-10-27T12:00:05Z",
    "durationMs": 5000,
    "overallStatus": "PASS",
    "targets": [...]
  },
  "executionId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "releaseDecision": "PROMOTE"
}
```

### Release Decisions
- **`PROMOTE`**: Triggered when overall status is `PASS` or `WARN`. The pipeline may proceed with deployment.
- **`BLOCK`**: Triggered when overall status is `FAIL`. The pipeline must abort or halt deployment.

---

## 4. Token Revocation
If a token is compromised or no longer needed, it can be revoked immediately by the project owner.

> **Implementation note:** The current backend exposes token generation and validation. A revoke controller route is not currently wired in `ProjectController`; revocation is available through `CiTokenService.revokeToken(...)` and must not be treated as an available HTTP endpoint until a controller route is added and tested.

### Service operation
`CiTokenService.revokeToken(tokenId, ownerUserId)`

The operation requires the owning user's ID and rejects a different owner. Once `revokedAt` is set, validation fails closed.

---

## 5. Security Controls & Safeguards
- **Tenant Isolation:** A CI token for Project A is cryptographically and logically bound to Project A. It is strictly rejected if used against Project B.
- **Separate Authentication:** CI endpoints use dedicated header-based token authentication (`X-CI-Token`), avoiding JWT dependency.
- **Audit Persistence:** Successful CI audits are automatically recorded in the database with `audit_type = 'CI'`, tied to a system principal (`00000000-0000-0000-0000-000000000001`), ensuring they appear in project audit history without cluttering personal user histories.
- **SSRF & TLS Protection:** All probes respect strict URL safety policies, private network blocking, and TLS certificate validation.
