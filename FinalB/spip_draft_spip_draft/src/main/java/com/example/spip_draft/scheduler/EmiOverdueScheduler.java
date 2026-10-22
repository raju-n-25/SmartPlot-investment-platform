package com.example.spip_draft.scheduler;

import com.example.spip_draft.entity.EmiInstallment;
import com.example.spip_draft.repository.EmiInstallmentRepository;
import com.example.spip_draft.service.EmiPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EmiOverdueScheduler {

    private final EmiInstallmentRepository emiInstallmentRepository;
    private final EmiPaymentService emiPaymentService;

    @Scheduled(cron = "0 0 0 * * *") // every day at midnight
    @Transactional
    public void markOverdueInstallments() {
        List<EmiInstallment> installments = emiInstallmentRepository.findAll();

        for (EmiInstallment installment : installments) {
            emiPaymentService.refreshInstallmentOverdueState(installment);
        }
    }
}