package com.example.spip_draft.dto.auth;

import lombok.Data;

@Data
public class RegisterInvestorRequest {

    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String panCard;
}