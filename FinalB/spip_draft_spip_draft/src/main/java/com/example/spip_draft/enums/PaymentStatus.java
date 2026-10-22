package com.example.spip_draft.enums;

public enum PaymentStatus {

    PENDING,
    TOKEN_PAID,
    COMPLETED,
    FAILED,
    REFUNDED,
    REFUND_PENDING;

    public boolean isSuccess() {
        return this == TOKEN_PAID || this == COMPLETED;
    }

    public boolean isFinal() {
        return this == COMPLETED || this == FAILED || this == REFUNDED;
    }
}