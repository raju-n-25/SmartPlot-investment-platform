package com.example.spip_draft.service;

import com.example.spip_draft.dto.emi.EmiPaymentTransactionResponse;
import com.example.spip_draft.dto.emi.ForeclosureResponse;
import com.example.spip_draft.dto.emi.PayEmiInstallmentRequest;
import com.example.spip_draft.entity.EmiInstallment;
import com.example.spip_draft.entity.EmiPaymentTransaction;
import com.example.spip_draft.entity.EmiPlan;
import com.example.spip_draft.entity.Investor;
import com.example.spip_draft.entity.Plot;
import com.example.spip_draft.enums.BookingStatus;
import com.example.spip_draft.enums.EmiPaymentMode;
import com.example.spip_draft.enums.EmiPaymentTransactionStatus;
import com.example.spip_draft.enums.EmiPlanStatus;
import com.example.spip_draft.enums.InstallmentStatus;
import com.example.spip_draft.exception.ResourceNotFoundException;
import com.example.spip_draft.payment.gateway.PaymentGateway;
import com.example.spip_draft.payment.gateway.PaymentGatewayFactory;
import com.example.spip_draft.repository.BookingRepository;
import com.example.spip_draft.repository.EmiInstallmentRepository;
import com.example.spip_draft.repository.EmiPaymentTransactionRepository;
import com.example.spip_draft.repository.EmiPlanRepository;
import com.example.spip_draft.repository.PlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EmiPaymentService {

    private final EmiInstallmentRepository emiInstallmentRepository;
    private final EmiPaymentTransactionRepository emiPaymentTransactionRepository;
    private final EmiPlanRepository emiPlanRepository;
    private final BookingRepository bookingRepository;
    private final PlotRepository plotRepository;
    private final InvestorService investorService;
    private final WalletAccountService walletAccountService;
    private final PaymentGatewayFactory gatewayFactory;

    public EmiPaymentTransactionResponse payInstallment(Long installmentId, PayEmiInstallmentRequest request) {

        Investor currentInvestor = investorService.getCurrentInvestor();

        EmiInstallment installment = emiInstallmentRepository.findByIdForUpdate(installmentId)
                .orElseThrow(() -> new ResourceNotFoundException("EMI installment not found"));

        EmiPlan plan = installment.getEmiPlan();

        // refresh overdue + penalty before processing payment
        refreshInstallmentOverdueState(installment);

        validateOwnership(plan, currentInvestor);
        validateInstallmentForPayment(installment, request);

        BigDecimal payableNow = calculateCurrentPayable(installment);
        BigDecimal requestedAmount = request.getAmount().setScale(2, RoundingMode.HALF_UP);

        if (requestedAmount.compareTo(payableNow) > 0) {
            throw new IllegalArgumentException("Amount exceeds current payable amount");
        }

        BigDecimal walletAmount = BigDecimal.ZERO;
        BigDecimal gatewayAmount = BigDecimal.ZERO;
        EmiPaymentMode paymentMode;

        if (request.isUseWallet()) {
            BigDecimal requestedWalletAmount = getSafeAmount(request.getWalletAmount())
                    .setScale(2, RoundingMode.HALF_UP);

            if (requestedWalletAmount.compareTo(BigDecimal.ZERO) > 0) {
                walletAmount = walletAccountService.getWalletContribution(
                        currentInvestor,
                        requestedWalletAmount.min(requestedAmount)
                );
            } else {
                walletAmount = walletAccountService.getWalletContribution(currentInvestor, requestedAmount);
            }

            gatewayAmount = requestedAmount.subtract(walletAmount);

            if (walletAmount.compareTo(BigDecimal.ZERO) > 0 && gatewayAmount.compareTo(BigDecimal.ZERO) > 0) {
                paymentMode = EmiPaymentMode.MIXED;
            } else if (walletAmount.compareTo(BigDecimal.ZERO) > 0) {
                paymentMode = EmiPaymentMode.WALLET;
            } else {
                paymentMode = EmiPaymentMode.GATEWAY;
            }
        } else {
            gatewayAmount = requestedAmount;
            paymentMode = EmiPaymentMode.GATEWAY;
        }

        if (gatewayAmount.compareTo(BigDecimal.ZERO) > 0
                && (request.getGatewayType() == null || request.getGatewayType().isBlank())) {
            throw new IllegalArgumentException("Gateway type is required when gateway amount is used");
        }

        EmiPaymentTransaction tx = new EmiPaymentTransaction();
        tx.setEmiPlan(plan);
        tx.setEmiInstallment(installment);
        tx.setInvestor(currentInvestor);
        tx.setTenantId(plan.getTenantId());
        tx.setAmount(requestedAmount);
        tx.setWalletAmount(walletAmount);
        tx.setGatewayAmount(gatewayAmount);
        tx.setPaymentMode(paymentMode);
        tx.setStatus(EmiPaymentTransactionStatus.INITIATED);
        tx.setGatewayType(request.getGatewayType());
        tx.setRemarks(request.getRemarks());

        EmiPaymentTransaction savedTx = emiPaymentTransactionRepository.save(tx);

        try {
            if (walletAmount.compareTo(BigDecimal.ZERO) > 0) {
                walletAccountService.debitWallet(
                        currentInvestor,
                        walletAmount,
                        "Wallet used for EMI installment " + installment.getInstallmentNumber() +
                                " of plan " + plan.getId(),
                        installment.getId()
                );
            }

            String gatewayReference = null;
            if (gatewayAmount.compareTo(BigDecimal.ZERO) > 0) {
                PaymentGateway gateway = gatewayFactory.getGateway(request.getGatewayType());
                gatewayReference = gateway.processPayment(gatewayAmount);
            }

            savedTx.setGatewayReference(gatewayReference);
            savedTx.setStatus(EmiPaymentTransactionStatus.SUCCESS);
            savedTx.setPaidAt(LocalDateTime.now());

            allocatePayment(installment, requestedAmount);
            updatePlanAfterInstallmentPayment(plan);
            updateBookingIfPlanCompleted(plan);

            emiInstallmentRepository.save(installment);
            emiPlanRepository.save(plan);
            emiPaymentTransactionRepository.save(savedTx);

            return mapTransactionResponse(savedTx);

        } catch (Exception ex) {
            savedTx.setStatus(EmiPaymentTransactionStatus.FAILED);

            String failureMessage = "Payment failed: " + ex.getMessage();
            if (savedTx.getRemarks() == null || savedTx.getRemarks().isBlank()) {
                savedTx.setRemarks(failureMessage);
            } else {
                savedTx.setRemarks(savedTx.getRemarks() + " | " + failureMessage);
            }

            emiPaymentTransactionRepository.save(savedTx);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<EmiPaymentTransactionResponse> getInstallmentTransactions(Long installmentId) {
        Investor currentInvestor = investorService.getCurrentInvestor();

        EmiInstallment installment = emiInstallmentRepository.findById(installmentId)
                .orElseThrow(() -> new ResourceNotFoundException("EMI installment not found"));

        validateOwnership(installment.getEmiPlan(), currentInvestor);

        return emiPaymentTransactionRepository.findByEmiInstallmentIdOrderByCreatedAtAsc(installmentId)
                .stream()
                .map(this::mapTransactionResponse)
                .toList();
    }

    @Transactional
    public String recoverDefaultedPlan(Long emiPlanId) {
        Investor currentInvestor = investorService.getCurrentInvestor();

        EmiPlan plan = emiPlanRepository.findById(emiPlanId)
                .orElseThrow(() -> new ResourceNotFoundException("EMI plan not found"));

        validateOwnership(plan, currentInvestor);

        if (plan.getStatus() != EmiPlanStatus.DEFAULTED) {
            throw new IllegalArgumentException("Only defaulted plans can be recovered");
        }

        List<EmiInstallment> overdueInstallments =
                emiInstallmentRepository.findByEmiPlanIdAndStatus(emiPlanId, InstallmentStatus.OVERDUE);

        boolean hasUnclearedOverdues = overdueInstallments.stream()
                .anyMatch(i -> calculateCurrentPayable(i).compareTo(BigDecimal.ZERO) > 0);

        if (hasUnclearedOverdues) {
            throw new IllegalArgumentException("Cannot recover plan until all overdue installments are cleared");
        }

        for (EmiInstallment installment : overdueInstallments) {
            if (calculateCurrentPayable(installment).compareTo(BigDecimal.ZERO) <= 0) {
                installment.setStatus(InstallmentStatus.PAID);
                if (installment.getPaidDate() == null) {
                    installment.setPaidDate(LocalDate.now());
                }
                emiInstallmentRepository.save(installment);
            }
        }

        plan.setStatus(EmiPlanStatus.ACTIVE);

        List<EmiInstallment> installments =
                emiInstallmentRepository.findByEmiPlanIdOrderByInstallmentNumberAsc(emiPlanId);

        installments.stream()
                .filter(i -> i.getStatus() != InstallmentStatus.PAID)
                .findFirst()
                .ifPresent(next -> plan.setNextDueDate(next.getDueDate()));

        emiPlanRepository.save(plan);

        if (plan.getBooking() != null && plan.getBooking().getStatus() == BookingStatus.DEFAULTED) {
            plan.getBooking().setStatus(BookingStatus.EMI_ACTIVE);
            bookingRepository.save(plan.getBooking());
        }

        return "EMI plan recovered successfully";
    }

    @Transactional(readOnly = true)
    public ForeclosureResponse getForeclosureAmount(Long emiPlanId) {
        Investor currentInvestor = investorService.getCurrentInvestor();

        EmiPlan plan = emiPlanRepository.findById(emiPlanId)
                .orElseThrow(() -> new ResourceNotFoundException("EMI plan not found"));

        validateOwnership(plan, currentInvestor);

        if (plan.getStatus() != EmiPlanStatus.ACTIVE && plan.getStatus() != EmiPlanStatus.DEFAULTED) {
            throw new IllegalArgumentException("Foreclosure is allowed only for active or defaulted plans");
        }

        BigDecimal remainingPrincipal = calculateRemainingPrincipal(plan);
        BigDecimal unpaidPenalty = calculateUnpaidPenalty(plan);
        BigDecimal foreclosureFee = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal total = remainingPrincipal
                .add(unpaidPenalty)
                .add(foreclosureFee)
                .setScale(2, RoundingMode.HALF_UP);

        ForeclosureResponse response = new ForeclosureResponse();
        response.setEmiPlanId(plan.getId());
        response.setRemainingPrincipal(remainingPrincipal);
        response.setUnpaidPenalty(unpaidPenalty);
        response.setForeclosureFee(foreclosureFee);
        response.setTotalForeclosureAmount(total);
        response.setMessage("Foreclosure amount calculated successfully");

        return response;
    }

    @Transactional
    public String foreclosePlan(Long emiPlanId, boolean useWallet, String gatewayType) {
        Investor currentInvestor = investorService.getCurrentInvestor();

        EmiPlan plan = emiPlanRepository.findById(emiPlanId)
                .orElseThrow(() -> new ResourceNotFoundException("EMI plan not found"));

        validateOwnership(plan, currentInvestor);

        if (plan.getStatus() != EmiPlanStatus.ACTIVE && plan.getStatus() != EmiPlanStatus.DEFAULTED) {
            throw new IllegalArgumentException("Foreclosure is allowed only for active or defaulted plans");
        }

        ForeclosureResponse foreclosure = getForeclosureAmount(emiPlanId);
        BigDecimal totalAmount = foreclosure.getTotalForeclosureAmount();

        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Nothing left to foreclose");
        }

        BigDecimal walletUsed = BigDecimal.ZERO;
        BigDecimal gatewayAmount = totalAmount;

        if (useWallet) {
            walletUsed = walletAccountService.getWalletContribution(currentInvestor, totalAmount);
            gatewayAmount = totalAmount.subtract(walletUsed);

            if (walletUsed.compareTo(BigDecimal.ZERO) > 0) {
                walletAccountService.debitWallet(
                        currentInvestor,
                        walletUsed,
                        "Wallet used for foreclosure of EMI plan " + plan.getId(),
                        plan.getId()
                );
            }
        }

        if (gatewayAmount.compareTo(BigDecimal.ZERO) > 0) {
            if (gatewayType == null || gatewayType.isBlank()) {
                throw new IllegalArgumentException("Gateway type is required when wallet is not enough");
            }

            PaymentGateway gateway = gatewayFactory.getGateway(gatewayType);
            gateway.processPayment(gatewayAmount);
        }

        List<EmiInstallment> installments =
                emiInstallmentRepository.findByEmiPlanIdOrderByInstallmentNumberAsc(plan.getId());

        for (EmiInstallment installment : installments) {
            BigDecimal principalRemaining = getSafeAmount(installment.getPrincipalComponent())
                    .subtract(getSafeAmount(installment.getPrincipalPaid()))
                    .max(BigDecimal.ZERO);

            BigDecimal interestRemaining = getSafeAmount(installment.getInterestComponent())
                    .subtract(getSafeAmount(installment.getInterestPaid()))
                    .max(BigDecimal.ZERO);

            BigDecimal penaltyRemaining = getSafeAmount(installment.getPenaltyAmount())
                    .subtract(getSafeAmount(installment.getPenaltyPaid()))
                    .max(BigDecimal.ZERO);

            installment.setPrincipalPaid(getSafeAmount(installment.getPrincipalPaid()).add(principalRemaining));
            installment.setInterestPaid(getSafeAmount(installment.getInterestPaid()).add(interestRemaining));
            installment.setPenaltyPaid(getSafeAmount(installment.getPenaltyPaid()).add(penaltyRemaining));

            installment.setPaidAmount(
                    getSafeAmount(installment.getPrincipalPaid())
                            .add(getSafeAmount(installment.getInterestPaid()))
                            .add(getSafeAmount(installment.getPenaltyPaid()))
                            .setScale(2, RoundingMode.HALF_UP)
            );

            installment.setRemainingDue(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            installment.setStatus(InstallmentStatus.PAID);
            installment.setPaidDate(LocalDate.now());
            installment.setLastPaymentAt(LocalDateTime.now());

            emiInstallmentRepository.save(installment);
        }

        plan.setRemainingPrincipal(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        plan.setPaidInstallments(plan.getTotalInstallments());
        plan.setNextDueDate(null);
        plan.setStatus(EmiPlanStatus.FORECLOSED);
        emiPlanRepository.save(plan);

        if (plan.getBooking() != null) {
            plan.getBooking().setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(plan.getBooking());

            Plot plot = plan.getBooking().getPlot();
            if (plot != null) {
                plot.setAvailable(false);
                plot.setBookingStatus(BookingStatus.CONFIRMED);
                plot.setBlockedAt(null);
                plotRepository.save(plot);
            }
        }

        return "EMI plan foreclosed successfully";
    }

    public void refreshOverdueStatusForPlan(Long emiPlanId) {
        List<EmiInstallment> installments =
                emiInstallmentRepository.findByEmiPlanIdOrderByInstallmentNumberAsc(emiPlanId);

        for (EmiInstallment installment : installments) {
            refreshInstallmentOverdueState(installment);
        }
    }

    public void refreshInstallmentOverdueState(EmiInstallment installment) {

        if (installment == null || installment.getEmiPlan() == null) {
            return;
        }

        if (installment.getStatus() == InstallmentStatus.PAID
                || installment.getStatus() == InstallmentStatus.WAIVED) {
            return;
        }

        EmiPlan plan = installment.getEmiPlan();
        LocalDate today = LocalDate.now();

        int graceDays = plan.getGraceDays() == null ? 0 : plan.getGraceDays();
        LocalDate lastValidDate = installment.getDueDate().plusDays(graceDays);

        BigDecimal interestRemaining = getSafeAmount(installment.getInterestComponent())
                .subtract(getSafeAmount(installment.getInterestPaid()))
                .max(BigDecimal.ZERO);

        BigDecimal principalRemaining = getSafeAmount(installment.getPrincipalComponent())
                .subtract(getSafeAmount(installment.getPrincipalPaid()))
                .max(BigDecimal.ZERO);

        BigDecimal baseRemaining = interestRemaining
                .add(principalRemaining)
                .setScale(2, RoundingMode.HALF_UP);

        if (today.isAfter(lastValidDate) && baseRemaining.compareTo(BigDecimal.ZERO) > 0) {

            long daysLate = ChronoUnit.DAYS.between(lastValidDate, today);

            BigDecimal lateFeePerDay = getSafeAmount(plan.getLateFeePerDay()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal maxLateFee = getSafeAmount(plan.getMaxLateFee()).setScale(2, RoundingMode.HALF_UP);

            BigDecimal penalty = lateFeePerDay.multiply(BigDecimal.valueOf(daysLate))
                    .setScale(2, RoundingMode.HALF_UP);

            if (penalty.compareTo(maxLateFee) > 0) {
                penalty = maxLateFee;
            }

            installment.setPenaltyAmount(penalty);
            installment.setRemainingDue(calculateCurrentPayable(installment));
            installment.setStatus(InstallmentStatus.OVERDUE);

        } else {
            installment.setPenaltyAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            installment.setRemainingDue(calculateCurrentPayable(installment));

            if (installment.getRemainingDue().compareTo(BigDecimal.ZERO) <= 0) {
                installment.setStatus(InstallmentStatus.PAID);
                if (installment.getPaidDate() == null) {
                    installment.setPaidDate(LocalDate.now());
                }
                installment.setRemainingDue(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            } else if (getSafeAmount(installment.getPaidAmount()).compareTo(BigDecimal.ZERO) > 0) {
                installment.setStatus(InstallmentStatus.PARTIALLY_PAID);
            } else {
                installment.setStatus(InstallmentStatus.PENDING);
            }
        }

        emiInstallmentRepository.save(installment);
    }

    private void validateOwnership(EmiPlan plan, Investor currentInvestor) {
        if (!plan.getInvestor().getId().equals(currentInvestor.getId())) {
            throw new IllegalArgumentException("You do not own this EMI plan");
        }
    }

    private void validateInstallmentForPayment(EmiInstallment installment, PayEmiInstallmentRequest request) {
        if (installment.getStatus() == InstallmentStatus.PAID) {
            throw new IllegalArgumentException("Installment already paid");
        }

        if (installment.getEmiPlan().getStatus() != EmiPlanStatus.ACTIVE) {
            throw new IllegalArgumentException("EMI plan is not active");
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid payment amount");
        }

        BigDecimal payableNow = calculateCurrentPayable(installment);

        if (request.getAmount().compareTo(payableNow) > 0) {
            throw new IllegalArgumentException("Amount exceeds payable amount");
        }

        if (request.getWalletAmount() != null && request.getWalletAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Wallet amount cannot be negative");
        }
    }

    private BigDecimal calculateCurrentPayable(EmiInstallment installment) {
        BigDecimal penaltyRemaining = getSafeAmount(installment.getPenaltyAmount())
                .subtract(getSafeAmount(installment.getPenaltyPaid()))
                .max(BigDecimal.ZERO);

        BigDecimal interestRemaining = getSafeAmount(installment.getInterestComponent())
                .subtract(getSafeAmount(installment.getInterestPaid()))
                .max(BigDecimal.ZERO);

        BigDecimal principalRemaining = getSafeAmount(installment.getPrincipalComponent())
                .subtract(getSafeAmount(installment.getPrincipalPaid()))
                .max(BigDecimal.ZERO);

        return penaltyRemaining
                .add(interestRemaining)
                .add(principalRemaining)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void allocatePayment(EmiInstallment installment, BigDecimal amountPaid) {
        BigDecimal remainingPayment = amountPaid.setScale(2, RoundingMode.HALF_UP);

        BigDecimal penaltyRemaining = getSafeAmount(installment.getPenaltyAmount())
                .subtract(getSafeAmount(installment.getPenaltyPaid()))
                .max(BigDecimal.ZERO);

        if (remainingPayment.compareTo(BigDecimal.ZERO) > 0 && penaltyRemaining.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal penaltyAllocation = remainingPayment.min(penaltyRemaining);
            installment.setPenaltyPaid(getSafeAmount(installment.getPenaltyPaid()).add(penaltyAllocation));
            remainingPayment = remainingPayment.subtract(penaltyAllocation);
        }

        BigDecimal interestRemaining = getSafeAmount(installment.getInterestComponent())
                .subtract(getSafeAmount(installment.getInterestPaid()))
                .max(BigDecimal.ZERO);

        if (remainingPayment.compareTo(BigDecimal.ZERO) > 0 && interestRemaining.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal interestAllocation = remainingPayment.min(interestRemaining);
            installment.setInterestPaid(getSafeAmount(installment.getInterestPaid()).add(interestAllocation));
            remainingPayment = remainingPayment.subtract(interestAllocation);
        }

        BigDecimal principalRemaining = getSafeAmount(installment.getPrincipalComponent())
                .subtract(getSafeAmount(installment.getPrincipalPaid()))
                .max(BigDecimal.ZERO);

        if (remainingPayment.compareTo(BigDecimal.ZERO) > 0 && principalRemaining.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal principalAllocation = remainingPayment.min(principalRemaining);
            installment.setPrincipalPaid(getSafeAmount(installment.getPrincipalPaid()).add(principalAllocation));
            remainingPayment = remainingPayment.subtract(principalAllocation);
        }

        BigDecimal totalPaid = getSafeAmount(installment.getPenaltyPaid())
                .add(getSafeAmount(installment.getInterestPaid()))
                .add(getSafeAmount(installment.getPrincipalPaid()))
                .setScale(2, RoundingMode.HALF_UP);

        installment.setPaidAmount(totalPaid);
        installment.setRemainingDue(calculateCurrentPayable(installment));
        installment.setLastPaymentAt(LocalDateTime.now());

        if (installment.getRemainingDue().compareTo(BigDecimal.ZERO) <= 0) {
            installment.setStatus(InstallmentStatus.PAID);
            installment.setPaidDate(LocalDate.now());
            installment.setRemainingDue(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        } else {
            installment.setStatus(InstallmentStatus.PARTIALLY_PAID);
        }
    }

    private void updatePlanAfterInstallmentPayment(EmiPlan plan) {
        List<EmiInstallment> installments =
                emiInstallmentRepository.findByEmiPlanIdOrderByInstallmentNumberAsc(plan.getId());

        long paidCount = installments.stream()
                .filter(i -> i.getStatus() == InstallmentStatus.PAID)
                .count();

        plan.setPaidInstallments((int) paidCount);

        installments.stream()
                .filter(i -> i.getStatus() != InstallmentStatus.PAID)
                .findFirst()
                .ifPresentOrElse(
                        next -> plan.setNextDueDate(next.getDueDate()),
                        () -> {
                            plan.setNextDueDate(null);
                            plan.setStatus(EmiPlanStatus.COMPLETED);
                        }
                );

        BigDecimal remainingPrincipal = installments.stream()
                .map(i -> getSafeAmount(i.getPrincipalComponent())
                        .subtract(getSafeAmount(i.getPrincipalPaid()))
                        .max(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        plan.setRemainingPrincipal(remainingPrincipal);
    }

    private void updateBookingIfPlanCompleted(EmiPlan plan) {
        if (plan.getStatus() == EmiPlanStatus.COMPLETED) {
            plan.getBooking().setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(plan.getBooking());

            Plot plot = plan.getBooking().getPlot();
            if (plot != null) {
                plot.setAvailable(false);
                plot.setBookingStatus(BookingStatus.CONFIRMED);
                plot.setBlockedAt(null);
                plotRepository.save(plot);
            }
        }
    }

    private BigDecimal calculateRemainingPrincipal(EmiPlan plan) {
        List<EmiInstallment> installments =
                emiInstallmentRepository.findByEmiPlanIdOrderByInstallmentNumberAsc(plan.getId());

        return installments.stream()
                .map(i -> getSafeAmount(i.getPrincipalComponent())
                        .subtract(getSafeAmount(i.getPrincipalPaid()))
                        .max(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateUnpaidPenalty(EmiPlan plan) {
        List<EmiInstallment> installments =
                emiInstallmentRepository.findByEmiPlanIdOrderByInstallmentNumberAsc(plan.getId());

        return installments.stream()
                .map(i -> getSafeAmount(i.getPenaltyAmount())
                        .subtract(getSafeAmount(i.getPenaltyPaid()))
                        .max(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private EmiPaymentTransactionResponse mapTransactionResponse(EmiPaymentTransaction tx) {
        EmiPaymentTransactionResponse response = new EmiPaymentTransactionResponse();

        response.setTransactionId(tx.getId());
        response.setEmiPlanId(tx.getEmiPlan().getId());
        response.setEmiInstallmentId(tx.getEmiInstallment().getId());
        response.setInvestorId(tx.getInvestor().getId());
        response.setTenantId(tx.getTenantId());

        response.setAmount(tx.getAmount());
        response.setWalletAmount(tx.getWalletAmount());
        response.setGatewayAmount(tx.getGatewayAmount());

        response.setPaymentMode(tx.getPaymentMode());
        response.setStatus(tx.getStatus());

        response.setGatewayType(tx.getGatewayType());
        response.setGatewayReference(tx.getGatewayReference());
        response.setRemarks(tx.getRemarks());
        response.setPaidAt(tx.getPaidAt());

        response.setCreatedAt(tx.getCreatedAt());
        response.setUpdatedAt(tx.getUpdatedAt());

        return response;
    }

    private BigDecimal getSafeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}