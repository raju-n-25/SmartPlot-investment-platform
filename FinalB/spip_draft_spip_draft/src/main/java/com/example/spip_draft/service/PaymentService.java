package com.example.spip_draft.service;

import com.example.spip_draft.dto.payment.PaymentResponse;
import com.example.spip_draft.enums.RefundMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentOrchestrator orchestrator;

    public PaymentResponse payToken(Long bookingId, BigDecimal amount, String gatewayType, boolean useWallet) {
        return orchestrator.handleTokenPayment(bookingId, amount, gatewayType, useWallet);
    }

    public PaymentResponse completePayment(Long bookingId, String gatewayType, boolean useWallet) {
        return orchestrator.handleFinalPayment(bookingId, gatewayType, useWallet);
    }

    public void refund(Long bookingId, RefundMode mode) {
        orchestrator.handleRefund(bookingId, mode);
    }

    public PaymentResponse getByBooking(Long bookingId) {
        return orchestrator.getPaymentDetails(bookingId);
    }

    public PaymentResponse releaseEscrow(Long bookingId) {
        return orchestrator.releaseEscrow(bookingId);
    }

    public PaymentResponse freezeEscrow(Long bookingId, String reason) {
        return orchestrator.freezeEscrow(
                bookingId,
                reason,
                "Resolve pending escrow requirement",
                7
        );
    }

    public PaymentResponse freezeEscrow(Long bookingId, String reason, String actionRequired, int deadlineDays) {
        return orchestrator.freezeEscrow(bookingId, reason, actionRequired, deadlineDays);
    }

    public PaymentResponse unfreezeEscrow(Long bookingId, String note) {
        return orchestrator.unfreezeEscrow(bookingId, note);
    }

    public PaymentResponse openDispute(Long bookingId, String reason) {
        return orchestrator.openDispute(bookingId, reason);
    }
    
    public PaymentResponse requestUnfreezeReview(Long bookingId, String note) {
        return orchestrator.requestUnfreezeReview(bookingId, note);
    }
}