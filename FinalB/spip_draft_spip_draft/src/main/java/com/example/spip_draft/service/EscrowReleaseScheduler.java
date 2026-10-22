package com.example.spip_draft.service;

import com.example.spip_draft.entity.Payment;
import com.example.spip_draft.enums.EscrowStatus;
import com.example.spip_draft.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EscrowReleaseScheduler {

    private final PaymentRepository paymentRepository;
    private final PaymentOrchestrator paymentOrchestrator;

    @Scheduled(fixedRate = 180000)
    @Transactional
    public void releaseReadyEscrow() {
        List<Payment> payments = paymentRepository.findByEscrowStatus(EscrowStatus.READY_FOR_RELEASE);

        for (Payment payment : payments) {
            try {
                paymentOrchestrator.releaseEscrowBySystem(payment.getBooking().getId());
            } catch (Exception ex) {
                payment.markReleaseFailed(ex.getMessage());
                paymentRepository.save(payment);

                log.error(
                        "Escrow auto-release failed | bookingId={} | paymentId={} | error={}",
                        payment.getBooking().getId(),
                        payment.getId(),
                        ex.getMessage()
                );
            }
        }
    }
}