package com.example.spip_draft.service;

import com.example.spip_draft.dto.agreement.AgreementResponse;
import com.example.spip_draft.dto.payment.PaymentResponse;
import com.example.spip_draft.entity.Booking;
import com.example.spip_draft.entity.BookingAgreement;
import com.example.spip_draft.entity.Investor;
import com.example.spip_draft.entity.Payment;
import com.example.spip_draft.entity.Plot;
import com.example.spip_draft.enums.BookingStatus;
import com.example.spip_draft.enums.EscrowStatus;
import com.example.spip_draft.enums.PaymentAuditEventType;
import com.example.spip_draft.enums.PaymentStatus;
import com.example.spip_draft.enums.RefundMode;
import com.example.spip_draft.enums.RiskLevel;
import com.example.spip_draft.mapper.PaymentMapper;
import com.example.spip_draft.payment.gateway.PaymentGateway;
import com.example.spip_draft.payment.gateway.PaymentGatewayFactory;
import com.example.spip_draft.repository.BookingAgreementRepository;
import com.example.spip_draft.repository.BookingRepository;
import com.example.spip_draft.repository.PaymentRepository;
import com.example.spip_draft.repository.PlotRepository;
import com.example.spip_draft.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentOrchestrator {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final PlotRepository plotRepository;
    private final BookingAgreementRepository bookingAgreementRepository;
    private final PaymentGatewayFactory gatewayFactory;
    private final WalletAccountService walletAccountService;
    private final AgreementService agreementService;
    private final EscrowRiskService escrowRiskService;
    private final EscrowRuleEngineService escrowRuleEngineService;
    private final PaymentAuditService paymentAuditService;
    private final NotificationService notificationService;

    @Transactional
    public PaymentResponse handleTokenPayment(Long bookingId,
                                              BigDecimal amount,
                                              String gatewayType,
                                              boolean useWallet) {

        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        validateInvestorOwnership(booking);

        Plot plot = plotRepository.findPlotForUpdate(booking.getPlot().getId())
                .orElseThrow(() -> new IllegalArgumentException("Plot not found"));

        if (booking.getStatus() != BookingStatus.TOKEN_BLOCKED) {
            throw new IllegalStateException("Booking is not in TOKEN_BLOCKED state");
        }

        if (booking.getTokenExpiry() != null && booking.getTokenExpiry().isBefore(LocalDateTime.now())) {
            booking.setStatus(BookingStatus.EXPIRED);
            plot.setBookingStatus(null);
            plot.setBlockedAt(null);
            plot.setAvailable(true);

            bookingRepository.save(booking);
            plotRepository.save(plot);

            throw new IllegalStateException("Booking token has expired");
        }

        if (amount == null || amount.compareTo(booking.getAmount()) != 0) {
            throw new IllegalStateException("Invalid token amount");
        }

        paymentRepository.findByBookingIdForUpdate(bookingId)
                .ifPresent(existing -> {
                    if (existing.getPaymentStatus() == PaymentStatus.TOKEN_PAID
                            || existing.getPaymentStatus() == PaymentStatus.COMPLETED) {
                        throw new IllegalStateException("Token payment already processed");
                    }
                });

        Investor investor = booking.getInvestor();

        BigDecimal walletUsed = BigDecimal.ZERO;
        BigDecimal gatewayAmount = amount;

        if (useWallet) {
            walletUsed = walletAccountService.getWalletContribution(investor, amount);
            gatewayAmount = amount.subtract(walletUsed);

            if (walletUsed.compareTo(BigDecimal.ZERO) > 0) {
                walletAccountService.debitWallet(
                        investor,
                        walletUsed,
                        "Wallet used for token payment for booking " + booking.getId(),
                        booking.getId()
                );
            }
        }

        String txnId = null;
        if (gatewayAmount.compareTo(BigDecimal.ZERO) > 0) {
            PaymentGateway gateway = gatewayFactory.getGateway(gatewayType);
            txnId = gateway.processPayment(gatewayAmount);
        }

        Payment payment = paymentRepository.findByBookingIdForUpdate(bookingId).orElse(new Payment());
        payment.setBooking(booking);
        payment.setTenantId(booking.getTenantId());
        payment.setPaymentType(gatewayType);
        payment.setTransactionId(txnId);
        payment.markTokenPaid(amount);

        paymentRepository.save(payment);

        booking.setStatus(BookingStatus.AWAITING_BUILDER_APPROVAL);
        plot.setBookingStatus(BookingStatus.AWAITING_BUILDER_APPROVAL);

        bookingRepository.save(booking);
        plotRepository.save(plot);

        notificationService.notifyBuilderTokenPaid(booking);

        paymentAuditService.log(
                payment,
                PaymentAuditEventType.TOKEN_PAID,
                PaymentStatus.PENDING.name(),
                PaymentStatus.TOKEN_PAID.name(),
                "Token payment successful; waiting for builder approval"
        );

        log.info(
                "Token payment success | bookingId={} | plotId={} | walletUsed={} | gatewayUsed={} | nextStatus={}",
                bookingId, plot.getId(), walletUsed, gatewayAmount, booking.getStatus()
        );

        return PaymentMapper.toResponse(payment, null, canRefund(booking, payment), true, false, null);
    }

    @Transactional
    public PaymentResponse handleFinalPayment(Long bookingId,
                                              String gatewayType,
                                              boolean useWallet) {

        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        validateInvestorOwnership(booking);

        Plot plot = plotRepository.findPlotForUpdate(booking.getPlot().getId())
                .orElseThrow(() -> new IllegalArgumentException("Plot not found"));

        Payment payment = paymentRepository.findByBookingIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Token payment not found"));

        if (booking.getStatus() != BookingStatus.BUILDER_APPROVED) {
            throw new IllegalStateException("Final payment allowed only after builder approval");
        }

        if (payment.getPaymentStatus() != PaymentStatus.TOKEN_PAID) {
            throw new IllegalStateException("Token payment not in valid state");
        }

        if (payment.getExpiryTime() != null && payment.getExpiryTime().isBefore(LocalDateTime.now())) {
            payment.markFailed();
            booking.setStatus(BookingStatus.EXPIRED);
            plot.setBookingStatus(null);
            plot.setBlockedAt(null);
            plot.setAvailable(true);

            paymentRepository.save(payment);
            bookingRepository.save(booking);
            plotRepository.save(plot);

            throw new IllegalStateException("Final payment window expired");
        }

        BigDecimal totalPrice = plot.getPrice();
        BigDecimal tokenAmount = booking.getAmount();
        BigDecimal remainingAmount = totalPrice.subtract(tokenAmount);

        if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Invalid remaining amount");
        }

        Investor investor = booking.getInvestor();

        BigDecimal walletUsed = BigDecimal.ZERO;
        BigDecimal gatewayAmount = remainingAmount;

        if (useWallet) {
            walletUsed = walletAccountService.getWalletContribution(investor, remainingAmount);
            gatewayAmount = remainingAmount.subtract(walletUsed);

            if (walletUsed.compareTo(BigDecimal.ZERO) > 0) {
                walletAccountService.debitWallet(
                        investor,
                        walletUsed,
                        "Wallet used for final payment for booking " + booking.getId(),
                        booking.getId()
                );
            }
        }

        String txnId = payment.getTransactionId();

        if (gatewayAmount.compareTo(BigDecimal.ZERO) > 0) {
            PaymentGateway gateway = gatewayFactory.getGateway(gatewayType);
            txnId = gateway.processPayment(gatewayAmount);
        }

        payment.setPaymentType(gatewayType);
        payment.markCompleted(txnId, totalPrice);

        int riskScore = escrowRiskService.calculateRiskScore(payment);
        RiskLevel riskLevel = escrowRiskService.resolveRiskLevel(riskScore);
        payment.setRiskScore(riskScore);
        payment.setRiskLevel(riskLevel);

        paymentRepository.save(payment);

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTokenExpiry(null); // ✅ VERY IMPORTANT FIX
        bookingRepository.save(booking);

        plot.setBookingStatus(BookingStatus.CONFIRMED);
        plot.setAvailable(false);
        plot.setBlockedAt(null);
        plotRepository.save(plot);

        AgreementResponse agreement = agreementService.generateForBooking(bookingId);
        agreementService.sendAgreement(agreement.getId());

        notificationService.notifyBuilderFinalPaymentDone(booking);

        if (riskLevel == RiskLevel.HIGH) {
            payment.markUnderReview("High-risk transaction detected");
        }

        paymentRepository.save(payment);

        paymentAuditService.log(
                payment,
                PaymentAuditEventType.FINAL_PAYMENT_COMPLETED,
                PaymentStatus.TOKEN_PAID.name(),
                PaymentStatus.COMPLETED.name(),
                "Final payment successful after builder approval"
        );

        paymentAuditService.log(
                payment,
                PaymentAuditEventType.ESCROW_HELD,
                null,
                EscrowStatus.HELD.name(),
                "Funds held in escrow"
        );

        log.info(
                "Final payment success | bookingId={} | plotId={} | walletUsed={} | gatewayUsed={} | escrowStatus={}",
                bookingId, plot.getId(), walletUsed, gatewayAmount, payment.getEscrowStatus()
        );

        BookingAgreement bookingAgreement = bookingAgreementRepository.findByBookingId(bookingId).orElse(null);
        return PaymentMapper.toResponse(payment, bookingAgreement, canRefund(booking, payment), true, false, null);
    }

    @Transactional
    public void handleRefund(Long bookingId, RefundMode mode) {

        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        validateInvestorOwnership(booking);

        Plot plot = plotRepository.findPlotForUpdate(booking.getPlot().getId())
                .orElseThrow(() -> new IllegalArgumentException("Plot not found"));

        Payment payment = paymentRepository.findByBookingIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        if (payment.getPaymentStatus() == PaymentStatus.REFUNDED) {
            throw new IllegalStateException("Already refunded");
        }

        boolean tokenStageRefund =
                booking.getStatus() == BookingStatus.TOKEN_PAID
                        || booking.getStatus() == BookingStatus.AWAITING_BUILDER_APPROVAL
                        || booking.getStatus() == BookingStatus.BUILDER_REJECTED
                        || booking.getStatus() == BookingStatus.EXPIRED;

        boolean heldEscrowRefund =
                booking.getStatus() == BookingStatus.CONFIRMED
                        && payment.getEscrowStatus() != null
                        && (payment.getEscrowStatus() == EscrowStatus.HELD
                        || payment.getEscrowStatus() == EscrowStatus.UNDER_REVIEW
                        || payment.getEscrowStatus() == EscrowStatus.FROZEN);

        if (!tokenStageRefund && !heldEscrowRefund) {
            throw new IllegalStateException("Refund not allowed in current state");
        }

        if (payment.isEscrowReleased()) {
            throw new IllegalStateException("Refund not allowed after escrow release");
        }

        if (payment.getAmount() == null || payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Invalid refund amount");
        }

        payment.markRefundPending();
        paymentRepository.save(payment);

        paymentAuditService.log(
                payment,
                PaymentAuditEventType.REFUND_PENDING,
                null,
                EscrowStatus.REFUND_PENDING.name(),
                "Refund initiated"
        );

        if (mode == RefundMode.WALLET) {
            Investor investor = booking.getInvestor();
            walletAccountService.creditWallet(
                    investor,
                    payment.getAmount(),
                    "Refund credited to wallet for booking " + booking.getId(),
                    booking.getId()
            );

        } else if (mode == RefundMode.ORIGINAL) {
            log.info("Refund to original source | bookingId={} | amount={}", bookingId, payment.getAmount());
        } else {
            throw new IllegalStateException("Invalid refund mode");
        }

        payment.markRefunded();
        payment.setExpiryTime(null);
        paymentRepository.save(payment);

        booking.setStatus(BookingStatus.REFUNDED);
        bookingRepository.save(booking);

        plot.setBookingStatus(null);
        plot.setAvailable(true);
        plot.setBlockedAt(null);
        plotRepository.save(plot);

        bookingAgreementRepository.findByBookingId(bookingId).ifPresent(agreement -> {
            if (agreement.getStatus() != com.example.spip_draft.enums.AgreementStatus.REJECTED
                    && agreement.getStatus() != com.example.spip_draft.enums.AgreementStatus.SIGNED) {
                agreement.setStatus(com.example.spip_draft.enums.AgreementStatus.REJECTED);
                agreement.setRemarks("Refund processed for this agreement/booking flow");
                bookingAgreementRepository.save(agreement);
            }
        });

        paymentAuditService.log(
                payment,
                PaymentAuditEventType.REFUND_COMPLETED,
                null,
                PaymentStatus.REFUNDED.name(),
                "Refund completed via " + mode
        );
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentDetails(Long bookingId) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (SecurityUtil.isInvestor()) {
            validateInvestorOwnership(booking);
        }

        Payment payment = paymentRepository.findByBooking(booking)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        BookingAgreement agreement = bookingAgreementRepository.findByBookingId(bookingId).orElse(null);
        boolean canRelease = SecurityUtil.isAdmin()
                && escrowRuleEngineService.isEligibleForManualRelease(payment);

        String releaseBlockedReason = null;
        if (SecurityUtil.isAdmin() && !canRelease) {
            releaseBlockedReason = escrowRuleEngineService.firstManualFailureReason(payment);
        }
        return PaymentMapper.toResponse(payment, agreement, canRefund(booking, payment), true, canRelease, releaseBlockedReason);
    }

    @Transactional
    public PaymentResponse releaseEscrow(Long bookingId) {
        Payment payment = paymentRepository.findByBookingIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        if (!SecurityUtil.isAdmin()) {
            throw new IllegalStateException("Only admin can release escrow");
        }

        if (!escrowRuleEngineService.isEligibleForManualRelease(payment)) {
            throw new IllegalStateException("Escrow not eligible for manual release: "
                    + escrowRuleEngineService.firstManualFailureReason(payment));
        }

        EscrowStatus oldStatus = payment.getEscrowStatus();

        payment.markReleaseScheduled();
        paymentAuditService.log(
                payment,
                PaymentAuditEventType.ESCROW_RELEASE_SCHEDULED,
                oldStatus != null ? oldStatus.name() : null,
                EscrowStatus.RELEASE_SCHEDULED.name(),
                "Escrow release scheduled by admin"
        );

        payment.markEscrowReleased();
        paymentRepository.save(payment);

        paymentAuditService.log(
                payment,
                PaymentAuditEventType.ESCROW_RELEASED,
                EscrowStatus.RELEASE_SCHEDULED.name(),
                EscrowStatus.RELEASED.name(),
                "Escrow released manually by admin"
        );

        BookingAgreement agreement = bookingAgreementRepository.findByBookingId(bookingId).orElse(null);
        return PaymentMapper.toResponse(payment, agreement, false, false, false, null);
    }

    @Transactional
    public PaymentResponse releaseEscrowBySystem(Long bookingId) {
        Payment payment = paymentRepository.findByBookingIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        if (!escrowRuleEngineService.isEligibleForSystemRelease(payment)) {
            throw new IllegalStateException("Escrow not eligible for system release: "
                    + escrowRuleEngineService.firstSystemFailureReason(payment));
        }

        EscrowStatus oldStatus = payment.getEscrowStatus();

        payment.markReleaseScheduled();
        paymentAuditService.logSystem(
                payment,
                PaymentAuditEventType.ESCROW_RELEASE_SCHEDULED,
                oldStatus != null ? oldStatus.name() : null,
                EscrowStatus.RELEASE_SCHEDULED.name(),
                "Escrow release scheduled by system"
        );

        payment.markEscrowReleased();
        paymentRepository.save(payment);

        paymentAuditService.logSystem(
                payment,
                PaymentAuditEventType.ESCROW_RELEASED,
                EscrowStatus.RELEASE_SCHEDULED.name(),
                EscrowStatus.RELEASED.name(),
                "Escrow auto-released by system"
        );

        BookingAgreement agreement = bookingAgreementRepository.findByBookingId(bookingId).orElse(null);
        return PaymentMapper.toResponse(payment, agreement, false, false, false, null);
    }

    @Transactional
    public PaymentResponse freezeEscrow(Long bookingId, String reason, String actionRequired, int deadlineDays) {
        Payment payment = paymentRepository.findByBookingIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        if (!SecurityUtil.isAdmin()) {
            throw new IllegalStateException("Only admin can freeze escrow");
        }

        if (payment.isEscrowReleased()) {
            throw new IllegalStateException("Cannot freeze released escrow");
        }

        if (payment.isRefunded()) {
            throw new IllegalStateException("Cannot freeze refunded payment");
        }

        if (deadlineDays <= 0) {
            deadlineDays = 7;
        }

        EscrowStatus oldStatus = payment.getEscrowStatus();

        payment.markFrozen(reason, actionRequired, deadlineDays);
        paymentRepository.save(payment);

        paymentAuditService.log(
                payment,
                PaymentAuditEventType.ESCROW_FROZEN,
                oldStatus != null ? oldStatus.name() : null,
                EscrowStatus.FROZEN.name(),
                reason + " | Required action: " + actionRequired
        );

        notificationService.notifyEscrowFrozenToInvestor(payment, reason, actionRequired, payment.getFreezeDeadlineAt());
        notificationService.notifyEscrowFrozenToBuilder(payment, reason, actionRequired, payment.getFreezeDeadlineAt());

        BookingAgreement agreement = bookingAgreementRepository.findByBookingId(bookingId).orElse(null);
        return PaymentMapper.toResponse(payment, agreement, canRefund(payment.getBooking(), payment), false, false, null);
    }
    
    @Transactional
    public PaymentResponse unfreezeEscrow(Long bookingId, String note) {
        Payment payment = paymentRepository.findByBookingIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        if (!SecurityUtil.isAdmin()) {
            throw new IllegalStateException("Only admin can unfreeze escrow");
        }

        if (payment.getEscrowStatus() != EscrowStatus.FROZEN) {
            throw new IllegalStateException("Only frozen escrow can be unfrozen");
        }

        payment.unfreezeToReview(note != null ? note : "Escrow unfrozen by admin for re-review");
        paymentRepository.save(payment);

        paymentAuditService.log(
                payment,
                PaymentAuditEventType.ESCROW_UNFROZEN,
                EscrowStatus.FROZEN.name(),
                EscrowStatus.UNDER_REVIEW.name(),
                note != null ? note : "Escrow unfrozen by admin"
        );

        notificationService.notifyEscrowUnfrozenToInvestor(payment);
        notificationService.notifyEscrowUnfrozenToBuilder(payment);

        BookingAgreement agreement = bookingAgreementRepository.findByBookingId(bookingId).orElse(null);

        boolean canRelease = escrowRuleEngineService.isEligibleForManualRelease(payment);
        String blockedReason = canRelease ? null : escrowRuleEngineService.firstManualFailureReason(payment);

        return PaymentMapper.toResponse(payment, agreement, canRefund(payment.getBooking(), payment), true, canRelease, blockedReason);
    }
    
    @Transactional
    public void autoRefundFrozenEscrow(Long bookingId, String reason) {
        Payment payment = paymentRepository.findByBookingIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        Booking booking = payment.getBooking();
        Plot plot = booking.getPlot();

        if (payment.getEscrowStatus() != EscrowStatus.FROZEN) {
            return;
        }

        if (payment.isEscrowReleased() || payment.isRefunded()) {
            return;
        }

        payment.markRefundPending();
        paymentRepository.save(payment);

        paymentAuditService.logSystem(
                payment,
                PaymentAuditEventType.REFUND_PENDING,
                EscrowStatus.FROZEN.name(),
                EscrowStatus.REFUND_PENDING.name(),
                reason
        );

        payment.markRefunded();
        payment.setExpiryTime(null);
        paymentRepository.save(payment);

        booking.setStatus(BookingStatus.REFUNDED);
        bookingRepository.save(booking);

        plot.setBookingStatus(null);
        plot.setAvailable(true);
        plot.setBlockedAt(null);
        plotRepository.save(plot);

        bookingAgreementRepository.findByBookingId(bookingId).ifPresent(agreement -> {
            if (agreement.getStatus() != com.example.spip_draft.enums.AgreementStatus.SIGNED) {
                agreement.setStatus(com.example.spip_draft.enums.AgreementStatus.REJECTED);
                agreement.setRemarks("Auto-refund processed because escrow freeze deadline expired");
                bookingAgreementRepository.save(agreement);
            }
        });

        paymentAuditService.logSystem(
                payment,
                PaymentAuditEventType.REFUND_COMPLETED,
                EscrowStatus.REFUND_PENDING.name(),
                PaymentStatus.REFUNDED.name(),
                reason
        );

        notificationService.notifyEscrowAutoRefundedToInvestor(payment, reason);
        notificationService.notifyEscrowAutoRefundedToBuilder(payment, reason);
    }
    
    @Transactional
    public PaymentResponse requestUnfreezeReview(Long bookingId, String note) {
        Payment payment = paymentRepository.findByBookingIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        if (!SecurityUtil.isInvestor()) {
            throw new IllegalStateException("Only investor can request unfreeze review");
        }

        validateInvestorOwnership(payment.getBooking());

        if (payment.getEscrowStatus() != EscrowStatus.FROZEN) {
            throw new IllegalStateException("Unfreeze review can be requested only for frozen escrow");
        }

        if (payment.isUnfreezeRequested()) {
            throw new IllegalStateException("Unfreeze review already requested");
        }

        payment.requestUnfreezeReview(note);
        paymentRepository.save(payment);

        paymentAuditService.log(
                payment,
                PaymentAuditEventType.ESCROW_UNFROZEN,
                EscrowStatus.FROZEN.name(),
                EscrowStatus.FROZEN.name(),
                "Investor requested unfreeze review: " + payment.getUnfreezeRequestNote()
        );

        notificationService.notifyAdminsUnfreezeRequested(payment);

        BookingAgreement agreement =
                bookingAgreementRepository.findByBookingId(bookingId).orElse(null);

        return PaymentMapper.toResponse(
                payment,
                agreement,
                canRefund(payment.getBooking(), payment),
                true,
                false,
                "Investor requested admin review"
        );
    }
    
    @Transactional
    public PaymentResponse openDispute(Long bookingId, String reason) {
        Payment payment = paymentRepository.findByBookingIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        if (SecurityUtil.isInvestor()) {
            validateInvestorOwnership(payment.getBooking());
        } else if (!SecurityUtil.isAdmin()) {
            throw new IllegalStateException("Only booking investor or admin can open dispute");
        }

        payment.openDispute(reason);
        paymentRepository.save(payment);

        paymentAuditService.log(
                payment,
                PaymentAuditEventType.DISPUTE_OPENED,
                null,
                EscrowStatus.UNDER_REVIEW.name(),
                reason
        );

        BookingAgreement agreement = bookingAgreementRepository.findByBookingId(bookingId).orElse(null);
        return PaymentMapper.toResponse(payment, agreement, canRefund(payment.getBooking(), payment), true, false, null);
    }

    private void validateInvestorOwnership(Booking booking) {
        if (!SecurityUtil.isInvestor()) {
            return;
        }
        if (booking.getInvestor() == null
                || booking.getInvestor().getUser() == null
                || !booking.getInvestor().getUser().getId().equals(SecurityUtil.getCurrentUserId())) {
            throw new IllegalStateException("This payment/booking does not belong to the current investor");
        }
    }

    private boolean canRefund(Booking booking, Payment payment) {
        if (payment == null || payment.isRefunded() || payment.isEscrowReleased()) {
            return false;
        }

        boolean tokenStageRefund =
                booking.getStatus() == BookingStatus.TOKEN_PAID
                        || booking.getStatus() == BookingStatus.AWAITING_BUILDER_APPROVAL
                        || booking.getStatus() == BookingStatus.BUILDER_REJECTED
                        || booking.getStatus() == BookingStatus.EXPIRED;

        boolean heldEscrowRefund = booking.getStatus() == BookingStatus.CONFIRMED
                && payment.getEscrowStatus() != null
                && (payment.getEscrowStatus() == EscrowStatus.HELD
                || payment.getEscrowStatus() == EscrowStatus.UNDER_REVIEW
                || payment.getEscrowStatus() == EscrowStatus.FROZEN);

        return tokenStageRefund || heldEscrowRefund;
    }
}