# Deployment

This guide covers the intended free-tier layout: Vercel for the Vite frontend, Render for the Dockerized Spring Boot API, and Neon for PostgreSQL.

## Runtime and build commands

- Backend: Java 21, Maven 3.9+, Spring Boot 4.1.2.
- Frontend: Node.js 20+, npm 11+.
- Backend tests: `mvn -f backend/pom.xml test`
- Frontend tests: `npm --prefix frontend test`
- Frontend build: `npm --prefix frontend run build`
- Backend image: build from `backend/` with `docker build -t launchcheck-api .`

## Vercel frontend

Set this public build variable in the Vercel project:

| Variable | Value |
| --- | --- |
| `VITE_API_BASE_URL` | The Render backend origin, such as `https://launchcheck-api.onrender.com` |

Do not put database URLs, passwords, JWT secrets, CI tokens, or other private values in `VITE_*` variables. Vite embeds them in browser JavaScript. Local development leaves this variable unset and uses the Vite `/api` proxy to `http://127.0.0.1:8081`.

Build command: `npm run build` with the project root set to `frontend` (or use `npm --prefix frontend run build` from the repository root). Publish directory: `frontend/dist` when building from the repository root, or `dist` when the Vercel root directory is `frontend`.

## Render backend

Create a Web Service with the service root directory set to `backend` and `Dockerfile` as the Dockerfile. Render supplies `PORT`; the application honors it, falling back to `SERVER_PORT` and then `8080`.

Set these private environment variables:

| Variable | Purpose |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | Set to `prod` to enable production validation and strict CORS configuration. |
| `PORT` | Supplied by Render; do not override unless the service configuration requires it. |
| `SPRING_DATASOURCE_URL` | Neon JDBC URL, including the Neon `sslmode=require` parameter. |
| `SPRING_DATASOURCE_USERNAME` | Neon database role. |
| `SPRING_DATASOURCE_PASSWORD` | Neon database password. |
| `JWT_SECRET` | Random production signing secret of at least 32 characters. |
| `JWT_EXPIRATION_MS` | JWT lifetime in milliseconds, for example `86400000`. |
| `LAUNCHCHECK_SECURITY_CORS_ALLOWED_ORIGINS` | Comma-separated exact Vercel origin(s), without a trailing path. |

The production validator rejects the development database password and placeholder JWT secret. Never log or commit these values.

Use `https://<render-host>/actuator/health` as the Render health check path. It returns a lightweight health response without database details or secrets. Flyway runs automatically at application startup; migrations are not disabled in production.

## Neon PostgreSQL

Create a PostgreSQL database and copy its connection details into the Render variables. Use a JDBC URL similar to:

`jdbc:postgresql://<neon-host>/<database>?sslmode=require`

The exact host, database, role, and password come from Neon and must remain private. This repository does not claim Neon connectivity has been verified locally. Confirm the first Render startup log shows successful datasource initialization and Flyway completion without exposing credentials.

## Docker

From the repository root:

```bash
docker build -f backend/Dockerfile -t launchcheck-api backend

docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL='jdbc:postgresql://<host>/<db>?sslmode=require' \
  -e SPRING_DATASOURCE_USERNAME='<user>' \
  -e SPRING_DATASOURCE_PASSWORD='<password>' \
  -e JWT_SECRET='<random-secret-at-least-32-characters>' \
  -e LAUNCHCHECK_SECURITY_CORS_ALLOWED_ORIGINS='https://<vercel-origin>' \
  launchcheck-api
```

The image uses a Maven build stage and a Java 21 JRE runtime stage. The runtime process is non-root. Do not bake secrets or local environment files into the image.

## Free-tier limitations and troubleshooting

- Render and Neon free tiers may sleep or suspend resources, causing cold starts and connection delays.
- A browser CORS error usually means the exact Vercel origin is missing from `LAUNCHCHECK_SECURITY_CORS_ALLOWED_ORIGINS`; do not use `*` with credentials.
- A frontend request to the Vercel origin means `VITE_API_BASE_URL` was missing or malformed in the production build; rebuild after setting it.
- A datasource startup failure should be checked against the Neon JDBC URL, credentials, and `sslmode=require`.
- A Flyway validation failure indicates the database schema does not match the repository migration history; do not disable Flyway or reset a shared database.
- LaunchCheck intentionally rejects unsafe target URLs as part of its SSRF protection.
