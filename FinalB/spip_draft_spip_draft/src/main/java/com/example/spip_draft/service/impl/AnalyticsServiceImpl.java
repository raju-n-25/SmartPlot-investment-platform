package com.example.spip_draft.service.impl;

import com.example.spip_draft.dto.analytics.AdminAnalyticsSummary;
import com.example.spip_draft.dto.analytics.BuilderAnalyticsSummary;
import com.example.spip_draft.dto.analytics.HeatmapPointDto;
import com.example.spip_draft.dto.analytics.InvestorAnalyticsSummary;
import com.example.spip_draft.dto.analytics.ProjectAnalyticsSummary;
import com.example.spip_draft.entity.*;
import com.example.spip_draft.enums.BookingStatus;
import com.example.spip_draft.enums.InvestmentRecommendation;
import com.example.spip_draft.enums.PaymentStatus;
import com.example.spip_draft.repository.*;
import com.example.spip_draft.service.AnalyticsService;
import com.example.spip_draft.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final BuilderRepository builderRepository;
    private final InvestorRepository investorRepository;
    private final ProjectRepository projectRepository;
    private final PlotRepository plotRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final ProjectLegalDetailsRepository projectLegalDetailsRepository;
    private final EmiPaymentTransactionRepository emiPaymentTransactionRepository;

    private static final BigDecimal DEFAULT_APPRECIATION = new BigDecimal("8.0");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    @Override
    public double calculateROI(double investment, double returns) {
        if (investment <= 0) return 0;
        return ((returns - investment) / investment) * 100.0;
    }

    @Override
    public BigDecimal projectFutureValue(BigDecimal currentValue, BigDecimal annualAppreciationRate, int years) {
        if (currentValue == null) return BigDecimal.ZERO;
        if (annualAppreciationRate == null) annualAppreciationRate = DEFAULT_APPRECIATION;

        BigDecimal multiplier = BigDecimal.ONE.add(
                annualAppreciationRate.divide(HUNDRED, 6, RoundingMode.HALF_UP)
        );

        BigDecimal result = currentValue;
        for (int i = 0; i < years; i++) {
            result = result.multiply(multiplier);
        }

        return result.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public InvestorAnalyticsSummary getInvestorAnalyticsSummary(Long investorId, BigDecimal appreciationRate) {
        InvestorAnalyticsSummary summary = new InvestorAnalyticsSummary();

        List<Payment> payments = paymentRepository.findByBooking_Investor_Id(investorId);

        BigDecimal totalInvestment = BigDecimal.ZERO;
        BigDecimal currentPortfolioValue = BigDecimal.ZERO;
        BigDecimal totalArea = BigDecimal.ZERO;

        for (Payment payment : payments) {
            Booking booking = payment.getBooking();
            Plot plot = booking != null ? booking.getPlot() : null;

            if (payment.getPaymentStatus() == PaymentStatus.TOKEN_PAID
                    || payment.getPaymentStatus() == PaymentStatus.COMPLETED) {
                totalInvestment = totalInvestment.add(nvl(payment.getAmount()));
            }

            if (plot != null && booking != null &&
                    (booking.getStatus() == BookingStatus.CONFIRMED || booking.getStatus() == BookingStatus.EMI_ACTIVE)) {
                currentPortfolioValue = currentPortfolioValue.add(nvl(plot.getPrice()));
                totalArea = totalArea.add(BigDecimal.valueOf(plot.getArea() != null ? plot.getArea() : 0));
            }
        }

        List<EmiPaymentTransaction> emiTransactions =
                emiPaymentTransactionRepository.findByInvestorIdAndStatus(
                        investorId,
                        com.example.spip_draft.enums.EmiPaymentTransactionStatus.SUCCESS
                );

        for (EmiPaymentTransaction tx : emiTransactions) {
            totalInvestment = totalInvestment.add(nvl(tx.getAmount()));
        }

        BigDecimal projected3 = projectFutureValue(currentPortfolioValue, appreciationRate, 3);
        BigDecimal projected5 = projectFutureValue(currentPortfolioValue, appreciationRate, 5);

        BigDecimal avgPricePerSqft = totalArea.compareTo(BigDecimal.ZERO) > 0
                ? currentPortfolioValue.divide(totalArea, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        double roi = totalInvestment.compareTo(BigDecimal.ZERO) > 0
                ? projected5.subtract(totalInvestment)
                        .divide(totalInvestment, 6, RoundingMode.HALF_UP)
                        .multiply(HUNDRED)
                        .doubleValue()
                : 0.0;

        BigDecimal demandScore = estimateInvestorDemandScore(investorId);
        BigDecimal infraScore = new BigDecimal("70.00");

        summary.setTotalInvestment(totalInvestment);
        summary.setCurrentPortfolioValue(currentPortfolioValue);
        summary.setProjectedValue3Years(projected3);
        summary.setProjectedValue5Years(projected5);
        summary.setProjectedProfit3Years(projected3.subtract(totalInvestment).max(BigDecimal.ZERO));
        summary.setProjectedProfit5Years(projected5.subtract(totalInvestment).max(BigDecimal.ZERO));
        summary.setAveragePricePerSqft(avgPricePerSqft);
        summary.setRoiPercentage(roi);
        summary.setDemandScore(demandScore);
        summary.setInfrastructureScore(infraScore);
        summary.setRecommendation(recommendFromSignals(null, avgPricePerSqft, demandScore, infraScore));

        return summary;
    }

    @Override
    public BuilderAnalyticsSummary getBuilderAnalyticsSummary(Long builderId) {
        BuilderAnalyticsSummary summary = new BuilderAnalyticsSummary();

        List<Project> projects = projectRepository.findByBuilderId(builderId);
        List<Plot> plots = plotRepository.findByProject_Builder_Id(builderId);
        List<Payment> payments = paymentRepository.findByBooking_Plot_Project_Builder_Id(builderId);

        BigDecimal totalInventoryValue = BigDecimal.ZERO;
        BigDecimal totalArea = BigDecimal.ZERO;
        BigDecimal totalRevenue = BigDecimal.ZERO;

        long sold = 0;
        long blocked = 0;
        long available = 0;

        for (Plot plot : plots) {
            totalInventoryValue = totalInventoryValue.add(nvl(plot.getPrice()));
            totalArea = totalArea.add(BigDecimal.valueOf(plot.getArea() != null ? plot.getArea() : 0));

            if (Boolean.TRUE.equals(plot.getAvailable())) available++;
            if (plot.getBookingStatus() == BookingStatus.CONFIRMED) sold++;
            if (plot.getBookingStatus() == BookingStatus.TOKEN_BLOCKED) blocked++;
        }

        for (Payment payment : payments) {
            if (payment.getPaymentStatus() == PaymentStatus.COMPLETED
                    || payment.getPaymentStatus() == PaymentStatus.TOKEN_PAID) {
                totalRevenue = totalRevenue.add(nvl(payment.getAmount()));
            }
        }

        BigDecimal avgPricePerSqft = totalArea.compareTo(BigDecimal.ZERO) > 0
                ? totalInventoryValue.divide(totalArea, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long totalBookings = 0;
        for (Project project : projects) {
            totalBookings += bookingRepository.countByPlot_Project_Id(project.getId());
        }

        summary.setTotalProjects(projects.size());
        summary.setTotalPlots(plots.size());
        summary.setAvailablePlots(available);
        summary.setSoldPlots(sold);
        summary.setBlockedPlots(blocked);
        summary.setTotalInventoryValue(totalInventoryValue);
        summary.setTotalRevenue(totalRevenue);
        summary.setAveragePricePerSqft(avgPricePerSqft);
        summary.setTotalBookings(totalBookings);
        summary.setLast30DaysBookings(0); // can be refined later
        summary.setDemandScore(calculateBuilderDemandScore(projects));
        summary.setProjectPopularityScore(calculateBuilderPopularity(projects));

        return summary;
    }

    @Override
    public AdminAnalyticsSummary getAdminAnalyticsSummary() {
        AdminAnalyticsSummary summary = new AdminAnalyticsSummary();

        List<Plot> allPlots = plotRepository.findAll();
        List<Payment> allPayments = paymentRepository.findAll();

        BigDecimal inventoryValue = BigDecimal.ZERO;
        BigDecimal area = BigDecimal.ZERO;
        BigDecimal revenue = BigDecimal.ZERO;

        for (Plot plot : allPlots) {
            inventoryValue = inventoryValue.add(nvl(plot.getPrice()));
            area = area.add(BigDecimal.valueOf(plot.getArea() != null ? plot.getArea() : 0));
        }

        for (Payment payment : allPayments) {
            if (payment.getPaymentStatus() == PaymentStatus.COMPLETED) {
                revenue = revenue.add(nvl(payment.getAmount()));
            }
        }

        BigDecimal avgPricePerSqft = area.compareTo(BigDecimal.ZERO) > 0
                ? inventoryValue.divide(area, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        summary.setTotalProjects(projectRepository.count());
        summary.setTotalPlots(plotRepository.count());
        summary.setTotalBuilders(builderRepository.count());
        summary.setTotalInvestors(investorRepository.count());
        summary.setPlatformInventoryValue(inventoryValue);
        summary.setPlatformRevenue(revenue);
        summary.setAveragePricePerSqft(avgPricePerSqft);
        summary.setHighRiskProjects(projectLegalDetailsRepository.countByProjectRiskCategory(com.example.spip_draft.enums.RiskCategory.HIGH));
        summary.setMediumRiskProjects(projectLegalDetailsRepository.countByProjectRiskCategory(com.example.spip_draft.enums.RiskCategory.MEDIUM));
        summary.setLowRiskProjects(projectLegalDetailsRepository.countByProjectRiskCategory(com.example.spip_draft.enums.RiskCategory.LOW));
        summary.setPlatformDemandScore(new BigDecimal("72.50"));

        return summary;
    }

    @Override
    public ProjectAnalyticsSummary getProjectAnalyticsSummary(Long projectId, BigDecimal appreciationRate) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        List<Plot> plots = plotRepository.findByProjectId(projectId);

        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal totalArea = BigDecimal.ZERO;
        long available = 0;
        long sold = 0;
        long blocked = 0;

        for (Plot plot : plots) {
            totalValue = totalValue.add(nvl(plot.getPrice()));
            totalArea = totalArea.add(BigDecimal.valueOf(plot.getArea() != null ? plot.getArea() : 0));

            if (Boolean.TRUE.equals(plot.getAvailable())) available++;
            if (plot.getBookingStatus() == BookingStatus.CONFIRMED) sold++;
            if (plot.getBookingStatus() == BookingStatus.TOKEN_BLOCKED) blocked++;
        }

        BigDecimal avgPricePerSqft = totalArea.compareTo(BigDecimal.ZERO) > 0
                ? totalValue.divide(totalArea, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal demandScore = calculateProjectDemandScore(projectId);
        BigDecimal popularityScore = calculateProjectPopularityScore(projectId);
        BigDecimal infrastructureScore = new BigDecimal("68.00");

        ProjectLegalDetails legalDetails = projectLegalDetailsRepository.findByProject_Id(projectId).orElse(null);
        Integer riskScore = legalDetails != null ? legalDetails.getProjectRiskScore() : 50;
        String riskCategory = legalDetails != null && legalDetails.getProjectRiskCategory() != null
                ? legalDetails.getProjectRiskCategory().name()
                : "MEDIUM";

        ProjectAnalyticsSummary summary = new ProjectAnalyticsSummary();
        summary.setProjectId(project.getId());
        summary.setProjectName(project.getProjectName());
        summary.setTotalPlots(plots.size());
        summary.setAvailablePlots(available);
        summary.setSoldPlots(sold);
        summary.setBlockedPlots(blocked);
        summary.setTotalProjectValue(totalValue);
        summary.setAveragePricePerSqft(avgPricePerSqft);
        summary.setDemandScore(demandScore);
        summary.setPopularityScore(popularityScore);
        summary.setInfrastructureScore(infrastructureScore);
        summary.setLegalRiskScore(riskScore);
        summary.setLegalRiskCategory(riskCategory);
        summary.setRecommendation(recommendFromSignals(riskScore, avgPricePerSqft, demandScore, infrastructureScore));
        summary.setProjectedGrowth3Years(projectFutureValue(totalValue, appreciationRate, 3));
        summary.setProjectedGrowth5Years(projectFutureValue(totalValue, appreciationRate, 5));

        return summary;
    }

    @Override
    public List<HeatmapPointDto> getProjectHeatmap(Long projectId, BigDecimal appreciationRate) {
        List<Plot> plots = plotRepository.findByProjectId(projectId);
        List<HeatmapPointDto> result = new ArrayList<>();

        BigDecimal infraScore = new BigDecimal("68.00");

        for (Plot plot : plots) {
            HeatmapPointDto dto = new HeatmapPointDto();
            dto.setPlotId(plot.getId());
            dto.setPlotNumber(plot.getPlotNumber());
            dto.setProjectName(plot.getProject() != null ? plot.getProject().getProjectName() : null);
            dto.setFacing(plot.getFacing());
            dto.setCornerPlot(plot.getCornerPlot());

            BigDecimal area = BigDecimal.valueOf(plot.getArea() != null ? plot.getArea() : 0);
            BigDecimal ppsf = area.compareTo(BigDecimal.ZERO) > 0
                    ? nvl(plot.getPrice()).divide(area, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal demandScore = calculatePlotDemandScore(plot);
            Integer riskScore = 50;

            dto.setPricePerSqft(ppsf);
            dto.setDemandScore(demandScore);
            dto.setInfrastructureScore(infraScore);
            dto.setRiskScore(riskScore);
            dto.setRecommendation(recommendFromSignals(riskScore, ppsf, demandScore, infraScore).name());

            result.add(dto);
        }

        return result;
    }

    private BigDecimal calculateProjectDemandScore(Long projectId) {
        long totalBookings = bookingRepository.countByPlot_Project_Id(projectId);
        long confirmed = bookingRepository.countByPlot_Project_IdAndStatus(projectId, BookingStatus.CONFIRMED);
        return BigDecimal.valueOf((totalBookings * 5.0) + (confirmed * 10.0)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateProjectPopularityScore(Long projectId) {
        long totalBookings = bookingRepository.countByPlot_Project_Id(projectId);
        return BigDecimal.valueOf(Math.min(100, totalBookings * 4.0)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateBuilderDemandScore(List<Project> projects) {
        BigDecimal total = BigDecimal.ZERO;
        for (Project project : projects) {
            total = total.add(calculateProjectDemandScore(project.getId()));
        }
        return total;
    }

    private BigDecimal calculateBuilderPopularity(List<Project> projects) {
        BigDecimal total = BigDecimal.ZERO;
        for (Project project : projects) {
            total = total.add(calculateProjectPopularityScore(project.getId()));
        }
        return total;
    }

    private BigDecimal estimateInvestorDemandScore(Long investorId) {
        long confirmed = bookingRepository.countByInvestorIdAndStatusIn(
                investorId,
                java.util.List.of(BookingStatus.CONFIRMED, BookingStatus.EMI_ACTIVE)
        );
        return BigDecimal.valueOf(Math.min(100, confirmed * 12.5)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculatePlotDemandScore(Plot plot) {
        BookingStatus status = plot.getBookingStatus();
        if (status == BookingStatus.CONFIRMED) return new BigDecimal("90.00");
        if (status == BookingStatus.TOKEN_PAID) return new BigDecimal("75.00");
        if (status == BookingStatus.TOKEN_BLOCKED) return new BigDecimal("60.00");
        return new BigDecimal("35.00");
    }

    private InvestmentRecommendation recommendFromSignals(Integer legalRiskScore,
                                                          BigDecimal pricePerSqft,
                                                          BigDecimal demandScore,
                                                          BigDecimal infrastructureScore) {

        int risk = legalRiskScore != null ? legalRiskScore : 50;
        BigDecimal demand = demandScore != null ? demandScore : BigDecimal.ZERO;
        BigDecimal infra = infrastructureScore != null ? infrastructureScore : BigDecimal.ZERO;
        BigDecimal price = pricePerSqft != null ? pricePerSqft : BigDecimal.ZERO;

        if (risk <= 30 && demand.compareTo(new BigDecimal("60")) >= 0 && infra.compareTo(new BigDecimal("60")) >= 0) {
            return InvestmentRecommendation.RECOMMENDED;
        }

        if (risk >= 60 || price.compareTo(new BigDecimal("15000")) > 0) {
            return InvestmentRecommendation.RISKY;
        }

        return InvestmentRecommendation.MODERATE;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}