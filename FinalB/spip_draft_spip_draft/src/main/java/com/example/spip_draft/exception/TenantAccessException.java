package com.example.spip_draft.exception;

public class TenantAccessException extends RuntimeException {

    public TenantAccessException(String message) {
        super(message);
    }

    public TenantAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}