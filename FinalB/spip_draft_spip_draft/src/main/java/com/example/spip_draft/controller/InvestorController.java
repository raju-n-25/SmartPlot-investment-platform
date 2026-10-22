package com.example.spip_draft.controller;

import com.example.spip_draft.dto.investor.InvestorProfileResponse;
import com.example.spip_draft.dto.profile.ProfilePhotoUpdateRequest;
import com.example.spip_draft.entity.Investor;
import com.example.spip_draft.service.InvestorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/investors")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InvestorController {

    private final InvestorService investorService;

    /**
     * Get logged-in investor profile
     */
    @GetMapping("/me")
    public ResponseEntity<InvestorProfileResponse> getMyProfile() {
        return ResponseEntity.ok(investorService.getCurrentInvestorProfile());
    }

    /**
     * Update investor profile photo
     */
    @PutMapping("/photo")
    public ResponseEntity<InvestorProfileResponse> updateProfilePhoto(
            @RequestBody ProfilePhotoUpdateRequest request
    ) {
        return ResponseEntity.ok(
                investorService.updateProfilePhoto(request.getProfilePhotoUrl())
        );
    }

    /**
     * Get investor by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Investor> getInvestorById(@PathVariable Long id) {
        return ResponseEntity.ok(investorService.getInvestorById(id));
    }
}