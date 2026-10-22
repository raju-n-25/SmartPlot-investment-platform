package com.example.spip_draft.service;

import com.example.spip_draft.dto.auth.*;
import com.example.spip_draft.entity.Builder;
import com.example.spip_draft.entity.Investor;
import com.example.spip_draft.entity.User;
import com.example.spip_draft.enums.KycStatus;
import com.example.spip_draft.repository.BuilderRepository;
import com.example.spip_draft.repository.InvestorRepository;
import com.example.spip_draft.repository.UserRepository;
import com.example.spip_draft.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final BuilderRepository builderRepository;
    private final InvestorRepository investorRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse login(LoginRequest request) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        com.example.spip_draft.security.CustomUserDetails userDetails =
                (com.example.spip_draft.security.CustomUserDetails) authentication.getPrincipal();

        String token = jwtTokenProvider.generateToken(userDetails);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(86400L)
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .message("Login successful")
                .build();
    }

    public AuthResponse registerBuilder(RegisterBuilderRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        Long tenantId = generateTenantId();

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("ROLE_BUILDER");
        user.setActive(true);
        user.setTenantId(tenantId);

        userRepository.save(user);

        Builder builder = new Builder();
        builder.setUser(user);
        builder.setCompanyName(request.getCompanyName());
        builder.setCompanyRegistration(request.getCompanyRegistration());
        builder.setPanNumber(request.getPanNumber());
        builder.setGstNumber(request.getGstNumber());
        builder.setKycStatus(KycStatus.PENDING);
        builder.setRating(0.0);
        builder.setTenantId(tenantId);

        builderRepository.save(builder);

        return AuthResponse.builder()
                .message("Builder registered successfully")
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .build();
    }

    public AuthResponse registerInvestor(RegisterInvestorRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        Long tenantId = generateTenantId();

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("ROLE_INVESTOR");
        user.setActive(true);
        user.setTenantId(tenantId);

        userRepository.save(user);

        Investor investor = new Investor();
        investor.setUser(user);
        investor.setPanCard(request.getPanCard());
        investor.setWalletBalance(BigDecimal.ZERO);
        investor.setRiskScore(0);
        investor.setApproved(false);
        investor.setTenantId(tenantId);

        investorRepository.save(investor);

        return AuthResponse.builder()
                .message("Investor registered successfully")
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .build();
    }

    private Long generateTenantId() {
        return System.currentTimeMillis();
    }
}