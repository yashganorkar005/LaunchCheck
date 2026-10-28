CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE projects (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    frontend_url VARCHAR(1024) NOT NULL,
    backend_url VARCHAR(1024) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_projects_user_id ON projects(user_id);

CREATE TABLE audits (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    duration_ms BIGINT NOT NULL,
    overall_status VARCHAR(32) NOT NULL,
    frontend_status VARCHAR(32),
    frontend_http_status INT,
    frontend_tls_status VARCHAR(32),
    frontend_diagnostic TEXT,
    backend_status VARCHAR(32),
    backend_http_status INT,
    backend_tls_status VARCHAR(32),
    backend_diagnostic TEXT,
    cors_status VARCHAR(32),
    cors_diagnostic TEXT,
    api_smoke_test_count INT NOT NULL DEFAULT 0,
    api_smoke_test_passed_count INT NOT NULL DEFAULT 0,
    details_json TEXT
);

CREATE INDEX idx_audits_project_id_started_at ON audits(project_id, started_at DESC);
CREATE INDEX idx_audits_user_id_started_at ON audits(user_id, started_at DESC);
