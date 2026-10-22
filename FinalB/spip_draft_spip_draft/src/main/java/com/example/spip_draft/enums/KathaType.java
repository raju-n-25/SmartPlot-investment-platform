package com.example.spip_draft.enums;

public enum KathaType {

    A("A Khata"),
    B("B Khata"),
    E("E Khata");

    private final String displayName;

    KathaType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    // ✅ IMPORTANT METHOD
    public static KathaType fromValue(String value) {
        try {
            return KathaType.valueOf(value.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid KathaType: " + value);
        }
    }
}