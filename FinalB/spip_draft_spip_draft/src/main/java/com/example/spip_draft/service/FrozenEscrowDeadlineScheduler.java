package com.example.spip_draft.service;

import com.example.spip_draft.entity.Payment;
import com.example.spip_draft.enums.EscrowStatus;
import com.example.spip_draft.enums.PaymentAuditEventType;
import com.example.spip_draft.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FrozenEscrowDeadlineScheduler {

    private final PaymentRepository paymentRepository;
    private final PaymentOrchestrator paymentOrchestrator;
    private final PaymentAuditService paymentAuditService;
    private final NotificationService notificationService;

    @Scheduled(fixedRate = 3600000)
    public void processExpiredFrozenEscrow() {
        List<Payment> expired = paymentRepository.findByEscrowStatusAndFreezeDeadlineAtBefore(
                EscrowStatus.FROZEN,
                LocalDateTime.now()
        );

        for (Payment payment : expired) {
            try {
                paymentAuditService.logSystem(
                        payment,
                        PaymentAuditEventType.ESCROW_FREEZE_DEADLINE_EXPIRED,
                        EscrowStatus.FROZEN.name(),
                        EscrowStatus.FROZEN.name(),
                        "Freeze resolution deadline expired"
                );

                notificationService.notifyEscrowFreezeDeadlineExpiredToAdmin(payment);

                paymentOrchestrator.autoRefundFrozenEscrow(
                        payment.getBooking().getId(),
                        "Escrow freeze deadline expired without required action"
                );

            } catch (Exception ex) {
                log.error(
                        "Frozen escrow deadline processing failed | paymentId={} | bookingId={} | error={}",
                        payment.getId(),
                        payment.getBooking() != null ? payment.getBooking().getId() : null,
                        ex.getMessage()
                );
            }
        }
    }
}