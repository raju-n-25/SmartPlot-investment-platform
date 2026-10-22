CREATE TABLE complaints (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,

    complaint_type VARCHAR(60) NOT NULL,
    raised_by_role VARCHAR(30) NOT NULL,
    status VARCHAR(40) NOT NULL,

    reason VARCHAR(255) NOT NULL,
    description TEXT NULL,
    admin_note TEXT NULL,
    resolution_note TEXT NULL,
    rejection_reason TEXT NULL,

    escrow_related BIT NOT NULL DEFAULT 0,
    payment_related BIT NOT NULL DEFAULT 0,
    agreement_related BIT NOT NULL DEFAULT 0,

    created_at DATETIME NOT NULL,
    reviewed_at DATETIME NULL,
    resolved_at DATETIME NULL,
    rejected_at DATETIME NULL,

    raised_by_user_id BIGINT NOT NULL,
    assigned_admin_id BIGINT NULL,
    resolved_by_admin_id BIGINT NULL,

    booking_id BIGINT NULL,
    payment_id BIGINT NULL,
    agreement_id BIGINT NULL,
    project_id BIGINT NULL,
    plot_id BIGINT NULL,

    PRIMARY KEY (id),

    INDEX idx_complaints_tenant_created (tenant_id, created_at),
    INDEX idx_complaints_status (status),
    INDEX idx_complaints_raised_by_user (raised_by_user_id),
    INDEX idx_complaints_booking (booking_id),
    INDEX idx_complaints_payment (payment_id),

    CONSTRAINT fk_complaints_raised_by_user
        FOREIGN KEY (raised_by_user_id) REFERENCES users(id),

    CONSTRAINT fk_complaints_assigned_admin
        FOREIGN KEY (assigned_admin_id) REFERENCES users(id),

    CONSTRAINT fk_complaints_resolved_by_admin
        FOREIGN KEY (resolved_by_admin_id) REFERENCES users(id),

    CONSTRAINT fk_complaints_booking
        FOREIGN KEY (booking_id) REFERENCES bookings(id),

    CONSTRAINT fk_complaints_payment
        FOREIGN KEY (payment_id) REFERENCES payments(id),

    CONSTRAINT fk_complaints_agreement
        FOREIGN KEY (agreement_id) REFERENCES booking_agreements(id),

    CONSTRAINT fk_complaints_project
        FOREIGN KEY (project_id) REFERENCES projects(id),

    CONSTRAINT fk_complaints_plot
        FOREIGN KEY (plot_id) REFERENCES plots(id)
);