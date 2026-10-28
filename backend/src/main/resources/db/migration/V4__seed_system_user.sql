-- H2 (used in tests) does not support Postgres "ON CONFLICT" syntax.
-- Use a portable NOT EXISTS insert so Flyway migrations work across DBs.
INSERT INTO users (id, email, password_hash, created_at, updated_at)
SELECT
    '00000000-0000-0000-0000-000000000001',
    'system@launchcheck.test',
    'system-user-password-hash',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE id = '00000000-0000-0000-0000-000000000001'
);
