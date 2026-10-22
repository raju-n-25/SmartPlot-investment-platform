package com.example.spip_draft.service;

import com.example.spip_draft.dto.agreement.AgreementResponse;
import com.example.spip_draft.entity.Booking;
import com.example.spip_draft.entity.BookingAgreement;
import com.example.spip_draft.entity.Builder;
import com.example.spip_draft.entity.Investor;
import com.example.spip_draft.entity.Payment;
import com.example.spip_draft.enums.AgreementStatus;
import com.example.spip_draft.enums.AgreementType;
import com.example.spip_draft.enums.AuditAction;
import com.example.spip_draft.enums.BookingStatus;
import com.example.spip_draft.enums.EscrowStatus;
import com.example.spip_draft.enums.PaymentAuditEventType;
import com.example.spip_draft.enums.PaymentStatus;
import com.example.spip_draft.enums.RefundMode;
import com.example.spip_draft.repository.BookingAgreementRepository;
import com.example.spip_draft.repository.BookingRepository;
import com.example.spip_draft.repository.BuilderRepository;
import com.example.spip_draft.repository.EmiPlanRepository;
import com.example.spip_draft.repository.InvestorRepository;
import com.example.spip_draft.repository.PaymentRepository;
import com.example.spip_draft.repository.PlotRepository;
import com.example.spip_draft.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AgreementService {

    private final BookingAgreementRepository bookingAgreementRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final InvestorRepository investorRepository;
    private final BuilderRepository builderRepository;
    private final PlotRepository plotRepository;
    private final EmiPlanRepository emiPlanRepository;
    private final PaymentAuditService paymentAuditService;
    private final NotificationService notificationService;
    private final AgreementPdfService agreementPdfService;
    private final WalletAccountService walletAccountService;
    private final AuditLogService auditLogService;

    public AgreementResponse getByBooking(Long bookingId) {
        BookingAgreement agreement = bookingAgreementRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Agreement not found"));

        validateCanAccess(agreement);
        autoExpireIfNeeded(agreement);
        ensurePdfExists(agreement);

        return map(agreement);
    }

    public AgreementResponse generateForBooking(Long bookingId) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        Payment payment = paymentRepository.findByBooking(booking)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        Optional<BookingAgreement> existingOptional = bookingAgreementRepository.findByBookingId(bookingId);

        if (existingOptional.isPresent()) {
            BookingAgreement existing = existingOptional.get();
            existing.setPayment(payment);

            if (existing.getAgreementType() == null) {
                if (emiPlanRepository.existsByBookingId(booking.getId())) {
                    existing.setAgreementType(AgreementType.EMI);
                } else {
                    existing.setAgreementType(AgreementType.FULL_PAYMENT);
                }
            }

            ensurePdfExists(existing);

            bookingAgreementRepository.save(existing);
            return map(existing);
        }

        BookingAgreement agreement = new BookingAgreement();
        agreement.setBooking(booking);
        agreement.setPayment(payment);
        agreement.setTenantId(booking.getTenantId());
        agreement.setInvestorId(booking.getInvestor().getId());
        agreement.setBuilderId(booking.getPlot().getProject().getBuilder().getId());
        agreement.setAgreementNumber("AGR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        if (emiPlanRepository.existsByBookingId(booking.getId())) {
            agreement.setAgreementType(AgreementType.EMI);
            agreement.setRemarks("BrickSquare digitally generated EMI agreement");
        } else {
            agreement.setAgreementType(AgreementType.FULL_PAYMENT);
            agreement.setRemarks("BrickSquare digitally generated full payment agreement");
        }

        agreement.setStatus(AgreementStatus.GENERATED);
        agreement.setGeneratedAt(LocalDateTime.now());
        agreement.setExpiresAt(LocalDateTime.now().plusDays(7));
        agreement.setInvestorSigned(false);
        agreement.setBuilderSigned(false);
        agreement.setInvestorViewedDocument(false);
        agreement.setBuilderViewedDocument(false);

        bookingAgreementRepository.save(agreement);

        agreement.setFileUrl(agreementPdfService.generateAgreementPdf(agreement));
        bookingAgreementRepository.save(agreement);

        auditLogService.log(
                AuditAction.AGREEMENT_GENERATED,
                "AGREEMENT",
                agreement.getId(),
                "Agreement generated for booking " + bookingId,
                null,
                agreement.getStatus().name()
        );

        updatePaymentAgreementStatus(payment, AgreementStatus.GENERATED);

        paymentAuditService.log(
                payment,
                PaymentAuditEventType.AGREEMENT_GENERATED,
                null,
                AgreementStatus.GENERATED.name(),
                "Agreement generated for booking " + bookingId
        );

        notificationService.notifyInvestorAgreementGenerated(booking);
        notificationService.notifyBuilderAgreementGenerated(booking);

        return map(agreement);
    }
    
    public AgreementResponse regenerateExpiredAgreement(Long agreementId) {
        BookingAgreement agreement = getAgreement(agreementId);
        validateCanAccess(agreement);

        if (!SecurityUtil.isBuilder() && !SecurityUtil.isAdmin()) {
            throw new IllegalStateException("Only builder or admin can regenerate expired agreement");
        }

        if (agreement.getStatus() != AgreementStatus.EXPIRED) {
            throw new IllegalStateException("Only expired agreements can be regenerated");
        }

        AgreementStatus oldStatus = agreement.getStatus();

        agreement.setStatus(AgreementStatus.GENERATED);
        agreement.setGeneratedAt(LocalDateTime.now());
        agreement.setSentAt(null);
        agreement.setInvestorAcceptedAt(null);
        agreement.setBuilderAcceptedAt(null);
        agreement.setSignedAt(null);

        agreement.setInvestorSigned(false);
        agreement.setBuilderSigned(false);
        agreement.setInvestorSignedAt(null);
        agreement.setBuilderSignedAt(null);

        agreement.setExpiresAt(LocalDateTime.now().plusDays(7));
        agreement.setRemarks("Agreement regenerated after expiry");

        bookingAgreementRepository.save(agreement);

        agreement.setFileUrl(agreementPdfService.generateAgreementPdf(agreement));
        bookingAgreementRepository.save(agreement);

        updatePaymentAgreementStatus(agreement.getPayment(), AgreementStatus.GENERATED);

        paymentAuditService.log(
                agreement.getPayment(),
                PaymentAuditEventType.AGREEMENT_GENERATED,
                oldStatus.name(),
                AgreementStatus.GENERATED.name(),
                "Agreement regenerated after expiry"
        );

        auditLogService.log(
                AuditAction.AGREEMENT_GENERATED,
                "AGREEMENT",
                agreement.getId(),
                "Expired agreement regenerated for booking " + agreement.getBooking().getId(),
                oldStatus.name(),
                AgreementStatus.GENERATED.name()
        );

        notificationService.notifyAgreementRegenerated(agreement.getBooking());

        return map(agreement);
    }

    private void ensurePdfExists(BookingAgreement agreement) {
        try {
            if (agreement.getFileUrl() == null || agreement.getFileUrl().isBlank()) {
                agreement.setFileUrl(agreementPdfService.generateAgreementPdf(agreement));
                bookingAgreementRepository.save(agreement);
                return;
            }

            String relativePath = agreement.getFileUrl().replaceFirst("^/uploads/", "");

            Path physicalPath = Paths.get("uploads", relativePath)
                    .toAbsolutePath()
                    .normalize();

            if (!Files.exists(physicalPath)) {
                agreement.setFileUrl(agreementPdfService.generateAgreementPdf(agreement));
                bookingAgreementRepository.save(agreement);
            }

        } catch (Exception e) {
            throw new RuntimeException("Agreement PDF repair failed: " + e.getMessage(), e);
        }
    }

    public AgreementResponse sendAgreement(Long agreementId) {
        BookingAgreement agreement = getAgreement(agreementId);
        validateCanAccess(agreement);
        autoExpireIfNeeded(agreement);
        ensurePdfExists(agreement);

        if (agreement.getStatus() == AgreementStatus.SENT
                || agreement.getStatus() == AgreementStatus.INVESTOR_ACCEPTED
                || agreement.getStatus() == AgreementStatus.BUILDER_ACCEPTED
                || agreement.getStatus() == AgreementStatus.SIGNED) {
            return map(agreement);
        }

        if (agreement.getStatus() != AgreementStatus.GENERATED) {
            throw new IllegalStateException("Only generated agreements can be sent");
        }

        AgreementStatus oldStatus = agreement.getStatus();
        agreement.setStatus(AgreementStatus.SENT);
        agreement.setSentAt(LocalDateTime.now());
        bookingAgreementRepository.save(agreement);

        auditLogService.log(
                AuditAction.AGREEMENT_SENT,
                "AGREEMENT",
                agreement.getId(),
                "Agreement sent",
                oldStatus.name(),
                AgreementStatus.SENT.name()
        );

        updatePaymentAgreementStatus(agreement.getPayment(), AgreementStatus.SENT);

        paymentAuditService.log(
                agreement.getPayment(),
                PaymentAuditEventType.AGREEMENT_SENT,
                oldStatus.name(),
                AgreementStatus.SENT.name(),
                "Agreement sent"
        );

        notificationService.notifyBuilderAgreementSent(agreement.getBooking());
        notificationService.notifyInvestorAgreementSent(agreement.getBooking());

        return map(agreement);
    }

    public AgreementResponse markViewed(Long agreementId) {
        BookingAgreement agreement = getAgreement(agreementId);
        validateCanAccess(agreement);
        autoExpireIfNeeded(agreement);
        ensurePdfExists(agreement);

        if (agreement.getStatus() == AgreementStatus.REJECTED
                || agreement.getStatus() == AgreementStatus.EXPIRED) {
            throw new IllegalStateException("Agreement is not available for review");
        }

        if (SecurityUtil.isInvestor()) {
            agreement.setInvestorViewedDocument(true);
            agreement.setInvestorViewedDocumentAt(LocalDateTime.now());
            agreement.setLastActionBy("INVESTOR");
            agreement.setLastActionMessage("Investor viewed agreement document");
        } else if (SecurityUtil.isBuilder()) {
            agreement.setBuilderViewedDocument(true);
            agreement.setBuilderViewedDocumentAt(LocalDateTime.now());
            agreement.setLastActionBy("BUILDER");
            agreement.setLastActionMessage("Builder viewed agreement document");
        } else if (SecurityUtil.isAdmin()) {
            agreement.setLastActionBy("ADMIN");
            agreement.setLastActionMessage("Admin viewed agreement document");
        } else {
            throw new IllegalStateException("Invalid agreement viewer");
        }

        bookingAgreementRepository.save(agreement);

        try {
            paymentAuditService.log(
                    agreement.getPayment(),
                    PaymentAuditEventType.AGREEMENT_VIEWED,
                    null,
                    SecurityUtil.getCurrentUserRole(),
                    "Agreement document opened by current actor"
            );
        } catch (Exception ignored) {
            // Do not block agreement review if audit logging fails
        }

        return map(agreement);
    }

    public AgreementResponse investorAccept(Long agreementId) {
        BookingAgreement agreement = getAgreement(agreementId);
        validateInvestorActor(agreement);
        autoExpireIfNeeded(agreement);
        ensurePdfExists(agreement);

        if (agreement.getStatus() == AgreementStatus.INVESTOR_ACCEPTED
                || agreement.getStatus() == AgreementStatus.BUILDER_ACCEPTED
                || agreement.getStatus() == AgreementStatus.SIGNED) {
            return map(agreement);
        }

        if (agreement.getStatus() != AgreementStatus.SENT) {
            throw new IllegalStateException("Agreement must be sent before investor acceptance");
        }

        ensureActorViewedDocument(agreement);

        AgreementStatus oldStatus = agreement.getStatus();
        agreement.setStatus(AgreementStatus.INVESTOR_ACCEPTED);
        agreement.setInvestorAcceptedAt(LocalDateTime.now());
        bookingAgreementRepository.save(agreement);

        auditLogService.log(
                AuditAction.AGREEMENT_INVESTOR_ACCEPTED,
                "AGREEMENT",
                agreement.getId(),
                "Investor accepted agreement",
                oldStatus.name(),
                AgreementStatus.INVESTOR_ACCEPTED.name()
        );

        updatePaymentAgreementStatus(agreement.getPayment(), AgreementStatus.INVESTOR_ACCEPTED);

        paymentAuditService.log(
                agreement.getPayment(),
                PaymentAuditEventType.AGREEMENT_INVESTOR_ACCEPTED,
                oldStatus.name(),
                AgreementStatus.INVESTOR_ACCEPTED.name(),
                "Investor accepted agreement"
        );

        notificationService.notifyBuilderInvestorAcceptedAgreement(agreement.getBooking());

        return map(agreement);
    }

    public AgreementResponse builderAccept(Long agreementId) {
        BookingAgreement agreement = getAgreement(agreementId);
        validateBuilderActor(agreement);
        autoExpireIfNeeded(agreement);
        ensurePdfExists(agreement);

        if (agreement.getStatus() == AgreementStatus.BUILDER_ACCEPTED || agreement.getStatus() == AgreementStatus.SIGNED) {
            return map(agreement);
        }

        if (agreement.getStatus() != AgreementStatus.INVESTOR_ACCEPTED) {
            throw new IllegalStateException("Investor must accept before builder acceptance");
        }

        ensureActorViewedDocument(agreement);

        AgreementStatus oldStatus = agreement.getStatus();
        agreement.setStatus(AgreementStatus.BUILDER_ACCEPTED);
        agreement.setBuilderAcceptedAt(LocalDateTime.now());
        bookingAgreementRepository.save(agreement);

        auditLogService.log(
                AuditAction.AGREEMENT_BUILDER_ACCEPTED,
                "AGREEMENT",
                agreement.getId(),
                "Builder accepted agreement",
                oldStatus.name(),
                AgreementStatus.BUILDER_ACCEPTED.name()
        );

        updatePaymentAgreementStatus(agreement.getPayment(), AgreementStatus.BUILDER_ACCEPTED);

        paymentAuditService.log(
                agreement.getPayment(),
                PaymentAuditEventType.AGREEMENT_BUILDER_ACCEPTED,
                oldStatus.name(),
                AgreementStatus.BUILDER_ACCEPTED.name(),
                "Builder accepted agreement"
        );

        notificationService.notifyInvestorBuilderAcceptedAgreement(agreement.getBooking());

        return map(agreement);
    }

    public AgreementResponse investorSign(Long agreementId) {
        BookingAgreement agreement = getAgreement(agreementId);
        validateInvestorActor(agreement);
        autoExpireIfNeeded(agreement);
        ensurePdfExists(agreement);

        if (Boolean.TRUE.equals(agreement.getInvestorSigned())) {
            finalizeIfBothSigned(agreement);

            if (agreement.getStatus() == AgreementStatus.SIGNED) {
                bookingAgreementRepository.save(agreement);
                updatePaymentAgreementStatus(agreement.getPayment(), AgreementStatus.SIGNED);
            }

            return map(agreement);
        }

        if (agreement.getStatus() != AgreementStatus.BUILDER_ACCEPTED) {
            throw new IllegalStateException("Builder must accept before investor signature");
        }

        ensureActorViewedDocument(agreement);

        agreement.setInvestorSigned(true);
        agreement.setInvestorSignedAt(LocalDateTime.now());

        finalizeIfBothSigned(agreement);

        bookingAgreementRepository.save(agreement);
        agreement.setFileUrl(agreementPdfService.generateAgreementPdf(agreement));
        bookingAgreementRepository.save(agreement);

        if (agreement.getStatus() == AgreementStatus.SIGNED) {
            updatePaymentAgreementStatus(agreement.getPayment(), AgreementStatus.SIGNED);

            paymentAuditService.log(
                    agreement.getPayment(),
                    PaymentAuditEventType.AGREEMENT_SIGNED,
                    AgreementStatus.BUILDER_ACCEPTED.name(),
                    AgreementStatus.SIGNED.name(),
                    "Agreement fully signed"
            );

            auditLogService.log(
                    AuditAction.AGREEMENT_SIGNED,
                    "AGREEMENT",
                    agreement.getId(),
                    "Agreement fully signed",
                    AgreementStatus.BUILDER_ACCEPTED.name(),
                    AgreementStatus.SIGNED.name()
            );

            notificationService.notifyAgreementCompleted(agreement.getBooking());
        } else {
            updatePaymentAgreementStatus(agreement.getPayment(), AgreementStatus.BUILDER_ACCEPTED);
            notificationService.notifyBuilderInvestorSignedAgreement(agreement.getBooking());
        }

        return map(agreement);
    }

    public AgreementResponse builderSign(Long agreementId) {
        BookingAgreement agreement = getAgreement(agreementId);
        validateBuilderActor(agreement);
        autoExpireIfNeeded(agreement);
        ensurePdfExists(agreement);

        if (Boolean.TRUE.equals(agreement.getBuilderSigned())) {
            finalizeIfBothSigned(agreement);

            if (agreement.getStatus() == AgreementStatus.SIGNED) {
                bookingAgreementRepository.save(agreement);
                updatePaymentAgreementStatus(agreement.getPayment(), AgreementStatus.SIGNED);
            }

            return map(agreement);
        }

        if (agreement.getStatus() != AgreementStatus.BUILDER_ACCEPTED) {
            throw new IllegalStateException("Agreement must be builder-accepted before signature");
        }

        ensureActorViewedDocument(agreement);

        agreement.setBuilderSigned(true);
        agreement.setBuilderSignedAt(LocalDateTime.now());

        finalizeIfBothSigned(agreement);

        bookingAgreementRepository.save(agreement);
        agreement.setFileUrl(agreementPdfService.generateAgreementPdf(agreement));
        bookingAgreementRepository.save(agreement);

        if (agreement.getStatus() == AgreementStatus.SIGNED) {
            updatePaymentAgreementStatus(agreement.getPayment(), AgreementStatus.SIGNED);

            paymentAuditService.log(
                    agreement.getPayment(),
                    PaymentAuditEventType.AGREEMENT_SIGNED,
                    AgreementStatus.BUILDER_ACCEPTED.name(),
                    AgreementStatus.SIGNED.name(),
                    "Agreement fully signed"
            );

            auditLogService.log(
                    AuditAction.AGREEMENT_SIGNED,
                    "AGREEMENT",
                    agreement.getId(),
                    "Agreement fully signed",
                    AgreementStatus.BUILDER_ACCEPTED.name(),
                    AgreementStatus.SIGNED.name()
            );

            notificationService.notifyAgreementCompleted(agreement.getBooking());
        } else {
            updatePaymentAgreementStatus(agreement.getPayment(), AgreementStatus.BUILDER_ACCEPTED);
            notificationService.notifyInvestorBuilderSignedAgreement(agreement.getBooking());
        }

        return map(agreement);
    }

    public AgreementResponse investorRejectAndRefund(Long agreementId, RefundMode refundMode, String reason) {
        BookingAgreement agreement = getAgreement(agreementId);
        validateInvestorActor(agreement);
        autoExpireIfNeeded(agreement);

        if (agreement.getStatus() == AgreementStatus.SIGNED) {
            throw new IllegalStateException("Signed agreement cannot be rejected for refund");
        }

        if (agreement.getStatus() == AgreementStatus.REJECTED) {
            return map(agreement);
        }

        AgreementStatus oldStatus = agreement.getStatus();

        agreement.setStatus(AgreementStatus.REJECTED);
        agreement.setRemarks(reason == null || reason.isBlank()
                ? "Investor rejected the agreement and initiated refund"
                : reason.trim());

        bookingAgreementRepository.save(agreement);

        auditLogService.log(
                AuditAction.AGREEMENT_REJECTED,
                "AGREEMENT",
                agreement.getId(),
                "Investor rejected agreement and initiated refund",
                oldStatus != null ? oldStatus.name() : null,
                AgreementStatus.REJECTED.name()
        );

        updatePaymentAgreementStatus(agreement.getPayment(), AgreementStatus.REJECTED);

        paymentAuditService.log(
                agreement.getPayment(),
                PaymentAuditEventType.AGREEMENT_REJECTED,
                oldStatus != null ? oldStatus.name() : null,
                AgreementStatus.REJECTED.name(),
                agreement.getRemarks()
        );

        processAgreementRefund(agreement, refundMode);
        notificationService.notifyAgreementRejected(agreement.getBooking(), agreement.getRemarks());

        return map(agreement);
    }

    private BookingAgreement getAgreement(Long agreementId) {
        return bookingAgreementRepository.findDetailedById(agreementId)
                .orElseThrow(() -> new IllegalArgumentException("Agreement not found"));
    }

    private void finalizeIfBothSigned(BookingAgreement agreement) {
        if (Boolean.TRUE.equals(agreement.getInvestorSigned())
                && Boolean.TRUE.equals(agreement.getBuilderSigned())) {
            agreement.setStatus(AgreementStatus.SIGNED);

            if (agreement.getSignedAt() == null) {
                agreement.setSignedAt(LocalDateTime.now());
            }
        }
    }

    private void updatePaymentAgreementStatus(Payment payment, AgreementStatus status) {
        if (payment == null) {
            return;
        }

        payment.setAgreementStatus(status);
        paymentRepository.save(payment);
    }

    private void autoExpireIfNeeded(BookingAgreement agreement) {
        if (agreement.getStatus() == AgreementStatus.SIGNED
                || agreement.getStatus() == AgreementStatus.REJECTED
                || agreement.getStatus() == AgreementStatus.EXPIRED) {
            return;
        }

        if (agreement.getExpiresAt() != null && agreement.getExpiresAt().isBefore(LocalDateTime.now())) {
            AgreementStatus oldStatus = agreement.getStatus();

            agreement.setStatus(AgreementStatus.EXPIRED);
            agreement.setRemarks("Agreement expired before completion");
            bookingAgreementRepository.save(agreement);

            updatePaymentAgreementStatus(agreement.getPayment(), AgreementStatus.EXPIRED);

            paymentAuditService.log(
                    agreement.getPayment(),
                    PaymentAuditEventType.AGREEMENT_REJECTED,
                    oldStatus != null ? oldStatus.name() : null,
                    AgreementStatus.EXPIRED.name(),
                    "Agreement expired before completion"
            );

            notificationService.notifyAgreementExpired(agreement.getBooking());
        }
    }
    private void ensureActorViewedDocument(BookingAgreement agreement) {
        if (SecurityUtil.isInvestor()) {
            if (!Boolean.TRUE.equals(agreement.getInvestorViewedDocument())) {
                throw new IllegalStateException("Open the agreement document before accepting or signing");
            }
            return;
        }

        if (SecurityUtil.isBuilder()) {
            if (!Boolean.TRUE.equals(agreement.getBuilderViewedDocument())) {
                throw new IllegalStateException("Open the agreement document before accepting or signing");
            }
            return;
        }

        if (SecurityUtil.isAdmin()) {
            return;
        }

        throw new IllegalStateException("Invalid agreement actor");
    }
    private void validateCanAccess(BookingAgreement agreement) {
        if (SecurityUtil.isAdmin()) {
            return;
        }

        Long currentUserId = SecurityUtil.getCurrentUserId();

        boolean investorOwner = agreement.getBooking().getInvestor() != null
                && agreement.getBooking().getInvestor().getUser() != null
                && agreement.getBooking().getInvestor().getUser().getId().equals(currentUserId);

        boolean builderOwner = agreement.getBooking().getPlot() != null
                && agreement.getBooking().getPlot().getProject() != null
                && agreement.getBooking().getPlot().getProject().getBuilder() != null
                && agreement.getBooking().getPlot().getProject().getBuilder().getUser() != null
                && agreement.getBooking().getPlot().getProject().getBuilder().getUser().getId().equals(currentUserId);

        if (!investorOwner && !builderOwner) {
            throw new IllegalStateException("You are not allowed to access this agreement");
        }
    }

    private void validateInvestorActor(BookingAgreement agreement) {
        validateCanAccess(agreement);

        if (!SecurityUtil.isInvestor()) {
            throw new IllegalStateException("Only investor can perform this action");
        }

        Investor investor = investorRepository.findByUserId(SecurityUtil.getCurrentUserId())
                .orElseThrow(() -> new IllegalStateException("Investor not found"));

        if (!investor.getId().equals(agreement.getInvestorId())) {
            throw new IllegalStateException("This agreement does not belong to the current investor");
        }
    }

    private void validateBuilderActor(BookingAgreement agreement) {
        validateCanAccess(agreement);

        if (!SecurityUtil.isBuilder()) {
            throw new IllegalStateException("Only builder can perform this action");
        }

        Builder builder = builderRepository.findByUserId(SecurityUtil.getCurrentUserId())
                .orElseThrow(() -> new IllegalStateException("Builder not found"));

        if (!builder.getId().equals(agreement.getBuilderId())) {
            throw new IllegalStateException("This agreement does not belong to the current builder");
        }
    }

    private void processAgreementRefund(BookingAgreement agreement, RefundMode mode) {
        Booking booking = agreement.getBooking();
        Payment payment = agreement.getPayment();

        if (payment == null) {
            throw new IllegalStateException("Payment not found for refund");
        }

        if (payment.getPaymentStatus() == PaymentStatus.REFUNDED) {
            throw new IllegalStateException("Booking payment already refunded");
        }

        if (payment.isEscrowReleased()) {
            throw new IllegalStateException("Refund not allowed after escrow release");
        }

        if (payment.getAmount() == null || payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Invalid refund amount");
        }

        boolean tokenStageRefund = booking.getStatus() == BookingStatus.TOKEN_PAID
                || booking.getStatus() == BookingStatus.AWAITING_BUILDER_APPROVAL
                || booking.getStatus() == BookingStatus.BUILDER_REJECTED;

        boolean heldEscrowRefund = booking.getStatus() == BookingStatus.CONFIRMED
                && payment.getEscrowStatus() != null
                && (payment.getEscrowStatus() == EscrowStatus.HELD
                || payment.getEscrowStatus() == EscrowStatus.UNDER_REVIEW
                || payment.getEscrowStatus() == EscrowStatus.FROZEN);

        if (!tokenStageRefund && !heldEscrowRefund) {
            throw new IllegalStateException("Refund not allowed in current state");
        }

        payment.markRefundPending();
        paymentRepository.save(payment);

        paymentAuditService.log(
                payment,
                PaymentAuditEventType.REFUND_PENDING,
                null,
                EscrowStatus.REFUND_PENDING.name(),
                "Refund initiated from agreement rejection"
        );

        if (mode == RefundMode.WALLET) {
            walletAccountService.creditWallet(
                    booking.getInvestor(),
                    payment.getAmount(),
                    "Agreement rejection refund credited to wallet for booking " + booking.getId(),
                    booking.getId()
            );
        } else if (mode != RefundMode.ORIGINAL) {
            throw new IllegalStateException("Invalid refund mode");
        }

        payment.markRefunded();
        payment.setExpiryTime(null);
        paymentRepository.save(payment);

        booking.setStatus(BookingStatus.REFUNDED);
        bookingRepository.save(booking);

        if (booking.getPlot() != null) {
            booking.getPlot().setBookingStatus(null);
            booking.getPlot().setAvailable(true);
            booking.getPlot().setBlockedAt(null);
            plotRepository.save(booking.getPlot());
        }

        paymentAuditService.log(
                payment,
                PaymentAuditEventType.REFUND_COMPLETED,
                null,
                PaymentStatus.REFUNDED.name(),
                "Refund completed via " + mode + " after agreement rejection"
        );
    }

    private AgreementResponse map(BookingAgreement agreement) {
        AgreementResponse response = new AgreementResponse();

        Long currentUserId = SecurityUtil.getCurrentUserId();

        boolean currentUserInvestor = SecurityUtil.isInvestor()
                && agreement.getBooking().getInvestor() != null
                && agreement.getBooking().getInvestor().getUser() != null
                && agreement.getBooking().getInvestor().getUser().getId().equals(currentUserId);

        boolean currentUserBuilder = SecurityUtil.isBuilder()
                && agreement.getBooking().getPlot() != null
                && agreement.getBooking().getPlot().getProject() != null
                && agreement.getBooking().getPlot().getProject().getBuilder() != null
                && agreement.getBooking().getPlot().getProject().getBuilder().getUser() != null
                && agreement.getBooking().getPlot().getProject().getBuilder().getUser().getId().equals(currentUserId);

        boolean investorViewed = agreement.getBooking().getInvestor() != null
                && agreement.getBooking().getInvestor().getUser() != null
                && paymentAuditService.hasActorViewedAgreement(
                agreement.getBooking().getId(),
                agreement.getBooking().getInvestor().getUser().getId()
        );

        boolean builderViewed = agreement.getBooking().getPlot() != null
                && agreement.getBooking().getPlot().getProject() != null
                && agreement.getBooking().getPlot().getProject().getBuilder() != null
                && agreement.getBooking().getPlot().getProject().getBuilder().getUser() != null
                && paymentAuditService.hasActorViewedAgreement(
                agreement.getBooking().getId(),
                agreement.getBooking().getPlot().getProject().getBuilder().getUser().getId()
        );

        LocalDateTime investorViewedAt = agreement.getBooking().getInvestor() != null
                && agreement.getBooking().getInvestor().getUser() != null
                ? paymentAuditService.getLatestActorViewLog(
                agreement.getBooking().getId(),
                agreement.getBooking().getInvestor().getUser().getId()
        ).map(log -> log.getCreatedAt()).orElse(null)
                : null;

        LocalDateTime builderViewedAt = agreement.getBooking().getPlot() != null
                && agreement.getBooking().getPlot().getProject() != null
                && agreement.getBooking().getPlot().getProject().getBuilder() != null
                && agreement.getBooking().getPlot().getProject().getBuilder().getUser() != null
                ? paymentAuditService.getLatestActorViewLog(
                agreement.getBooking().getId(),
                agreement.getBooking().getPlot().getProject().getBuilder().getUser().getId()
        ).map(log -> log.getCreatedAt()).orElse(null)
                : null;

        response.setId(agreement.getId());
        response.setBookingId(agreement.getBooking().getId());
        response.setPaymentId(agreement.getPayment() != null ? agreement.getPayment().getId() : null);
        response.setTenantId(agreement.getTenantId());
        response.setInvestorId(agreement.getInvestorId());
        response.setBuilderId(agreement.getBuilderId());
        response.setAgreementNumber(agreement.getAgreementNumber());
        response.setAgreementType(agreement.getAgreementType() != null ? agreement.getAgreementType().name() : null);

        response.setFileUrl(agreement.getFileUrl());
        response.setReviewFileUrl(agreement.getFileUrl());
        response.setExecutedFileUrl(agreement.getStatus() == AgreementStatus.SIGNED ? agreement.getFileUrl() : null);

        response.setStatus(agreement.getStatus().name());
        response.setCurrentStage(resolveCurrentStage(agreement));
        response.setNextAction(resolveNextAction(agreement, currentUserInvestor, currentUserBuilder));
        response.setNextActionMessage(resolveNextActionMessage(agreement, currentUserInvestor, currentUserBuilder, investorViewed, builderViewed));

        response.setGeneratedAt(agreement.getGeneratedAt());
        response.setSentAt(agreement.getSentAt());
        response.setInvestorViewedAt(investorViewedAt);
        response.setBuilderViewedAt(builderViewedAt);
        response.setInvestorAcceptedAt(agreement.getInvestorAcceptedAt());
        response.setBuilderAcceptedAt(agreement.getBuilderAcceptedAt());
        response.setSignedAt(agreement.getSignedAt());
        response.setExpiresAt(agreement.getExpiresAt());
        response.setRemarks(agreement.getRemarks());

        response.setInvestorViewed(investorViewed);
        response.setBuilderViewed(builderViewed);
        response.setInvestorSigned(Boolean.TRUE.equals(agreement.getInvestorSigned()));
        response.setBuilderSigned(Boolean.TRUE.equals(agreement.getBuilderSigned()));
        response.setInvestorSignedAt(agreement.getInvestorSignedAt());
        response.setBuilderSignedAt(agreement.getBuilderSignedAt());

        response.setCompleted(agreement.getStatus() == AgreementStatus.SIGNED);
        response.setRejected(agreement.getStatus() == AgreementStatus.REJECTED);
        response.setExpired(agreement.getStatus() == AgreementStatus.EXPIRED);
        response.setExecutedDocumentAvailable(agreement.getStatus() == AgreementStatus.SIGNED);

        response.setInvestorName(agreement.getBooking().getInvestor().getUser().getFullName());
        response.setBuilderName(agreement.getBooking().getPlot().getProject().getBuilder().getCompanyName());
        response.setProjectName(agreement.getBooking().getPlot().getProject().getProjectName());
        response.setPlotNumber(agreement.getBooking().getPlot().getPlotNumber());

        response.setCurrentUserInvestor(currentUserInvestor);
        response.setCurrentUserBuilder(currentUserBuilder);
        response.setCurrentUserAdmin(SecurityUtil.isAdmin());

        response.setCanSend(canSend(agreement, currentUserInvestor, currentUserBuilder));
        response.setCanMarkViewed(canMarkViewed(agreement));
        response.setCanInvestorAccept(currentUserInvestor && agreement.getStatus() == AgreementStatus.SENT && investorViewed);
        response.setCanBuilderAccept(currentUserBuilder && agreement.getStatus() == AgreementStatus.INVESTOR_ACCEPTED && builderViewed);

        response.setCanInvestorSign(
                currentUserInvestor
                        && agreement.getStatus() == AgreementStatus.BUILDER_ACCEPTED
                        && !Boolean.TRUE.equals(agreement.getInvestorSigned())
                        && investorViewed
        );

        response.setCanBuilderSign(
                currentUserBuilder
                        && agreement.getStatus() == AgreementStatus.BUILDER_ACCEPTED
                        && !Boolean.TRUE.equals(agreement.getBuilderSigned())
                        && builderViewed
        );

        response.setCanRejectAndRefund(currentUserInvestor
                && agreement.getStatus() != AgreementStatus.SIGNED
                && agreement.getStatus() != AgreementStatus.REJECTED
                && agreement.getStatus() != AgreementStatus.EXPIRED);

        response.setMustOpenBeforeAccept(true);
        response.setMustOpenBeforeSign(true);

        return response;
    }

    private boolean canSend(BookingAgreement agreement, boolean currentUserInvestor, boolean currentUserBuilder) {
        if (agreement.getStatus() != AgreementStatus.GENERATED) {
            return false;
        }

        return SecurityUtil.isAdmin() || currentUserBuilder || currentUserInvestor;
    }

    private boolean canMarkViewed(BookingAgreement agreement) {
        return agreement.getStatus() != AgreementStatus.REJECTED && agreement.getStatus() != AgreementStatus.EXPIRED;
    }

    private String resolveCurrentStage(BookingAgreement agreement) {
        return switch (agreement.getStatus()) {
            case GENERATED -> "GENERATED";
            case SENT -> "REVIEW_PENDING";
            case INVESTOR_ACCEPTED -> "BUILDER_REVIEW_PENDING";
            case BUILDER_ACCEPTED -> "SIGNATURE_PENDING";
            case SIGNED -> "COMPLETED";
            case REJECTED -> "REJECTED";
            case EXPIRED -> "EXPIRED";
            default -> agreement.getStatus().name();
        };
    }

    private String resolveNextAction(BookingAgreement agreement,
                                     boolean currentUserInvestor,
                                     boolean currentUserBuilder) {
        if (agreement.getStatus() == AgreementStatus.SIGNED) {
            return "DOWNLOAD_EXECUTED_AGREEMENT";
        }

        if (agreement.getStatus() == AgreementStatus.REJECTED) {
            return "REFUND_IN_PROGRESS_OR_COMPLETED";
        }

        if (agreement.getStatus() == AgreementStatus.EXPIRED) {
            return "AGREEMENT_EXPIRED";
        }

        if (currentUserInvestor && agreement.getStatus() == AgreementStatus.SENT) {
            return "OPEN_AND_ACCEPT_AGREEMENT";
        }

        if (currentUserBuilder && agreement.getStatus() == AgreementStatus.INVESTOR_ACCEPTED) {
            return "OPEN_AND_ACCEPT_AGREEMENT";
        }

        if (currentUserInvestor
                && agreement.getStatus() == AgreementStatus.BUILDER_ACCEPTED
                && !Boolean.TRUE.equals(agreement.getInvestorSigned())) {
            return "SIGN_AGREEMENT";
        }

        if (currentUserBuilder
                && agreement.getStatus() == AgreementStatus.BUILDER_ACCEPTED
                && !Boolean.TRUE.equals(agreement.getBuilderSigned())) {
            return "SIGN_AGREEMENT";
        }

        if (agreement.getStatus() == AgreementStatus.BUILDER_ACCEPTED) {
            return "WAITING_FOR_OTHER_SIGNATURE";
        }

        return "WAIT";
    }

    private String resolveNextActionMessage(BookingAgreement agreement,
                                            boolean currentUserInvestor,
                                            boolean currentUserBuilder,
                                            boolean investorViewed,
                                            boolean builderViewed) {
        if (agreement.getStatus() == AgreementStatus.SIGNED) {
            return "Agreement is fully signed. Use the executed document as the final downloadable version.";
        }

        if (agreement.getStatus() == AgreementStatus.REJECTED) {
            return "Agreement was rejected. Refund/dispute flow has been triggered according to the payment state.";
        }

        if (agreement.getStatus() == AgreementStatus.EXPIRED) {
            return "Agreement validity expired before completion.";
        }

        if (currentUserInvestor && agreement.getStatus() == AgreementStatus.SENT && !investorViewed) {
            return "Open the agreement PDF first. After review, you can accept or reject with refund.";
        }

        if (currentUserInvestor && agreement.getStatus() == AgreementStatus.SENT) {
            return "Review complete. You can now accept the agreement or reject and request refund.";
        }

        if (currentUserBuilder && agreement.getStatus() == AgreementStatus.INVESTOR_ACCEPTED && !builderViewed) {
            return "Investor has accepted the agreement. Open the agreement PDF before builder acceptance.";
        }

        if (currentUserBuilder && agreement.getStatus() == AgreementStatus.INVESTOR_ACCEPTED) {
            return "Investor has accepted the agreement. You can now accept it from builder side.";
        }

        if (currentUserInvestor && agreement.getStatus() == AgreementStatus.BUILDER_ACCEPTED && !investorViewed) {
            return "Open the agreement PDF before signing from investor side.";
        }

        if (currentUserBuilder && agreement.getStatus() == AgreementStatus.BUILDER_ACCEPTED && !builderViewed) {
            return "Open the agreement PDF before signing from builder side.";
        }

        if (currentUserInvestor
                && agreement.getStatus() == AgreementStatus.BUILDER_ACCEPTED
                && !Boolean.TRUE.equals(agreement.getInvestorSigned())) {
            return "Builder has accepted. Investor signature is pending.";
        }

        if (currentUserBuilder
                && agreement.getStatus() == AgreementStatus.BUILDER_ACCEPTED
                && !Boolean.TRUE.equals(agreement.getBuilderSigned())) {
            return "Investor is waiting for builder signature to complete the agreement.";
        }

        if (agreement.getStatus() == AgreementStatus.BUILDER_ACCEPTED) {
            return "One party has signed. Waiting for the other signature to complete the executed agreement.";
        }

        return "Agreement is progressing.";
    }
}