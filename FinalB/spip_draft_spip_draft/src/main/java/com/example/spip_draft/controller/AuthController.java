package com.example.spip_draft.controller;

import com.example.spip_draft.dto.auth.AuthResponse;
import com.example.spip_draft.dto.auth.LoginRequest;
import com.example.spip_draft.dto.auth.RegisterBuilderRequest;
import com.example.spip_draft.dto.auth.RegisterInvestorRequest;
import com.example.spip_draft.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthenticationService authenticationService;

    /**
     * Login user
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticationService.login(request));
    }

    /**
     * Register builder
     */
    @PostMapping("/register/builder")
    public ResponseEntity<AuthResponse> registerBuilder(
            @RequestBody RegisterBuilderRequest request
    ) {
        return ResponseEntity.ok(authenticationService.registerBuilder(request));
    }

    /**
     * Register investor
     */
    @PostMapping("/register/investor")
    public ResponseEntity<AuthResponse> registerInvestor(
            @RequestBody RegisterInvestorRequest request
    ) {
        return ResponseEntity.ok(authenticationService.registerInvestor(request));
    }
}