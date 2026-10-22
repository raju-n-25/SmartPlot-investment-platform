CREATE TABLE emi_payment_transactions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) DEFAULT NULL,

    emi_plan_id BIGINT NOT NULL,
    emi_installment_id BIGINT NOT NULL,
    investor_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,

    amount DECIMAL(15,2) NOT NULL,
    wallet_amount DECIMAL(15,2) NOT NULL,
    gateway_amount DECIMAL(15,2) NOT NULL,

    payment_mode VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    gateway_type VARCHAR(50) DEFAULT NULL,
    gateway_reference VARCHAR(150) DEFAULT NULL,
    paid_at DATETIME(6) DEFAULT NULL,
    remarks VARCHAR(500) DEFAULT NULL,

    PRIMARY KEY (id),
    KEY idx_emi_tx_installment (emi_installment_id),
    KEY idx_emi_tx_plan (emi_plan_id),
    KEY idx_emi_tx_investor (investor_id),
    KEY idx_emi_tx_status (status),

    CONSTRAINT fk_emi_tx_plan FOREIGN KEY (emi_plan_id) REFERENCES emi_plans(id),
    CONSTRAINT fk_emi_tx_installment FOREIGN KEY (emi_installment_id) REFERENCES emi_installments(id),
    CONSTRAINT fk_emi_tx_investor FOREIGN KEY (investor_id) REFERENCES investors(id)
);