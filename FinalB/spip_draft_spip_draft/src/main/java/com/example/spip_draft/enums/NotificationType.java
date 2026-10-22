package com.example.spip_draft.enums;

public final class NotificationType {

    private NotificationType() {}

    public static final String TOKEN_PAID = "TOKEN_PAID";
    public static final String FINAL_PAYMENT_COMPLETED = "FINAL_PAYMENT_COMPLETED";
    public static final String AGREEMENT_SENT = "AGREEMENT_SENT";
    public static final String AGREEMENT_SIGNED = "AGREEMENT_SIGNED";
    public static final String BUILDER_APPROVED = "BUILDER_APPROVED";
    public static final String BUILDER_REJECTED = "BUILDER_REJECTED";

    public static final String MEETING_REQUESTED = "MEETING_REQUESTED";
    public static final String MEETING_APPROVED = "MEETING_APPROVED";
    public static final String MEETING_REJECTED = "MEETING_REJECTED";
    public static final String MEETING_COMPLETED = "MEETING_COMPLETED";
    public static final String FOLLOW_UP_MEETING_REQUESTED = "FOLLOW_UP_MEETING_REQUESTED";
    public static final String PLOT_RELEASED_TO_MARKETPLACE = "PLOT_RELEASED_TO_MARKETPLACE";
    public static final String MEETING_BUY_REQUESTED = "MEETING_BUY_REQUESTED";

    public static final String AGREEMENT_GENERATED = "AGREEMENT_GENERATED";
    public static final String INVESTOR_ACCEPTED_AGREEMENT = "INVESTOR_ACCEPTED_AGREEMENT";
    public static final String BUILDER_ACCEPTED_AGREEMENT = "BUILDER_ACCEPTED_AGREEMENT";
    public static final String INVESTOR_SIGNED_AGREEMENT = "INVESTOR_SIGNED_AGREEMENT";
    public static final String BUILDER_SIGNED_AGREEMENT = "BUILDER_SIGNED_AGREEMENT";
    public static final String AGREEMENT_COMPLETED = "AGREEMENT_COMPLETED";
    public static final String AGREEMENT_REJECTED = "AGREEMENT_REJECTED";
}