-- V25__upgrade_audit_logs.sql

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NULL,
    entity_id BIGINT NULL,
    actor_user_id BIGINT NULL,
    actor_role VARCHAR(50) NULL,
    tenant_id BIGINT NULL,
    description VARCHAR(1000) NULL,
    old_value VARCHAR(1000) NULL,
    new_value VARCHAR(1000) NULL,
    PRIMARY KEY (id)
);