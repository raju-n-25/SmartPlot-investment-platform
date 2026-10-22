package com.example.spip_draft.controller;

import com.example.spip_draft.dto.payment.PaymentResponse;
import com.example.spip_draft.enums.EscrowStatus;
import com.example.spip_draft.mapper.PaymentMapper;
import com.example.spip_draft.repository.PaymentRepository;
import com.example.spip_draft.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/escrow")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminEscrowController {

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    @GetMapping("/held")
    public ResponseEntity<List<PaymentResponse>> getHeldEscrow() {
        return ResponseEntity.ok(
                paymentRepository.findByEscrowStatus(EscrowStatus.HELD)
                        .stream()
                        .map(PaymentMapper::toResponse)
                        .toList()
        );
    }

    @GetMapping("/review")
    public ResponseEntity<List<PaymentResponse>> getReviewEscrow() {
        return ResponseEntity.ok(
                paymentRepository.findByEscrowStatus(EscrowStatus.UNDER_REVIEW)
                        .stream()
                        .map(PaymentMapper::toResponse)
                        .toList()
        );
    }

    @GetMapping("/release-ready")
    public ResponseEntity<List<PaymentResponse>> getReleaseReadyEscrow() {
        return ResponseEntity.ok(
                paymentRepository.findByEscrowStatus(EscrowStatus.READY_FOR_RELEASE)
                        .stream()
                        .map(PaymentMapper::toResponse)
                        .toList()
        );
    }

    @PostMapping("/{bookingId}/release")
    public ResponseEntity<PaymentResponse> releaseEscrow(@PathVariable Long bookingId) {
        return ResponseEntity.ok(paymentService.releaseEscrow(bookingId));
    }

    @PostMapping("/{bookingId}/freeze")
    public ResponseEntity<PaymentResponse> freezeEscrow(
            @PathVariable Long bookingId,
            @RequestParam String reason,
            @RequestParam(defaultValue = "Complete pending escrow requirement") String actionRequired,
            @RequestParam(defaultValue = "7") int deadlineDays
    ) {
        return ResponseEntity.ok(
                paymentService.freezeEscrow(bookingId, reason, actionRequired, deadlineDays)
        );
    }

    @PostMapping("/{bookingId}/dispute")
    public ResponseEntity<PaymentResponse> openDispute(@PathVariable Long bookingId,
                                                       @RequestParam String reason) {
        return ResponseEntity.ok(paymentService.openDispute(bookingId, reason));
    }

    @PostMapping("/{bookingId}/unfreeze")
    public ResponseEntity<PaymentResponse> unfreezeEscrow(
            @PathVariable Long bookingId,
            @RequestParam(defaultValue = "Issue resolved. Escrow moved back to review.") String note
    ) {
        return ResponseEntity.ok(
                paymentService.unfreezeEscrow(bookingId, note)
        );
    }
}