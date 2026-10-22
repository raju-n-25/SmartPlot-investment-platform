package com.example.spip_draft.controller;

import com.example.spip_draft.dto.builder.BuilderKycUpdateRequest;
import com.example.spip_draft.dto.profile.ProfilePhotoUpdateRequest;
import com.example.spip_draft.dto.builder.BuilderProfileResponse;
import com.example.spip_draft.service.BuilderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/builder/profile")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BuilderProfileController {

    private final BuilderService builderService;

    @GetMapping
    public ResponseEntity<BuilderProfileResponse> getMyProfile() {
        return ResponseEntity.ok(builderService.getCurrentBuilderProfile());
    }

    @PutMapping("/kyc")
    public ResponseEntity<BuilderProfileResponse> updateMyKyc(
            @Valid @RequestBody BuilderKycUpdateRequest request
    ) {
        return ResponseEntity.ok(builderService.updateCurrentBuilderKyc(request));
    }

    @GetMapping("/kyc/status")
    public ResponseEntity<String> getMyKycStatus() {
        return ResponseEntity.ok(
                builderService.getCurrentBuilderProfile().getKycStatus()
        );
    }

    @PutMapping("/photo")
    public ResponseEntity<BuilderProfileResponse> updateProfilePhoto(
            @RequestBody ProfilePhotoUpdateRequest request
    ) {
        return ResponseEntity.ok(
                builderService.updateProfilePhoto(request.getProfilePhotoUrl())
        );
    }

    @PostMapping("/photo/upload")
    public ResponseEntity<BuilderProfileResponse> uploadProfilePhoto(
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(
                builderService.uploadProfilePhoto(file)
        );
    }
}