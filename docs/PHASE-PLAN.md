# LaunchCheck Phase Plan

## Phase 1 (Completed)
- Frontend React SPA with URL input form and structured results display.
- Spring Boot backend API (`POST /api/v1/audits`).
- Rigorous SSRF protection, destination IP pinning, candidate IP failover, and transport security tests.
- Documentation (`README.md`, `ARCHITECTURE.md`, `SECURITY.md`).

## Phase 2 (Completed)
- Cyberpunk visual theme redesign with high-contrast palette and status indicators.
- CORS preflight diagnostics for specified origins and backend endpoints (`GET /api/v1/audits/cors`).
- Configurable API smoke tests with header assertions and parsed RFC 6901 JSON Pointer assertions (`ApiSmokeTestService`).

## Phase 3 (Completed)
- User registration and login with BCrypt password hashing and stateless JWT token authentication (`/api/v1/auth/**`).
- Multi-tenant Project CRUD management (`/api/v1/projects/**`) with strict user isolation and SSRF target validation.
- Database persistence via Flyway schema migrations (`users`, `projects`, `audits` tables) using JPA/Hibernate.
- Project-linked audit execution and persistence (`POST /api/v1/projects/{id}/audits`).
- Bounded paginated audit history retrieval (`GET /api/v1/projects/{id}/audits`) and full historical audit report lookup (`GET /api/v1/audits/history/{id}`) with strict cross-user tenant isolation.
- Shared audit execution engine (`AuditExecutionService`) supporting both authenticated persistent runs and unauthenticated public preflights.
- Frontend Cyberpunk UI integration with auth modals, project switching/creation, and an audit history inspection drawer.

## Phase 4 & Beyond (Future)
- Scheduled recurring monitoring and webhook/email alert dispatches.
- Multi-region synthetic probing nodes.
- Team workspace collaboration and role-based access control (RBAC).
