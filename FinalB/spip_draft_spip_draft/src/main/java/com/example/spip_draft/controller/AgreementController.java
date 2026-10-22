package com.example.spip_draft.controller;

import com.example.spip_draft.dto.agreement.AgreementResponse;
import com.example.spip_draft.enums.RefundMode;
import com.example.spip_draft.service.AgreementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agreements")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AgreementController {

    private final AgreementService agreementService;

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<AgreementResponse> getByBooking(@PathVariable Long bookingId) {
        return ResponseEntity.ok(agreementService.getByBooking(bookingId));
    }

    @PostMapping("/booking/{bookingId}/generate")
    public ResponseEntity<AgreementResponse> generate(@PathVariable Long bookingId) {
        return ResponseEntity.ok(agreementService.generateForBooking(bookingId));
    }
    @PostMapping("/{agreementId}/regenerate")
    public ResponseEntity<AgreementResponse> regenerate(@PathVariable Long agreementId) {
        return ResponseEntity.ok(agreementService.regenerateExpiredAgreement(agreementId));
    }

    @PostMapping("/{agreementId}/send")
    public ResponseEntity<AgreementResponse> send(@PathVariable Long agreementId) {
        return ResponseEntity.ok(agreementService.sendAgreement(agreementId));
    }

    @PostMapping("/{agreementId}/viewed")
    public ResponseEntity<AgreementResponse> markViewed(@PathVariable Long agreementId) {
        return ResponseEntity.ok(agreementService.markViewed(agreementId));
    }

    @PostMapping("/{agreementId}/investor-accept")
    public ResponseEntity<AgreementResponse> investorAccept(@PathVariable Long agreementId) {
        return ResponseEntity.ok(agreementService.investorAccept(agreementId));
    }

    @PostMapping("/{agreementId}/builder-accept")
    public ResponseEntity<AgreementResponse> builderAccept(@PathVariable Long agreementId) {
        return ResponseEntity.ok(agreementService.builderAccept(agreementId));
    }

    @PostMapping("/{agreementId}/investor-sign")
    public ResponseEntity<AgreementResponse> investorSign(@PathVariable Long agreementId) {
        return ResponseEntity.ok(agreementService.investorSign(agreementId));
    }

    @PostMapping("/{agreementId}/builder-sign")
    public ResponseEntity<AgreementResponse> builderSign(@PathVariable Long agreementId) {
        return ResponseEntity.ok(agreementService.builderSign(agreementId));
    }

    @PostMapping("/{agreementId}/investor-reject")
    public ResponseEntity<AgreementResponse> investorReject(
            @PathVariable Long agreementId,
            @RequestParam(defaultValue = "WALLET") RefundMode mode,
            @RequestParam(required = false) String reason
    ) {
        return ResponseEntity.ok(agreementService.investorRejectAndRefund(agreementId, mode, reason));
    }
}
