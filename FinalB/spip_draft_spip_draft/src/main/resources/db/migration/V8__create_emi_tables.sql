CREATE TABLE emi_plans (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) DEFAULT NULL,

    booking_id BIGINT NOT NULL,
    investor_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,

    total_amount DECIMAL(15,2) NOT NULL,
    token_paid DECIMAL(15,2) NOT NULL,
    down_payment DECIMAL(15,2) NOT NULL,
    principal_amount DECIMAL(15,2) NOT NULL,
    annual_interest_rate DECIMAL(5,2) NOT NULL,
    tenure_months INT NOT NULL,
    monthly_emi DECIMAL(15,2) NOT NULL,
    total_interest DECIMAL(15,2) NOT NULL,
    total_payable DECIMAL(15,2) NOT NULL,

    paid_installments INT NOT NULL,
    total_installments INT NOT NULL,
    remaining_principal DECIMAL(15,2) NOT NULL,

    grace_days INT NOT NULL,
    late_fee_per_day DECIMAL(10,2) NOT NULL,
    max_late_fee DECIMAL(10,2) DEFAULT NULL,

    start_date DATE NOT NULL,
    first_due_date DATE NOT NULL,
    next_due_date DATE DEFAULT NULL,

    status VARCHAR(30) NOT NULL,
    remarks VARCHAR(500) DEFAULT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_emi_plan_booking (booking_id),
    KEY idx_emi_plan_booking (booking_id),
    KEY idx_emi_plan_investor (investor_id),
    KEY idx_emi_plan_tenant (tenant_id),
    KEY idx_emi_plan_status (status),

    CONSTRAINT fk_emi_plan_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
    CONSTRAINT fk_emi_plan_investor FOREIGN KEY (investor_id) REFERENCES investors(id)
);

CREATE TABLE emi_installments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) DEFAULT NULL,

    emi_plan_id BIGINT NOT NULL,
    installment_number INT NOT NULL,
    due_date DATE NOT NULL,

    opening_principal DECIMAL(15,2) NOT NULL,
    principal_component DECIMAL(15,2) NOT NULL,
    interest_component DECIMAL(15,2) NOT NULL,
    emi_amount DECIMAL(15,2) NOT NULL,

    penalty_amount DECIMAL(15,2) NOT NULL,
    paid_amount DECIMAL(15,2) NOT NULL,
    remaining_due DECIMAL(15,2) NOT NULL,

    paid_date DATE DEFAULT NULL,
    last_payment_at DATETIME(6) DEFAULT NULL,

    status VARCHAR(30) NOT NULL,
    remarks VARCHAR(500) DEFAULT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_plan_installment_no (emi_plan_id, installment_number),
    KEY idx_emi_inst_plan (emi_plan_id),
    KEY idx_emi_inst_due_date (due_date),
    KEY idx_emi_inst_status (status),

    CONSTRAINT fk_emi_installment_plan FOREIGN KEY (emi_plan_id) REFERENCES emi_plans(id)
);