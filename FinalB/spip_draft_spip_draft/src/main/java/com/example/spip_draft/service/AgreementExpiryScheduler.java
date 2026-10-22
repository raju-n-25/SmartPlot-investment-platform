package com.example.spip_draft.service;

import com.example.spip_draft.entity.BookingAgreement;
import com.example.spip_draft.entity.Payment;
import com.example.spip_draft.enums.AgreementStatus;
import com.example.spip_draft.enums.PaymentAuditEventType;
import com.example.spip_draft.repository.BookingAgreementRepository;
import com.example.spip_draft.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgreementExpiryScheduler {

    private final BookingAgreementRepository bookingAgreementRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentAuditService paymentAuditService;
    private final NotificationService notificationService;

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void expirePendingAgreements() {
        expireByStatus(AgreementStatus.GENERATED);
        expireByStatus(AgreementStatus.SENT);
        expireByStatus(AgreementStatus.INVESTOR_ACCEPTED);
        expireByStatus(AgreementStatus.BUILDER_ACCEPTED);
    }

    private void expireByStatus(AgreementStatus status) {
        List<BookingAgreement> agreements =
                bookingAgreementRepository.findByStatusAndExpiresAtBefore(status, LocalDateTime.now());

        for (BookingAgreement agreement : agreements) {
            AgreementStatus oldStatus = agreement.getStatus();

            agreement.setStatus(AgreementStatus.EXPIRED);
            agreement.setRemarks("Agreement expired before completion");
            bookingAgreementRepository.save(agreement);

            Payment payment = agreement.getPayment();
            if (payment != null) {
                payment.setAgreementStatus(AgreementStatus.EXPIRED);
                payment.markUnderReview("Agreement expired before signing");
                paymentRepository.save(payment);

                paymentAuditService.logSystem(
                        payment,
                        PaymentAuditEventType.AGREEMENT_REJECTED,
                        oldStatus != null ? oldStatus.name() : null,
                        AgreementStatus.EXPIRED.name(),
                        "Agreement expired before completion"
                );
            }

            notificationService.notifyAgreementExpired(agreement.getBooking());

            log.info("Agreement expired: agreementId={}, bookingId={}, oldStatus={}",
                    agreement.getId(),
                    agreement.getBooking() != null ? agreement.getBooking().getId() : null,
                    oldStatus);
        }
    }
}