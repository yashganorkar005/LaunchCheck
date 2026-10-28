-- V3__ci_service_tokens.sql
CREATE TABLE ci_service_tokens (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    owner_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_name VARCHAR(255) NOT NULL,
    token_hash VARCHAR(256) NOT NULL,
    token_prefix VARCHAR(16) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ci_tokens_project_id ON ci_service_tokens(project_id);
CREATE INDEX idx_ci_tokens_token_prefix ON ci_service_tokens(token_prefix);
