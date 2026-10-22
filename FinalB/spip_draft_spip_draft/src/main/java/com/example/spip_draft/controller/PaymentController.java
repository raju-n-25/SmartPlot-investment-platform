package com.example.spip_draft.controller;

import com.example.spip_draft.dto.payment.PaymentResponse;
import com.example.spip_draft.dto.payment.RefundRequest;
import com.example.spip_draft.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/token/{bookingId}")
    public ResponseEntity<PaymentResponse> payToken(
            @PathVariable Long bookingId,
            @RequestParam BigDecimal amount,
            @RequestParam String gatewayType,
            @RequestParam(defaultValue = "true") boolean useWallet
    ) {
        return ResponseEntity.ok(
                paymentService.payToken(bookingId, amount, gatewayType, useWallet)
        );
    }

    @PostMapping("/complete/{bookingId}")
    public ResponseEntity<PaymentResponse> completePayment(
            @PathVariable Long bookingId,
            @RequestParam String gatewayType,
            @RequestParam(defaultValue = "true") boolean useWallet
    ) {
        return ResponseEntity.ok(
                paymentService.completePayment(bookingId, gatewayType, useWallet)
        );
    }

    @PostMapping("/refund/{bookingId}")
    public ResponseEntity<String> refund(
            @PathVariable Long bookingId,
            @RequestBody RefundRequest request
    ) {
        paymentService.refund(bookingId, request.getMode());
        return ResponseEntity.ok("Refund processed");
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<PaymentResponse> getPaymentByBooking(
            @PathVariable Long bookingId
    ) {
        return ResponseEntity.ok(
                paymentService.getByBooking(bookingId)
        );
    }

    @PostMapping("/escrow/release/{bookingId}")
    public ResponseEntity<PaymentResponse> releaseEscrow(
            @PathVariable Long bookingId
    ) {
        return ResponseEntity.ok(
                paymentService.releaseEscrow(bookingId)
        );
    }

    @PostMapping("/dispute/{bookingId}")
    public ResponseEntity<PaymentResponse> openDispute(
            @PathVariable Long bookingId,
            @RequestParam String reason
    ) {
        return ResponseEntity.ok(
                paymentService.openDispute(bookingId, reason)
        );
    }

    @PostMapping("/freeze/{bookingId}")
    public ResponseEntity<PaymentResponse> freezeEscrow(
            @PathVariable Long bookingId,
            @RequestParam String reason
    ) {
        return ResponseEntity.ok(
                paymentService.freezeEscrow(bookingId, reason)
        );
    }
    
    @PostMapping("/unfreeze-request/{bookingId}")
    public ResponseEntity<PaymentResponse> requestUnfreezeReview(
            @PathVariable Long bookingId,
            @RequestParam(defaultValue = "Investor completed required action and requested escrow review.") String note
    ) {
        return ResponseEntity.ok(
                paymentService.requestUnfreezeReview(bookingId, note)
        );
    }
}