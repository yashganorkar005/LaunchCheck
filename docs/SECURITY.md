# LaunchCheck Security Model

LaunchCheck treats all target URLs and client inputs as hostile input and enforces multi-layered defense-in-depth protections.

## Core SSRF Protection

- **Permitted Schemes:** Only `http` and `https` are allowed.
- **Permitted Ports:** Standard ports 80 (HTTP) and 443 (HTTPS) only.
- **Address Range Restrictions:** Prohibits connections to loopback (`127.0.0.0/8`, `::1`), private networks (RFC 1918 `10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`, RFC 4193 `fc00::/7`), link-local (`169.254.0.0/16`, `fe80::/10`), multicast, unspecified, reserved, IPv4-mapped IPv6, and cloud metadata destinations (e.g., `169.254.169.254`).
- **DNS Rebinding Prevention & IP Pinning:** Resolves hostnames once upfront and validates all resolved IP addresses. Socket connections are strictly pinned to the validated IP address, preventing time-of-check to time-of-use (TOCTOU) DNS rebinding attacks while maintaining original SNI and TLS hostname verification.
- **Candidate IP Failover:** If a multi-homed domain has unreachable IP candidates, the engine fails over exclusively to other pre-validated public IPs within the same DNS resolution set.
- **Redirect Policy:** Redirects are manually tracked up to a strict limit (maximum 3), and every destination URL is re-parsed and re-validated against the safety policy. HTTPS-to-HTTP downgrades are strictly prohibited.
- **Project Target Validation:** Project creation and updates enforce `UrlSafetyPolicy` at the persistence layer, ensuring SSRF vectors cannot be stored in the database.

## Authentication & Multi-Tenancy

- **Password Hashing:** Passwords are never stored in plaintext. Passwords are salted and hashed using Spring Security's `BCryptPasswordEncoder` (work factor 10+).
- **Stateless JWT Tokens:** Authentication emits HMAC-SHA256 signed JSON Web Tokens containing the operator's UUID. Expiration is strictly enforced (24 hours). Tokens are validated on every request via `JwtAuthenticationFilter`.
- **Tenant Isolation:** All project and audit operations enforce compound key filtering (`userId + entityId`). Users cannot view, update, delete, or trigger audits on projects or audit records belonging to other tenants.
- **Bounded Pagination:** All listing endpoints enforce minimum (1) and maximum (100) page sizes to protect against memory exhaustion and database denial of service.

## Logging & Privacy

- Full target URLs, query parameters, request bodies, and credentials are never written to logs. Only safe ASCII hostnames or redacted identifiers are recorded.
- Audit history detail records (`detailsJson`) store diagnostic telemetry without credentials or bearer tokens.
