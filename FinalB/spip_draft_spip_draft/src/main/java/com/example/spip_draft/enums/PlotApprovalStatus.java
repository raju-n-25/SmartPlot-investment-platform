package com.example.spip_draft.enums;

public enum PlotApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED,
	DELETED;

    public boolean isApproved() {
        return this == APPROVED;
    }
}