package com.example.spip_draft.enums;

public enum ProjectStatus {
    PENDING,
    APPROVED,
    REJECTED,
    DELETED;

    public boolean isApproved() {
        return this == APPROVED;
    }

    public boolean isPending() {
        return this == PENDING;
    }

    public boolean isRejected() {
        return this == REJECTED;
    }
}