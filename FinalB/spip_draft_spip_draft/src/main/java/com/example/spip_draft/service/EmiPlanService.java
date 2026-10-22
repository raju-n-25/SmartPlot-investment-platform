package com.example.spip_draft.service;

import com.example.spip_draft.dto.emi.CreateEmiPlanRequest;
import com.example.spip_draft.dto.emi.EmiInstallmentResponse;
import com.example.spip_draft.dto.emi.EmiPlanResponse;
import com.example.spip_draft.entity.Booking;
import com.example.spip_draft.entity.EmiInstallment;
import com.example.spip_draft.entity.EmiPlan;
import com.example.spip_draft.entity.Investor;
import com.example.spip_draft.entity.Plot;
import com.example.spip_draft.enums.BookingStatus;
import com.example.spip_draft.enums.EmiPlanStatus;
import com.example.spip_draft.exception.ResourceNotFoundException;
import com.example.spip_draft.mapper.EmiMapper;
import com.example.spip_draft.repository.BookingRepository;
import com.example.spip_draft.repository.EmiInstallmentRepository;
import com.example.spip_draft.repository.EmiPlanRepository;
import com.example.spip_draft.repository.PlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EmiPlanService {

    private final EmiPlanRepository emiPlanRepository;
    private final EmiInstallmentRepository emiInstallmentRepository;
    private final BookingRepository bookingRepository;
    private final PlotRepository plotRepository;
    private final InvestorService investorService;
    private final EmiCalculatorService emiCalculatorService;
    private final EmiMapper emiMapper;
    private final EmiPaymentService emiPaymentService;

    public EmiPlanResponse createPlan(Long bookingId, CreateEmiPlanRequest request) {

        Investor currentInvestor = investorService.getCurrentInvestor();

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        validateBookingForEmi(booking, currentInvestor);
        validateCreatePlanRequest(booking, request);

        BigDecimal totalAmount = booking.getPlot().getPrice();
        BigDecimal tokenPaid = getSafeAmount(booking.getAmount());
        BigDecimal downPayment = getSafeAmount(request.getDownPayment());

        BigDecimal principalAmount = totalAmount.subtract(tokenPaid).subtract(downPayment);

        if (principalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Down payment is too high for this booking");
        }

        BigDecimal monthlyEmi = emiCalculatorService.calculateMonthlyEmi(
                principalAmount,
                request.getAnnualInterestRate(),
                request.getTenureMonths()
        );

        EmiPlan plan = new EmiPlan();
        plan.setBooking(booking);
        plan.setInvestor(currentInvestor);
        plan.setTenantId(booking.getTenantId());

        plan.setTotalAmount(totalAmount);
        plan.setTokenPaid(tokenPaid);
        plan.setDownPayment(downPayment);
        plan.setPrincipalAmount(principalAmount);
        plan.setAnnualInterestRate(request.getAnnualInterestRate());
        plan.setTenureMonths(request.getTenureMonths());
        plan.setMonthlyEmi(monthlyEmi);

        plan.setPaidInstallments(0);
        plan.setTotalInstallments(request.getTenureMonths());
        plan.setRemainingPrincipal(principalAmount);

        plan.setGraceDays(request.getGraceDays() == null ? 5 : request.getGraceDays());
        plan.setLateFeePerDay(request.getLateFeePerDay() == null ? BigDecimal.ZERO : request.getLateFeePerDay());
        plan.setMaxLateFee(request.getMaxLateFee() == null ? BigDecimal.ZERO : request.getMaxLateFee());

        plan.setStartDate(LocalDate.now());
        plan.setFirstDueDate(request.getFirstDueDate());
        plan.setNextDueDate(request.getFirstDueDate());

        plan.setStatus(EmiPlanStatus.ACTIVE);
        plan.setRemarks(request.getRemarks());

        plan.setTotalInterest(BigDecimal.ZERO);
        plan.setTotalPayable(BigDecimal.ZERO);

        EmiPlan savedPlan = emiPlanRepository.save(plan);

        List<EmiInstallment> installments = emiCalculatorService.generateSchedule(savedPlan);
        emiInstallmentRepository.saveAll(installments);

        BigDecimal totalInterest = emiCalculatorService.calculateTotalInterest(installments);
        BigDecimal totalPayable = emiCalculatorService.calculateTotalPayable(tokenPaid.add(downPayment), installments);

        savedPlan.setTotalInterest(totalInterest);
        savedPlan.setTotalPayable(totalPayable);
        savedPlan.setRemainingPrincipal(principalAmount);

        booking.setStatus(BookingStatus.EMI_ACTIVE);
        bookingRepository.save(booking);

        Plot plot = booking.getPlot();
        plot.setAvailable(false);
        plot.setBookingStatus(BookingStatus.EMI_ACTIVE);
        plot.setBlockedAt(null);
        plotRepository.save(plot);

        emiPlanRepository.save(savedPlan);

        return emiMapper.toPlanResponse(savedPlan);
    }

    @Transactional(readOnly = true)
    public EmiPlanResponse getPlanByBookingId(Long bookingId) {
        EmiPlan plan = emiPlanRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("EMI plan not found for booking id: " + bookingId));

        validateOwnership(plan);
        return emiMapper.toPlanResponse(plan);
    }

    @Transactional(readOnly = false)
    public List<EmiInstallmentResponse> getPlanSchedule(Long emiPlanId) {
        EmiPlan plan = emiPlanRepository.findById(emiPlanId)
                .orElseThrow(() -> new ResourceNotFoundException("EMI plan not found"));

        validateOwnership(plan);

        emiPaymentService.refreshOverdueStatusForPlan(emiPlanId);

        return emiInstallmentRepository.findByEmiPlanIdOrderByInstallmentNumberAsc(emiPlanId)
                .stream()
                .map(emiMapper::toInstallmentResponse)
                .toList();
    }

    private void validateBookingForEmi(Booking booking, Investor currentInvestor) {
        if (booking.getInvestor() == null || !booking.getInvestor().getId().equals(currentInvestor.getId())) {
            throw new IllegalArgumentException("You do not own this booking");
        }

        if (booking.getStatus() != BookingStatus.BUILDER_APPROVED) {
            throw new IllegalArgumentException("EMI plan allowed only after builder approval");
        }

        if (emiPlanRepository.existsByBookingId(booking.getId())) {
            throw new IllegalArgumentException("EMI plan already exists for this booking");
        }

        if (booking.getPlot() == null || booking.getPlot().getPrice() == null) {
            throw new IllegalArgumentException("Booking plot price is missing");
        }
    }

    private void validateCreatePlanRequest(Booking booking, CreateEmiPlanRequest request) {
        if (request.getAnnualInterestRate().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Interest rate cannot be negative");
        }

        validateAllowedTenure(request.getTenureMonths());

        if (request.getFirstDueDate() == null || request.getFirstDueDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("First due date must be today or future");
        }

        BigDecimal totalAmount = booking.getPlot().getPrice();
        BigDecimal tokenPaid = getSafeAmount(booking.getAmount());
        BigDecimal downPayment = getSafeAmount(request.getDownPayment());

        if (downPayment.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Down payment cannot be negative");
        }

        if (tokenPaid.add(downPayment).compareTo(totalAmount) >= 0) {
            throw new IllegalArgumentException("Token + down payment must be less than total amount");
        }

        if (request.getGraceDays() != null && request.getGraceDays() < 0) {
            throw new IllegalArgumentException("Grace days cannot be negative");
        }

        if (request.getLateFeePerDay() != null && request.getLateFeePerDay().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Late fee per day cannot be negative");
        }

        if (request.getMaxLateFee() != null && request.getMaxLateFee().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Max late fee cannot be negative");
        }
    }

    private void validateAllowedTenure(Integer tenureMonths) {
        List<Integer> allowedTenures = List.of(6, 12, 24, 36, 48, 60);

        if (!allowedTenures.contains(tenureMonths)) {
            throw new IllegalArgumentException("Allowed tenure months are 6, 12, 24, 36, 48, 60 only");
        }
    }

    private void validateOwnership(EmiPlan plan) {
        Investor currentInvestor = investorService.getCurrentInvestor();

        if (!plan.getInvestor().getId().equals(currentInvestor.getId())) {
            throw new IllegalArgumentException("You are not allowed to access this EMI plan");
        }
    }

    private BigDecimal getSafeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}