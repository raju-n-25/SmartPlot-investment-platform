package com.example.spip_draft.service;

import com.example.spip_draft.entity.EmiInstallment;
import com.example.spip_draft.entity.EmiPlan;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class EmiCalculatorService {

    private static final int SCALE = 2;
    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);

    public BigDecimal calculateMonthlyEmi(BigDecimal principal, BigDecimal annualRatePercent, int tenureMonths) {

        if (principal == null || annualRatePercent == null || tenureMonths <= 0) {
            throw new IllegalArgumentException("Invalid EMI calculation input");
        }

        if (annualRatePercent.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(tenureMonths), SCALE, RoundingMode.HALF_UP);
        }

        BigDecimal monthlyRate = annualRatePercent
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);

        BigDecimal onePlusRPowerN = BigDecimal.ONE.add(monthlyRate).pow(tenureMonths, MC);

        BigDecimal numerator = principal.multiply(monthlyRate, MC).multiply(onePlusRPowerN, MC);
        BigDecimal denominator = onePlusRPowerN.subtract(BigDecimal.ONE, MC);

        return numerator.divide(denominator, SCALE, RoundingMode.HALF_UP);
    }

    public List<EmiInstallment> generateSchedule(EmiPlan emiPlan) {

        List<EmiInstallment> installments = new ArrayList<>();

        BigDecimal balance = emiPlan.getPrincipalAmount();
        BigDecimal monthlyRate = emiPlan.getAnnualInterestRate()
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);

        BigDecimal emiAmount = emiPlan.getMonthlyEmi();
        LocalDate dueDate = emiPlan.getFirstDueDate();

        for (int i = 1; i <= emiPlan.getTenureMonths(); i++) {

            EmiInstallment installment = new EmiInstallment();
            installment.setEmiPlan(emiPlan);
            installment.setInstallmentNumber(i);
            installment.setDueDate(dueDate);
            installment.setOpeningPrincipal(balance);

            BigDecimal interestComponent = balance.multiply(monthlyRate).setScale(SCALE, RoundingMode.HALF_UP);

            BigDecimal principalComponent;
            BigDecimal actualEmi;

            if (i == emiPlan.getTenureMonths()) {
                principalComponent = balance;
                actualEmi = principalComponent.add(interestComponent).setScale(SCALE, RoundingMode.HALF_UP);
            } else {
                principalComponent = emiAmount.subtract(interestComponent).setScale(SCALE, RoundingMode.HALF_UP);
                actualEmi = emiAmount;
            }

            installment.setInterestComponent(interestComponent);
            installment.setPrincipalComponent(principalComponent);
            installment.setEmiAmount(actualEmi);
            installment.setPenaltyAmount(BigDecimal.ZERO);
            installment.setPaidAmount(BigDecimal.ZERO);
            installment.setRemainingDue(actualEmi);

            balance = balance.subtract(principalComponent).setScale(SCALE, RoundingMode.HALF_UP);

            installments.add(installment);
            dueDate = dueDate.plusMonths(1);
        }

        return installments;
    }

    public BigDecimal calculateTotalInterest(List<EmiInstallment> installments) {
        return installments.stream()
                .map(EmiInstallment::getInterestComponent)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTotalPayable(BigDecimal downPayment, List<EmiInstallment> installments) {
        BigDecimal totalInstallmentAmount = installments.stream()
                .map(EmiInstallment::getEmiAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return downPayment.add(totalInstallmentAmount).setScale(SCALE, RoundingMode.HALF_UP);
    }
}