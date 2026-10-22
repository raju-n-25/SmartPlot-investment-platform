package com.example.spip_draft.service;

import com.example.spip_draft.entity.Payment;
import com.example.spip_draft.entity.PaymentAuditLog;
import com.example.spip_draft.enums.PaymentAuditEventType;
import com.example.spip_draft.repository.PaymentAuditLogRepository;
import com.example.spip_draft.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentAuditService {

    private final PaymentAuditLogRepository paymentAuditLogRepository;

    public void log(Payment payment,
                    PaymentAuditEventType eventType,
                    String oldValue,
                    String newValue,
                    String remarks) {

        PaymentAuditLog log = new PaymentAuditLog();
        log.setPaymentId(payment.getId());
        log.setBookingId(payment.getBooking().getId());
        log.setTenantId(payment.getTenantId());
        log.setEventType(eventType);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setActorType("USER");
        log.setActorId(SecurityUtil.getCurrentUserId());
        log.setRemarks(remarks);

        paymentAuditLogRepository.save(log);
    }

    public void logSystem(Payment payment,
                          PaymentAuditEventType eventType,
                          String oldValue,
                          String newValue,
                          String remarks) {

        PaymentAuditLog log = new PaymentAuditLog();
        log.setPaymentId(payment.getId());
        log.setBookingId(payment.getBooking().getId());
        log.setTenantId(payment.getTenantId());
        log.setEventType(eventType);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setActorType("SYSTEM");
        log.setActorId(null);
        log.setRemarks(remarks);

        paymentAuditLogRepository.save(log);
    }

    public boolean hasActorViewedAgreement(Long bookingId, Long actorUserId) {
        return paymentAuditLogRepository.existsByBookingIdAndEventTypeAndActorId(
                bookingId,
                PaymentAuditEventType.AGREEMENT_VIEWED,
                actorUserId
        );
    }

    public Optional<PaymentAuditLog> getLatestActorViewLog(Long bookingId, Long actorUserId) {
        return paymentAuditLogRepository.findFirstByBookingIdAndEventTypeAndActorIdOrderByCreatedAtDesc(
                bookingId,
                PaymentAuditEventType.AGREEMENT_VIEWED,
                actorUserId
        );
    }

    @Transactional(readOnly = true)
    public List<PaymentAuditLog> getBookingAuditTrail(Long bookingId) {
        return paymentAuditLogRepository.findByBookingIdOrderByCreatedAtAsc(bookingId);
    }
}
