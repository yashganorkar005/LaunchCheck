ALTER TABLE audits ALTER COLUMN project_id DROP NOT NULL;
ALTER TABLE audits ADD COLUMN audit_type VARCHAR(32) NOT NULL DEFAULT 'PREFLIGHT';
ALTER TABLE audits ADD COLUMN target_label VARCHAR(1024);

CREATE INDEX idx_audits_user_type_started_at ON audits(user_id, audit_type, started_at DESC);
