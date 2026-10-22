package com.example.spip_draft.dto.auth;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {

    private String accessToken;
    private String tokenType;
    private Long expiresIn;

    private Long userId;
    private String email;
    private String role;

    private String firstName;
    private String lastName;
    private String fullName;

    private String message;
}