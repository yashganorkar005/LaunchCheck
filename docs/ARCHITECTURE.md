# LaunchCheck Architecture

LaunchCheck is structured as a modular monolithic application featuring an independent React Single Page Application frontend and a Spring Boot MVC backend.

## Modules & Responsibilities (`backend/src/main/java/com/launchcheck/`)

### 1. `auth/` (Authentication & Security)
- **`User` & `UserRepository`**: Persisted operator identity with unique email and BCrypt password hash.
- **`AuthService`**: Manages registration, credential verification, and JWT generation.
- **`JwtService`**: Issues and verifies stateless HMAC-SHA256 signed JWT tokens (24-hour expiration).
- **`JwtAuthenticationFilter`**: Intercepts inbound HTTP requests, extracts Bearer tokens, validates signatures, and populates the `SecurityContext` with the authenticated `userId`.
- **`SecurityConfig`**: Spring Security 6+ filter chain allowing unauthenticated access to public endpoints (`/api/v1/auth/**`, `POST /api/v1/audits`, `/api/v1/audits/cors`, Swagger/Actuator) while requiring authentication for project and history operations.

### 2. `project/` (Multi-Tenant Project Management)
- **`Project` & `ProjectRepository`**: Persisted user projects containing target frontend and backend URLs.
- **`ProjectService`**: Handles project CRUD with strict tenant isolation (`findByIdAndUserId`). Validates target URLs against `UrlSafetyPolicy` upon project creation and updates to prevent storing SSRF vectors.
- **`ProjectController`**: Exposes `/api/v1/projects/**` endpoints.

### 3. `audit/` (Diagnostics & Persistence)
- **`safety/`**: Contains `UrlSafetyPolicy`, `GlobalAddressPolicy`, and address resolution. Enforces strict SSRF controls, domain validation, scheme and port checking, and IPv4/IPv6 global-unicast validation.
- **`http/`**: Contains `PinnedHttpProbe`, `PinnedHttpRequester`, and Apache HttpClient 5 integration. Implements destination-IP pinning with candidate IP failover to prevent DNS rebinding while retaining standard TLS hostname verification and SNI.
- **`service/`**:
  - `AuditExecutionService`: Core shared engine executing probes, TLS inspection, and API smoke tests for both public audits and saved project runs.
  - `AuditHistoryService`: Handles triggering project audits, saving `AuditRecord` entries to the database, querying paginated audit summaries, and deserializing full historical `AuditResponse` JSON snapshots.
  - `CorsDiagnosticService`: Runs preflight and actual CORS probes.
  - `ApiSmokeTestService`: Executes smoke tests and evaluates header and RFC 6901 JSON Pointer assertions on parsed documents.
- **`api/`**: `AuditController` and `RestExceptionHandler`.

### 4. `db/migration/` (Flyway Database Schema)
- `V1__init_auth_projects_audits.sql`: Defines tables for `users`, `projects`, and `audits` with UUID primary keys, foreign key constraints, indexes on `(user_id)` and `(project_id, started_at)`, and `TEXT` storage for full audit JSON snapshots.

## Multi-Tenant Isolation Architecture

1. **Compound Identity Verification:** All entity lookups and mutations query both entity `id` and the authenticated `userId` (e.g., `findByIdAndUserId`, `existsByIdAndUserId`).
2. **Horizontal Privilege Escalation Prevention:** Requests attempting to query, update, or audit projects or records belonging to another user are rejected with an explicit error, preventing cross-tenant information leakage.
3. **Bounded Query Resources:** Audit history queries enforce bounded page sizes (`Math.min(Math.max(size, 1), 100)`) preventing denial-of-service via unbounded queries.
