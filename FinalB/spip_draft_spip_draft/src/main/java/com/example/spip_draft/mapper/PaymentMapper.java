package com.example.spip_draft.mapper;

import com.example.spip_draft.dto.payment.PaymentResponse;
import com.example.spip_draft.entity.Booking;
import com.example.spip_draft.entity.BookingAgreement;
import com.example.spip_draft.entity.Payment;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

public class PaymentMapper {

    public static PaymentResponse toResponse(Payment payment) {
        return toResponse(payment, null, false, false, false, null);
    }

    public static PaymentResponse toResponse(Payment payment,
                                             BookingAgreement agreement,
                                             boolean canRefund,
                                             boolean canOpenDispute,
                                             boolean canReleaseEscrow,
                                             String releaseBlockedReason) {

        Booking booking = payment.getBooking();

        BigDecimal tokenPaid = BigDecimal.ZERO;
        BigDecimal totalPrice = payment.getAmount() != null ? payment.getAmount() : BigDecimal.ZERO;
        BigDecimal remainingAmount = BigDecimal.ZERO;

        if (booking != null) {
            if (booking.getAmount() != null) {
                tokenPaid = booking.getAmount();
            }

            if (booking.getPlot() != null && booking.getPlot().getPrice() != null) {
                totalPrice = booking.getPlot().getPrice();
            }

            remainingAmount = totalPrice.subtract(tokenPaid);
            if (remainingAmount.compareTo(BigDecimal.ZERO) < 0) {
                remainingAmount = BigDecimal.ZERO;
            }
        }

        PaymentResponse.PaymentResponseBuilder builder = PaymentResponse.builder()
                .id(payment.getId())
                .bookingId(booking != null ? booking.getId() : null)
                .agreementId(agreement != null ? agreement.getId() : null)
                .amount(payment.getAmount())
                .tokenPaid(tokenPaid)
                .remainingAmount(remainingAmount)
                .totalPrice(totalPrice)
                .status(payment.getPaymentStatus() != null ? payment.getPaymentStatus().name() : null)
                .bookingStatus(booking != null && booking.getStatus() != null ? booking.getStatus().name() : null)
                .transactionId(payment.getTransactionId())
                .escrowReleased(payment.isEscrowReleased())
                .refunded(payment.isRefunded())
                .escrowStatus(payment.getEscrowStatus() != null ? payment.getEscrowStatus().name() : null)
                .escrowHeldAt(payment.getEscrowHeldAt())
                .holdUntil(payment.getHoldUntil())
                .releasedAt(payment.getReleasedAt())
                .releaseReference(payment.getReleaseReference())
                .riskScore(payment.getRiskScore())
                .riskLevel(payment.getRiskLevel() != null ? payment.getRiskLevel().name() : null)
                .reviewReason(payment.getReviewReason())
                .frozenAt(payment.getFrozenAt())
                .freezeDeadlineAt(payment.getFreezeDeadlineAt())
                .freezeActionRequired(payment.getFreezeActionRequired())
                .freezeResolvedAt(payment.getFreezeResolvedAt())
                .unfreezeRequested(payment.isUnfreezeRequested())
                .unfreezeRequestedAt(payment.getUnfreezeRequestedAt())
                .unfreezeRequestNote(payment.getUnfreezeRequestNote())
                .agreementStatus(payment.getAgreementStatus() != null ? payment.getAgreementStatus().name() : null)
                .agreementNumber(agreement != null ? agreement.getAgreementNumber() : null)
                .agreementFileUrl(agreement != null ? agreement.getFileUrl() : null)
                .agreementCompleted(agreement != null && agreement.getStatus() != null && "SIGNED".equals(agreement.getStatus().name()))
                .investorSigned(agreement != null && Boolean.TRUE.equals(agreement.getInvestorSigned()))
                .builderSigned(agreement != null && Boolean.TRUE.equals(agreement.getBuilderSigned()))
                .disputeOpen(payment.isDisputeOpen())
                .payoutBlocked(payment.isPayoutBlocked())
                .canRefund(canRefund)
                .canOpenDispute(canOpenDispute)
                .canReleaseEscrow(canReleaseEscrow)
                .releaseBlockedReason(releaseBlockedReason)
                .expiryTime(payment.getExpiryTime());

        if (payment.getExpiryTime() != null) {
            long secondsRemaining = Duration.between(
                    LocalDateTime.now(),
                    payment.getExpiryTime()
            ).getSeconds();

            builder.secondsRemaining(Math.max(secondsRemaining, 0));
        }

        return builder.build();
    }
}