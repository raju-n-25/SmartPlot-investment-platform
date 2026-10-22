package com.example.spip_draft.enums;

public enum BookingStatus {

    TOKEN_BLOCKED,
    TOKEN_PAID,

    AWAITING_BUILDER_APPROVAL,
    PAYMENT_OPTION_PENDING,
    BUILDER_APPROVED,
    BUILDER_REJECTED,
    REAPPROVAL_REQUESTED,

    EMI_ACTIVE,

    // ✅ NEW: pre-booking marketplace meeting hold
    MEETING_HOLD,

    CONFIRMED,
    CANCELLED,
    FAILED,
    EXPIRED,
    DEFAULTED,
    FORECLOSED,
    REFUNDED;

    public boolean isActive() {
        return this == TOKEN_BLOCKED
                || this == TOKEN_PAID
                || this == AWAITING_BUILDER_APPROVAL
                || this == PAYMENT_OPTION_PENDING
                || this == BUILDER_APPROVED
                || this == REAPPROVAL_REQUESTED
                || this == EMI_ACTIVE
                || this == MEETING_HOLD;
    }

    public boolean isFinalState() {
        return this == CANCELLED
                || this == FAILED
                || this == EXPIRED
                || this == CONFIRMED
                || this == DEFAULTED
                || this == FORECLOSED
                || this == REFUNDED;
    }
}