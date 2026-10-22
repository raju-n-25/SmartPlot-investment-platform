package com.example.spip_draft.service;

import com.example.spip_draft.dto.analytics.InvestorAnalyticsSummary;
import com.example.spip_draft.dto.dashboard.AdminConflictLogResponse;
import com.example.spip_draft.dto.dashboard.AdminDashboardResponse;
import com.example.spip_draft.dto.dashboard.BuilderDashboardResponse;
import com.example.spip_draft.dto.dashboard.EscrowDetailResponse;
import com.example.spip_draft.dto.dashboard.InvestorDashboardResponse;
import com.example.spip_draft.dto.dashboard.MeetingWidgetResponse;
import com.example.spip_draft.dto.dashboard.PaymentWidgetResponse;
import com.example.spip_draft.dto.dashboard.RecentActivityDTO;
import com.example.spip_draft.entity.*;
import com.example.spip_draft.enums.*;
import com.example.spip_draft.exception.ResourceNotFoundException;
import com.example.spip_draft.repository.*;
import com.example.spip_draft.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final BuilderRepository builderRepository;
    private final InvestorRepository investorRepository;
    private final ProjectRepository projectRepository;
    private final PlotRepository plotRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final BookingConflictRepository bookingConflictRepository;
    private final ProjectLegalDetailsRepository projectLegalDetailsRepository;
    private final EmiPaymentTransactionRepository emiPaymentTransactionRepository;
    private final MeetingRequestRepository meetingRequestRepository;
    private final InvestorAnalyticsService investorAnalyticsService;
    private final UserDocumentRepository userDocumentRepository;
    private final EscrowRuleEngineService escrowRuleEngineService;

    public AdminDashboardResponse getAdminDashboard() {
        AdminDashboardResponse response = new AdminDashboardResponse();

        // =========================
        // BUILDER-LEVEL KYC SUMMARY
        // =========================
        response.setTotalBuilders(builderRepository.count());
        response.setPendingKycApprovals(builderRepository.countByKycStatus(KycStatus.PENDING));
        response.setApprovedBuilders(builderRepository.countByKycStatus(KycStatus.APPROVED));
        response.setRejectedBuilders(builderRepository.countByKycStatus(KycStatus.REJECTED));

        // =========================
        // PLATFORM / PROJECT SUMMARY
        // =========================
        response.setTotalProjects(projectRepository.count());
        response.setBookingConflictLogs(bookingConflictRepository.count());
        response.setHighRiskProjects(
                projectLegalDetailsRepository.countByProjectRiskCategory(RiskCategory.HIGH)
        );

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal escrowHeld = BigDecimal.ZERO;
        BigDecimal escrowReleased = BigDecimal.ZERO;

        for (Payment payment : paymentRepository.findAll()) {
            if (payment.getPaymentStatus() == PaymentStatus.COMPLETED) {
                totalRevenue = totalRevenue.add(nvl(payment.getAmount()));
            }

            if (payment.getPaymentStatus() == PaymentStatus.TOKEN_PAID && !payment.isEscrowReleased()) {
                escrowHeld = escrowHeld.add(nvl(payment.getAmount()));
            }

            if (payment.isEscrowReleased()) {
                escrowReleased = escrowReleased.add(nvl(payment.getAmount()));
            }
        }

        response.setTotalRevenue(totalRevenue);
        response.setEscrowHeld(escrowHeld);
        response.setEscrowReleased(escrowReleased);

        // =========================
        // DOCUMENT REVIEW SUMMARY
        // =========================
        response.setPendingBuilderDocuments(
                userDocumentRepository.countByRoleAndStatus("ROLE_BUILDER", DocumentStatus.PENDING)
        );
        response.setPendingInvestorDocuments(
                userDocumentRepository.countByRoleAndStatus("ROLE_INVESTOR", DocumentStatus.PENDING)
        );

        response.setApprovedBuilderDocuments(
                userDocumentRepository.countByRoleAndStatus("ROLE_BUILDER", DocumentStatus.APPROVED)
        );
        response.setApprovedInvestorDocuments(
                userDocumentRepository.countByRoleAndStatus("ROLE_INVESTOR", DocumentStatus.APPROVED)
        );

        response.setRejectedBuilderDocuments(
                userDocumentRepository.countByRoleAndStatus("ROLE_BUILDER", DocumentStatus.REJECTED)
        );
        response.setRejectedInvestorDocuments(
                userDocumentRepository.countByRoleAndStatus("ROLE_INVESTOR", DocumentStatus.REJECTED)
        );

        return response;
    }

    public BuilderDashboardResponse getBuilderDashboard() {
        Long tenantId = SecurityUtil.getCurrentTenantId();

        Builder builder = builderRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Builder not found"));

        Long builderId = builder.getId();

        BuilderDashboardResponse response = new BuilderDashboardResponse();

        response.setTotalProjects(projectRepository.findByBuilderId(builderId).size());
        response.setTotalPlots(plotRepository.countByProject_Builder_Id(builderId));
        response.setAvailablePlots(plotRepository.countByProject_Builder_IdAndAvailableTrue(builderId));
        response.setBlockedPlots(plotRepository.countByProject_Builder_IdAndBookingStatus(builderId, BookingStatus.TOKEN_BLOCKED));
        response.setSoldPlots(plotRepository.countByProject_Builder_IdAndBookingStatus(builderId, BookingStatus.CONFIRMED));

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal tokenRevenue = BigDecimal.ZERO;
        BigDecimal completedRevenue = BigDecimal.ZERO;

        List<Payment> builderPayments = paymentRepository.findByBooking_Plot_Project_Builder_Id(builderId);
        for (Payment payment : builderPayments) {
            totalRevenue = totalRevenue.add(nvl(payment.getAmount()));

            if (payment.getPaymentStatus() == PaymentStatus.TOKEN_PAID) {
                tokenRevenue = tokenRevenue.add(nvl(payment.getAmount()));
            }

            if (payment.getPaymentStatus() == PaymentStatus.COMPLETED) {
                completedRevenue = completedRevenue.add(nvl(payment.getAmount()));
            }
        }

        response.setTotalRevenue(totalRevenue);
        response.setTokenRevenue(tokenRevenue);
        response.setCompletedRevenue(completedRevenue);

        response.setTotalBookings(bookingRepository.countByPlot_Project_Builder_Id(builderId));
        response.setLast30DaysBookings(
                bookingRepository.findByPlot_Project_Builder_IdAndCreatedAtAfter(
                        builderId,
                        LocalDateTime.now().minusDays(30)
                ).size()
        );

        response.setLowRiskProjects(
                projectLegalDetailsRepository.countByProject_Builder_IdAndProjectRiskCategory(builderId, RiskCategory.LOW)
        );
        response.setMediumRiskProjects(
                projectLegalDetailsRepository.countByProject_Builder_IdAndProjectRiskCategory(builderId, RiskCategory.MEDIUM)
        );
        response.setHighRiskProjects(
                projectLegalDetailsRepository.countByProject_Builder_IdAndProjectRiskCategory(builderId, RiskCategory.HIGH)
        );

        response.setPendingMeetingRequests(
                meetingRequestRepository.countByBuilderIdAndStatus(builderId, MeetingStatus.REQUESTED)
        );
        response.setApprovedMeetings(
                meetingRequestRepository.countByBuilderIdAndStatus(builderId, MeetingStatus.APPROVED)
        );
        response.setCompletedMeetings(
                meetingRequestRepository.countByBuilderIdAndStatus(builderId, MeetingStatus.COMPLETED)
        );
        response.setRejectedMeetings(
                meetingRequestRepository.countByBuilderIdAndStatus(builderId, MeetingStatus.REJECTED)
        );

        response.setRecentActivities(getRecentActivitiesForBuilder(builderId));

        return response;
    }

    public InvestorDashboardResponse getInvestorDashboard(BigDecimal appreciationRate) {
        Long userId = SecurityUtil.getCurrentUserId();
        Long tenantId = SecurityUtil.getCurrentTenantId();

        Investor investor = investorRepository.findByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Investor not found"));

        Long investorId = investor.getId();

        InvestorDashboardResponse response = new InvestorDashboardResponse();

        response.setBlockedPlots(
                bookingRepository.countByInvestorIdAndStatusIn(
                        investorId,
                        List.of(BookingStatus.TOKEN_BLOCKED, BookingStatus.TOKEN_PAID)
                )
        );

        response.setPurchasedPlots(
                bookingRepository.countByInvestorIdAndStatusIn(
                        investorId,
                        List.of(BookingStatus.CONFIRMED, BookingStatus.EMI_ACTIVE)
                )
        );

        BigDecimal totalInvestment = BigDecimal.ZERO;

        List<Payment> investorPayments = paymentRepository.findByBooking_Investor_Id(investorId);
        for (Payment payment : investorPayments) {
            if (payment.getPaymentStatus() == PaymentStatus.TOKEN_PAID
                    || payment.getPaymentStatus() == PaymentStatus.COMPLETED) {
                totalInvestment = totalInvestment.add(nvl(payment.getAmount()));
            }
        }

        List<EmiPaymentTransaction> emiTransactions =
                emiPaymentTransactionRepository.findByInvestorIdAndStatus(
                        investorId,
                        EmiPaymentTransactionStatus.SUCCESS
                );

        for (EmiPaymentTransaction tx : emiTransactions) {
            totalInvestment = totalInvestment.add(nvl(tx.getAmount()));
        }

        response.setTotalInvestment(totalInvestment);

        InvestorAnalyticsSummary analyticsSummary =
                investorAnalyticsService.getInvestorAnalyticsSummary(
                        investorId,
                        totalInvestment,
                        appreciationRate
                );

        response.setRoiPercentage(analyticsSummary.getRoiPercentage());
        response.setProjectedValue3Years(analyticsSummary.getProjectedValue3Years());
        response.setProjectedValue5Years(analyticsSummary.getProjectedValue5Years());

        response.setTotalMeetings(
                meetingRequestRepository.findByInvestorIdOrderByCreatedAtDesc(investorId).size()
        );
        response.setApprovedMeetings(
                meetingRequestRepository.countByInvestorIdAndStatus(investorId, MeetingStatus.APPROVED)
        );
        response.setRequestedMeetings(
                meetingRequestRepository.countByInvestorIdAndStatus(investorId, MeetingStatus.REQUESTED)
        );
        response.setRejectedMeetings(
                meetingRequestRepository.countByInvestorIdAndStatus(investorId, MeetingStatus.REJECTED)
        );
        response.setCancelledMeetings(
                meetingRequestRepository.countByInvestorIdAndStatus(investorId, MeetingStatus.CANCELLED)
        );
        response.setCompletedMeetings(
                meetingRequestRepository.countByInvestorIdAndStatus(investorId, MeetingStatus.COMPLETED)
        );

        return response;
    }

    public List<AdminConflictLogResponse> getRecentConflictLogs() {
        return bookingConflictRepository.findAll().stream()
                .sorted(Comparator.comparing(BookingConflictLog::getCreatedAt).reversed())
                .limit(10)
                .map(log -> {
                    AdminConflictLogResponse response = new AdminConflictLogResponse();
                    response.setId(log.getId());
                    response.setBookingId(null);
                    response.setPlotId(log.getPlotId());
                    response.setMessage(log.getReason());
                    response.setCreatedAt(log.getCreatedAt());
                    return response;
                })
                .toList();
    }

    public List<EscrowDetailResponse> getEscrowDetails() {
        return paymentRepository.findAll().stream()
                .filter(payment ->
                        payment.getPaymentStatus() == PaymentStatus.TOKEN_PAID
                                || payment.getPaymentStatus() == PaymentStatus.COMPLETED
                                || payment.isEscrowReleased()
                                || payment.getEscrowStatus() != null
                )
                .sorted(Comparator.comparing(Payment::getCreatedAt).reversed())
                .map(payment -> {
                    EscrowDetailResponse response = new EscrowDetailResponse();

                    Booking booking = payment.getBooking();
                    Plot plot = booking != null ? booking.getPlot() : null;
                    Project project = plot != null ? plot.getProject() : null;
                    Builder builder = project != null ? project.getBuilder() : null;
                    Investor investor = booking != null ? booking.getInvestor() : null;
                    User builderUser = builder != null ? builder.getUser() : null;
                    User investorUser = investor != null ? investor.getUser() : null;

                    response.setPaymentId(payment.getId());
                    response.setBookingId(booking != null ? booking.getId() : null);
                    response.setPlotId(plot != null ? plot.getId() : null);

                    response.setProjectName(project != null ? project.getProjectName() : null);
                    response.setBuilderName(builderUser != null ? builderUser.getFullName() : null);
                    response.setBuilderCompanyName(builder != null ? builder.getCompanyName() : null);
                    response.setInvestorName(investorUser != null ? investorUser.getFullName() : null);

                    response.setTokenAmount(booking != null ? booking.getAmount() : null);
                    response.setFullAmount(plot != null ? plot.getPrice() : null);

                    response.setAmount(payment.getAmount());
                    response.setPaymentStatus(
                            payment.getPaymentStatus() != null ? payment.getPaymentStatus().name() : null
                    );

                    response.setEscrowStatus(
                            payment.getEscrowStatus() != null
                                    ? payment.getEscrowStatus().name()
                                    : (payment.isEscrowReleased() ? "RELEASED" : null)
                    );

                    response.setEscrowReleased(payment.isEscrowReleased());

                    // Current codebase does not clearly store AUTO vs ADMIN release source.
                    // Keeping a safe fallback so frontend can still show something.
                    response.setReleaseMode(payment.isEscrowReleased() ? "SYSTEM / ADMIN" : null);
                    response.setReleasedAt(payment.getReleasedAt());
                    response.setReleaseReference(payment.getReleaseReference());
                    response.setHoldUntil(payment.getHoldUntil());
                    response.setReadyForReleaseAt(payment.getReadyForReleaseAt());
                    response.setReleaseScheduledAt(payment.getReleaseScheduledAt());
                    response.setReviewReason(payment.getReviewReason());

                    boolean canRelease = escrowRuleEngineService.isEligibleForManualRelease(payment);
                    response.setCanReleaseEscrow(canRelease);
                    response.setReleaseBlockedReason(
                            canRelease ? null : escrowRuleEngineService.firstManualFailureReason(payment)
                    );

                    response.setRiskLevel(
                            payment.getRiskLevel() != null ? payment.getRiskLevel().name() : null
                    );
                    response.setRiskScore(payment.getRiskScore());

                    response.setCreatedAt(payment.getCreatedAt());
                    response.setFrozenAt(payment.getFrozenAt());
                    response.setFreezeDeadlineAt(payment.getFreezeDeadlineAt());
                    response.setFreezeActionRequired(payment.getFreezeActionRequired());
                    response.setFreezeResolvedAt(payment.getFreezeResolvedAt());
                    
                    response.setUnfreezeRequested(payment.isUnfreezeRequested());
                    response.setUnfreezeRequestedAt(payment.getUnfreezeRequestedAt());
                    response.setUnfreezeRequestNote(payment.getUnfreezeRequestNote());

                    return response;
                })
                .toList();
    }
    private List<RecentActivityDTO> getRecentActivitiesForBuilder(Long builderId) {
        List<RecentActivityDTO> activities = new ArrayList<>();

        bookingRepository.findBuilderBookings(builderId).stream()
                .sorted(Comparator.comparing(Booking::getCreatedAt).reversed())
                .limit(5)
                .forEach(b -> activities.add(
                        RecentActivityDTO.builder()
                                .type("BOOKING")
                                .description("Booking for plot " + b.getPlot().getPlotNumber())
                                .time(b.getCreatedAt())
                                .status(b.getStatus().name())
                                .build()
                ));

        meetingRequestRepository.findByBuilderIdOrderByCreatedAtDesc(builderId)
                .stream()
                .limit(5)
                .forEach(m -> activities.add(
                        RecentActivityDTO.builder()
                                .type("MEETING")
                                .description("Meeting request from investor " + m.getInvestor().getId())
                                .time(m.getCreatedAt())
                                .status(m.getStatus().name())
                                .build()
                ));

        return activities.stream()
                .sorted(Comparator.comparing(RecentActivityDTO::getTime).reversed())
                .limit(5)
                .toList();
    }

    private MeetingWidgetResponse mapMeetingWidget(MeetingRequest meeting) {
        MeetingWidgetResponse response = new MeetingWidgetResponse();
        response.setMeetingId(meeting.getId());
        response.setInvestorId(meeting.getInvestor() != null ? meeting.getInvestor().getId() : null);
        response.setBuilderId(meeting.getBuilder() != null ? meeting.getBuilder().getId() : null);
        response.setProjectId(meeting.getProject() != null ? meeting.getProject().getId() : null);
        response.setPlotId(meeting.getPlot() != null ? meeting.getPlot().getId() : null);
        response.setBudget(meeting.getBudget());
        response.setExpectedTimeline(meeting.getExpectedTimeline());
        response.setPurpose(meeting.getPurpose());
        response.setStatus(meeting.getStatus());
        response.setScheduledAt(meeting.getScheduledAt());
        response.setCreatedAt(meeting.getCreatedAt());
        return response;
    }

    public List<MeetingWidgetResponse> getRecentBuilderMeetings() {
        Long tenantId = SecurityUtil.getCurrentTenantId();

        Builder builder = builderRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Builder not found"));

        return meetingRequestRepository.findByBuilderIdOrderByCreatedAtDesc(builder.getId())
                .stream()
                .limit(5)
                .map(this::mapMeetingWidget)
                .toList();
    }

    public List<MeetingWidgetResponse> getRecentInvestorMeetings() {
        Long userId = SecurityUtil.getCurrentUserId();
        Long tenantId = SecurityUtil.getCurrentTenantId();

        Investor investor = investorRepository.findByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Investor not found"));

        return meetingRequestRepository.findByInvestorIdOrderByCreatedAtDesc(investor.getId())
                .stream()
                .limit(5)
                .map(this::mapMeetingWidget)
                .toList();
    }

    public List<PaymentWidgetResponse> getRecentAdminPayments() {
        return paymentRepository.findAll().stream()
                .sorted(Comparator.comparing(Payment::getCreatedAt).reversed())
                .limit(10)
                .map(this::mapPaymentWidget)
                .toList();
    }

    public List<PaymentWidgetResponse> getRecentBuilderPayments() {
        Long tenantId = SecurityUtil.getCurrentTenantId();

        Builder builder = builderRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Builder not found"));

        return paymentRepository.findByBooking_Plot_Project_Builder_Id(builder.getId()).stream()
                .sorted(Comparator.comparing(Payment::getCreatedAt).reversed())
                .limit(10)
                .map(this::mapPaymentWidget)
                .toList();
    }

    private PaymentWidgetResponse mapPaymentWidget(Payment payment) {
        PaymentWidgetResponse response = new PaymentWidgetResponse();
        response.setPaymentId(payment.getId());
        response.setBookingId(payment.getBooking() != null ? payment.getBooking().getId() : null);
        response.setPlotId(
                payment.getBooking() != null && payment.getBooking().getPlot() != null
                        ? payment.getBooking().getPlot().getId()
                        : null
        );
        response.setInvestorId(
                payment.getBooking() != null && payment.getBooking().getInvestor() != null
                        ? payment.getBooking().getInvestor().getId()
                        : null
        );
        response.setAmount(payment.getAmount());
        response.setPaymentStatus(
                payment.getPaymentStatus() != null ? payment.getPaymentStatus().name() : null
        );
        response.setEscrowReleased(payment.isEscrowReleased());
        response.setCreatedAt(payment.getCreatedAt());
        return response;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}