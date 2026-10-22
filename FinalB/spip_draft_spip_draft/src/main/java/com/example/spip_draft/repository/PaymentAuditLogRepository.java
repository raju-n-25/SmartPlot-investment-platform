package com.example.spip_draft.repository;

import com.example.spip_draft.entity.PaymentAuditLog;
import com.example.spip_draft.enums.PaymentAuditEventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentAuditLogRepository extends JpaRepository<PaymentAuditLog, Long> {

    List<PaymentAuditLog> findByPaymentIdOrderByCreatedAtAsc(Long paymentId);

    List<PaymentAuditLog> findByBookingIdOrderByCreatedAtAsc(Long bookingId);

    boolean existsByBookingIdAndEventTypeAndActorId(Long bookingId,
                                                    PaymentAuditEventType eventType,
                                                    Long actorId);

    Optional<PaymentAuditLog> findFirstByBookingIdAndEventTypeAndActorIdOrderByCreatedAtDesc(Long bookingId,
                                                                                              PaymentAuditEventType eventType,
                                                                                              Long actorId);
}
