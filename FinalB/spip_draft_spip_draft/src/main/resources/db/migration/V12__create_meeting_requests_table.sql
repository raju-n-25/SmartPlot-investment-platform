-- ============================================================
-- V12__create_meeting_requests_table.sql
-- Creates the initial meeting_requests table.
-- Later migrations:
--   V24 -> Meeting hold flow fields
--   V27 -> Meeting reschedule fields
-- ============================================================

CREATE TABLE IF NOT EXISTS meeting_requests (

                                                id BIGINT PRIMARY KEY AUTO_INCREMENT,

                                                investor_id BIGINT NOT NULL,
                                                builder_id BIGINT NOT NULL,
                                                project_id BIGINT NULL,
                                                plot_id BIGINT NULL,

                                                tenant_id BIGINT NOT NULL,

                                                budget DECIMAL(15,2) NULL,
    expected_timeline VARCHAR(255) NULL,
    purpose VARCHAR(1000) NULL,

    status VARCHAR(50) NOT NULL DEFAULT 'REQUESTED',

    scheduled_at DATETIME NULL,
    meeting_link VARCHAR(1000) NULL,
    rejection_reason VARCHAR(1000) NULL,

    reminder_sent BIT(1) NOT NULL DEFAULT b'0',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_meeting_investor
    FOREIGN KEY (investor_id)
    REFERENCES investors(id)
    ON DELETE CASCADE,

    CONSTRAINT fk_meeting_builder
    FOREIGN KEY (builder_id)
    REFERENCES builders(id)
    ON DELETE CASCADE,

    CONSTRAINT fk_meeting_project
    FOREIGN KEY (project_id)
    REFERENCES projects(id)
    ON DELETE SET NULL,

    CONSTRAINT fk_meeting_plot
    FOREIGN KEY (plot_id)
    REFERENCES plots(id)
    ON DELETE SET NULL
    );

CREATE INDEX idx_meeting_builder
    ON meeting_requests(builder_id);

CREATE INDEX idx_meeting_investor
    ON meeting_requests(investor_id);

CREATE INDEX idx_meeting_project
    ON meeting_requests(project_id);

CREATE INDEX idx_meeting_plot
    ON meeting_requests(plot_id);

CREATE INDEX idx_meeting_status
    ON meeting_requests(status);

CREATE INDEX idx_meeting_tenant
    ON meeting_requests(tenant_id);