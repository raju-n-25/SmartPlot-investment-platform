package com.example.spip_draft.mapper;

import com.example.spip_draft.dto.booking.BookingResponse;
import com.example.spip_draft.entity.Booking;
import com.example.spip_draft.entity.BookingAgreement;
import com.example.spip_draft.entity.Payment;
import com.example.spip_draft.enums.PaymentStatus;

import java.math.BigDecimal;

public class BookingMapper {

    public static BookingResponse toResponse(Booking booking) {
        return toResponse(booking, null, null);
    }

    public static BookingResponse toResponse(Booking booking,
                                             Payment payment,
                                             BookingAgreement agreement) {
        BigDecimal tokenPaid = booking.getAmount() != null ? booking.getAmount() : BigDecimal.ZERO;
        BigDecimal totalPrice = booking.getPlot() != null && booking.getPlot().getPrice() != null
                ? booking.getPlot().getPrice()
                : BigDecimal.ZERO;
        BigDecimal remainingAmount = totalPrice.subtract(tokenPaid);
        if (remainingAmount.compareTo(BigDecimal.ZERO) < 0) {
            remainingAmount = BigDecimal.ZERO;
        }

        boolean refundScheduled =
                payment != null &&
                payment.getPaymentStatus() != null &&
                payment.getPaymentStatus() == PaymentStatus.REFUND_PENDING;

        return BookingResponse.builder()
                .id(booking.getId())
                .tenantId(booking.getTenantId())

                .plotId(booking.getPlot() != null ? booking.getPlot().getId() : null)
                .plotNumber(booking.getPlot() != null ? booking.getPlot().getPlotNumber() : null)
                .projectName(
                        booking.getPlot() != null && booking.getPlot().getProject() != null
                                ? booking.getPlot().getProject().getProjectName()
                                : null
                )

                .investorId(booking.getInvestor() != null ? booking.getInvestor().getId() : null)
                .investorName(
                        booking.getInvestor() != null && booking.getInvestor().getUser() != null
                                ? booking.getInvestor().getUser().getFullName()
                                : null
                )

                .builderId(
                        booking.getPlot() != null &&
                        booking.getPlot().getProject() != null &&
                        booking.getPlot().getProject().getBuilder() != null
                                ? booking.getPlot().getProject().getBuilder().getId()
                                : null
                )
                .builderName(
                        booking.getPlot() != null &&
                        booking.getPlot().getProject() != null &&
                        booking.getPlot().getProject().getBuilder() != null
                                ? booking.getPlot().getProject().getBuilder().getCompanyName()
                                : null
                )

                .amount(booking.getAmount())
                .tokenPaid(tokenPaid)
                .totalPrice(totalPrice)
                .remainingAmount(remainingAmount)

                .status(booking.getStatus() != null ? booking.getStatus().name() : null)
                .bookingDate(booking.getBookingTime())
                .tokenExpiry(booking.getTokenExpiry())

                .builderResponseAt(booking.getBuilderResponseAt())
                .builderResponseNote(booking.getBuilderResponseNote())
                .builderApprovedBy(booking.getBuilderApprovedBy())

                .paymentStatus(
                        payment != null && payment.getPaymentStatus() != null
                                ? payment.getPaymentStatus().name()
                                : null
                )
                .escrowStatus(
                        payment != null && payment.getEscrowStatus() != null
                                ? payment.getEscrowStatus().name()
                                : null
                )
                .riskLevel(
                        payment != null && payment.getRiskLevel() != null
                                ? payment.getRiskLevel().name()
                                : null
                )
                
                
                
                .reviewReason(payment != null ? payment.getReviewReason() : null)
                .freezeActionRequired(payment != null ? payment.getFreezeActionRequired() : null)
                .frozenAt(payment != null ? payment.getFrozenAt() : null)
                .freezeDeadlineAt(payment != null ? payment.getFreezeDeadlineAt() : null)
                .freezeResolvedAt(payment != null ? payment.getFreezeResolvedAt() : null)
                .unfreezeRequested(payment != null && payment.isUnfreezeRequested())
                .unfreezeRequestedAt(payment != null ? payment.getUnfreezeRequestedAt() : null)
                .unfreezeRequestNote(payment != null ? payment.getUnfreezeRequestNote() : null)
                
                .agreementStatus(
                        agreement != null && agreement.getStatus() != null
                                ? agreement.getStatus().name()
                                : null
                )
                .agreementType(
                        agreement != null && agreement.getAgreementType() != null
                                ? agreement.getAgreementType().name()
                                : null
                )

                .escrowReleased(payment != null && payment.isEscrowReleased())
                .refunded(payment != null && payment.isRefunded())
                .disputeOpen(payment != null && payment.isDisputeOpen())
                .refundScheduled(refundScheduled)

                .escrowHeldAt(payment != null ? payment.getCreatedAt() : null)
                .holdUntil(payment != null ? payment.getHoldUntil() : null)
                .readyForReleaseAt(payment != null ? payment.getReadyForReleaseAt() : null)
                .releaseScheduledAt(payment != null ? payment.getReleaseScheduledAt() : null)
                .releasedAt(payment != null ? payment.getReleasedAt() : null)
                .releaseReference(payment != null ? payment.getReleaseReference() : null)
                .refundedAt(payment != null ? payment.getRefundedAt() : null)

                .agreementId(agreement != null ? agreement.getId() : null)
                .agreementNumber(agreement != null ? agreement.getAgreementNumber() : null)
                .agreementFileUrl(agreement != null ? agreement.getFileUrl() : null)
                .agreementReviewFileUrl(agreement != null ? agreement.getReviewFileUrl() : null)
                .agreementExecutedFileUrl(agreement != null ? agreement.getExecutedFileUrl() : null)

                .investorSigned(agreement != null && Boolean.TRUE.equals(agreement.getInvestorSigned()))
                .builderSigned(agreement != null && Boolean.TRUE.equals(agreement.getBuilderSigned()))
                .investorViewedDocument(agreement != null && Boolean.TRUE.equals(agreement.getInvestorViewedDocument()))
                .builderViewedDocument(agreement != null && Boolean.TRUE.equals(agreement.getBuilderViewedDocument()))
                .executedAgreementAvailable(
                        agreement != null &&
                        agreement.getExecutedFileUrl() != null &&
                        !agreement.getExecutedFileUrl().isBlank()
                )

                .agreementGeneratedAt(agreement != null ? agreement.getGeneratedAt() : null)
                .agreementSentAt(agreement != null ? agreement.getSentAt() : null)
                .investorAcceptedAt(agreement != null ? agreement.getInvestorAcceptedAt() : null)
                .builderAcceptedAt(agreement != null ? agreement.getBuilderAcceptedAt() : null)
                .investorSignedAt(agreement != null ? agreement.getInvestorSignedAt() : null)
                .builderSignedAt(agreement != null ? agreement.getBuilderSignedAt() : null)
                .agreementSignedAt(agreement != null ? agreement.getSignedAt() : null)
                .agreementExpiresAt(agreement != null ? agreement.getExpiresAt() : null)

                .build();
    }
}