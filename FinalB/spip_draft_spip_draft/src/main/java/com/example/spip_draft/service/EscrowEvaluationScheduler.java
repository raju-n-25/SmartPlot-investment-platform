package com.example.spip_draft.service;

import com.example.spip_draft.entity.Payment;
import com.example.spip_draft.enums.EscrowStatus;
import com.example.spip_draft.enums.PaymentAuditEventType;
import com.example.spip_draft.enums.RiskLevel;
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
public class EscrowEvaluationScheduler {

    private final PaymentRepository paymentRepository;
    private final EscrowRuleEngineService escrowRuleEngineService;
    private final EscrowRiskService escrowRiskService;
    private final PaymentAuditService paymentAuditService;

    @Scheduled(fixedRate = 120000)
    @Transactional
    public void evaluateHeldEscrow() {
        List<Payment> payments = paymentRepository.findByEscrowStatus(EscrowStatus.HELD);

        for (Payment payment : payments) {
            int score = escrowRiskService.calculateRiskScore(payment);
            RiskLevel level = escrowRiskService.resolveRiskLevel(score);
            payment.setRiskScore(score);
            payment.setRiskLevel(level);

            if (escrowRuleEngineService.isEligibleForAutoRelease(payment)) {
                payment.markReadyForRelease();
                paymentAuditService.logSystem(payment,
                        PaymentAuditEventType.ESCROW_READY_FOR_RELEASE,
                        EscrowStatus.HELD.name(),
                        EscrowStatus.READY_FOR_RELEASE.name(),
                        "Escrow auto-marked ready for release");
            } else if (level == RiskLevel.HIGH) {
            	String reason = escrowRuleEngineService.firstAutoFailureReason(payment);
                payment.markUnderReview(reason);
                paymentAuditService.logSystem(payment,
                        PaymentAuditEventType.ESCROW_UNDER_REVIEW,
                        EscrowStatus.HELD.name(),
                        EscrowStatus.UNDER_REVIEW.name(),
                        reason);
            }

            paymentRepository.save(payment);
        }
    }
}