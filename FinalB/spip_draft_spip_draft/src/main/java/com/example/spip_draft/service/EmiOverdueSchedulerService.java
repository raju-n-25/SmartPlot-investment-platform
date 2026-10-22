package com.example.spip_draft.service;

import com.example.spip_draft.entity.EmiInstallment;
import com.example.spip_draft.entity.EmiPlan;
import com.example.spip_draft.enums.BookingStatus;
import com.example.spip_draft.enums.EmiPlanStatus;
import com.example.spip_draft.enums.InstallmentStatus;
import com.example.spip_draft.repository.BookingRepository;
import com.example.spip_draft.repository.EmiInstallmentRepository;
import com.example.spip_draft.repository.EmiPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EmiOverdueSchedulerService {

    private final EmiInstallmentRepository emiInstallmentRepository;
    private final EmiPlanRepository emiPlanRepository;
    private final BookingRepository bookingRepository;

    private static final int DEFAULT_THRESHOLD = 3;

    // Runs every day at 9 AM
    @Scheduled(cron = "0 0 9 * * *")
    public void processOverdues() {
        LocalDate today = LocalDate.now();

        List<EmiInstallment> installments = emiInstallmentRepository.findByStatusIn(
                List.of(InstallmentStatus.PENDING, InstallmentStatus.PARTIALLY_PAID, InstallmentStatus.OVERDUE)
        );

        for (EmiInstallment installment : installments) {
            EmiPlan plan = installment.getEmiPlan();

            if (plan.getStatus() != EmiPlanStatus.ACTIVE) {
                continue;
            }

            LocalDate dueDate = installment.getDueDate();
            Integer graceDays = plan.getGraceDays() == null ? 0 : plan.getGraceDays();
            LocalDate effectiveDueDate = dueDate.plusDays(graceDays);

            BigDecimal currentPayable = installment.getRemainingDue() == null
                    ? BigDecimal.ZERO
                    : installment.getRemainingDue();

            if (currentPayable.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            if (today.isAfter(effectiveDueDate)) {
                long overdueDays = ChronoUnit.DAYS.between(effectiveDueDate, today);

                BigDecimal lateFeePerDay = safe(plan.getLateFeePerDay());
                BigDecimal maxLateFee = safe(plan.getMaxLateFee());

                BigDecimal recalculatedPenalty = lateFeePerDay
                        .multiply(BigDecimal.valueOf(overdueDays))
                        .setScale(2, RoundingMode.HALF_UP);

                if (maxLateFee.compareTo(BigDecimal.ZERO) > 0 &&
                        recalculatedPenalty.compareTo(maxLateFee) > 0) {
                    recalculatedPenalty = maxLateFee;
                }

                installment.setPenaltyAmount(recalculatedPenalty);
                installment.setRemainingDue(calculateCurrentPayable(installment));
                installment.setStatus(InstallmentStatus.OVERDUE);

                emiInstallmentRepository.save(installment);

                log.info("Installment marked overdue | installmentId={} | overdueDays={} | penalty={}",
                        installment.getId(), overdueDays, recalculatedPenalty);

                updatePlanDefaultStatusIfNeeded(plan);
            }
        }
    }

    private void updatePlanDefaultStatusIfNeeded(EmiPlan plan) {
        long overdueCount = emiInstallmentRepository.countByEmiPlanIdAndStatus(plan.getId(), InstallmentStatus.OVERDUE);

        if (overdueCount >= DEFAULT_THRESHOLD) {
            plan.setStatus(EmiPlanStatus.DEFAULTED);
            emiPlanRepository.save(plan);

            if (plan.getBooking() != null) {
                plan.getBooking().setStatus(BookingStatus.DEFAULTED);
                bookingRepository.save(plan.getBooking());
            }

            log.warn("EMI plan defaulted | planId={} | overdueInstallments={}",
                    plan.getId(), overdueCount);
        }
    }

    private BigDecimal calculateCurrentPayable(EmiInstallment installment) {
        BigDecimal penaltyRemaining = safe(installment.getPenaltyAmount())
                .subtract(safe(installment.getPenaltyPaid()))
                .max(BigDecimal.ZERO);

        BigDecimal interestRemaining = safe(installment.getInterestComponent())
                .subtract(safe(installment.getInterestPaid()))
                .max(BigDecimal.ZERO);

        BigDecimal principalRemaining = safe(installment.getPrincipalComponent())
                .subtract(safe(installment.getPrincipalPaid()))
                .max(BigDecimal.ZERO);

        return penaltyRemaining
                .add(interestRemaining)
                .add(principalRemaining)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}