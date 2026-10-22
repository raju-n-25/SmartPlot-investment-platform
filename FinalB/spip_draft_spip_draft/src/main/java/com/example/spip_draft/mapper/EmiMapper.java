package com.example.spip_draft.mapper;

import com.example.spip_draft.dto.emi.EmiInstallmentResponse;
import com.example.spip_draft.dto.emi.EmiPlanResponse;
import com.example.spip_draft.entity.EmiInstallment;
import com.example.spip_draft.entity.EmiPlan;
import org.springframework.stereotype.Component;

@Component
public class EmiMapper {

    public EmiPlanResponse toPlanResponse(EmiPlan plan) {
        EmiPlanResponse response = new EmiPlanResponse();

        response.setEmiPlanId(plan.getId());
        response.setBookingId(plan.getBooking().getId());
        response.setInvestorId(plan.getInvestor().getId());
        response.setTenantId(plan.getTenantId());

        response.setTotalAmount(plan.getTotalAmount());
        response.setTokenPaid(plan.getTokenPaid());
        response.setDownPayment(plan.getDownPayment());
        response.setPrincipalAmount(plan.getPrincipalAmount());
        response.setAnnualInterestRate(plan.getAnnualInterestRate());
        response.setTenureMonths(plan.getTenureMonths());
        response.setMonthlyEmi(plan.getMonthlyEmi());
        response.setTotalInterest(plan.getTotalInterest());
        response.setTotalPayable(plan.getTotalPayable());
        response.setPaidInstallments(plan.getPaidInstallments());
        response.setTotalInstallments(plan.getTotalInstallments());
        response.setRemainingPrincipal(plan.getRemainingPrincipal());

        response.setGraceDays(plan.getGraceDays());
        response.setLateFeePerDay(plan.getLateFeePerDay());
        response.setMaxLateFee(plan.getMaxLateFee());

        response.setStartDate(plan.getStartDate());
        response.setFirstDueDate(plan.getFirstDueDate());
        response.setNextDueDate(plan.getNextDueDate());

        response.setStatus(plan.getStatus());
        response.setRemarks(plan.getRemarks());

        response.setCreatedAt(plan.getCreatedAt());
        response.setUpdatedAt(plan.getUpdatedAt());

        return response;
    }

    public EmiInstallmentResponse toInstallmentResponse(EmiInstallment installment) {
        EmiInstallmentResponse response = new EmiInstallmentResponse();

        response.setInstallmentId(installment.getId());
        response.setEmiPlanId(installment.getEmiPlan().getId());
        response.setInstallmentNumber(installment.getInstallmentNumber());

        response.setDueDate(installment.getDueDate());
        response.setOpeningPrincipal(installment.getOpeningPrincipal());
        response.setPrincipalComponent(installment.getPrincipalComponent());
        response.setInterestComponent(installment.getInterestComponent());
        response.setEmiAmount(installment.getEmiAmount());
        response.setPenaltyAmount(installment.getPenaltyAmount());

        response.setPrincipalPaid(installment.getPrincipalPaid());
        response.setInterestPaid(installment.getInterestPaid());
        response.setPenaltyPaid(installment.getPenaltyPaid());

        response.setPaidAmount(installment.getPaidAmount());
        response.setRemainingDue(installment.getRemainingDue());

        response.setPaidDate(installment.getPaidDate());
        response.setLastPaymentAt(installment.getLastPaymentAt());
        response.setStatus(installment.getStatus());
        response.setRemarks(installment.getRemarks());

        response.setCreatedAt(installment.getCreatedAt());
        response.setUpdatedAt(installment.getUpdatedAt());

        return response;
    }
}