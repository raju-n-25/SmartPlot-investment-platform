package com.example.spip_draft.dto.auth;

import lombok.Data;

@Data
public class RegisterBuilderRequest {

    private String firstName;
    private String lastName;
    private String email;
    private String password;

    private String companyName;
    private String companyRegistration;
    private String panNumber;
    private String gstNumber;
}