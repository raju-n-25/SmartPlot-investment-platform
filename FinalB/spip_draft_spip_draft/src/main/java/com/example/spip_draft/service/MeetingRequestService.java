package com.example.spip_draft.service;

import com.example.spip_draft.dto.meeting.CreateMeetingRequest;


import com.example.spip_draft.dto.meeting.MeetingApprovalRequest;
import com.example.spip_draft.dto.meeting.MeetingResponse;
import com.example.spip_draft.entity.*;
import com.example.spip_draft.enums.BookingStatus;
import com.example.spip_draft.enums.MeetingStatus;
import com.example.spip_draft.exception.ResourceNotFoundException;
import com.example.spip_draft.repository.*;
import com.example.spip_draft.util.SecurityUtil;
import com.example.spip_draft.enums.AuditAction;
import com.example.spip_draft.dto.meeting.MeetingRescheduleRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MeetingRequestService {

    private final MeetingRequestRepository meetingRequestRepository;
    private final InvestorRepository investorRepository;
    private final BuilderRepository builderRepository;
    private final ProjectRepository projectRepository;
    private final PlotRepository plotRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final BookingRepository bookingRepository;

    public MeetingResponse createMeetingRequest(CreateMeetingRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();

        Investor investor = investorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Investor not found"));

        boolean fromBooking = Boolean.TRUE.equals(request.getFromBooking()) || request.getBookingId() != null;
        boolean fromMarketplace = Boolean.TRUE.equals(request.getFromMarketplace()) && !fromBooking;

        Builder builder;
        Project project = null;
        Plot plot = null;
        Booking booking = null;

        if (fromBooking) {
            if (request.getBookingId() == null) {
                throw new RuntimeException("Booking ID is required for booking meeting");
            }

            booking = bookingRepository.findById(request.getBookingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

            if (!booking.getInvestor().getId().equals(investor.getId())) {
                throw new RuntimeException("Unauthorized booking meeting request");
            }

            plot = booking.getPlot();

            if (plot == null) {
                throw new RuntimeException("Booking is not linked to any plot");
            }

            project = plot.getProject();
            builder = plot.getProject().getBuilder();

            if (booking.getStatus() == BookingStatus.CONFIRMED
                    || booking.getStatus() == BookingStatus.CANCELLED
                    || booking.getStatus() == BookingStatus.FAILED
                    || booking.getStatus() == BookingStatus.EXPIRED
                    || booking.getStatus() == BookingStatus.REFUNDED) {
                throw new RuntimeException("Meeting cannot be requested for this booking status");
            }

            boolean activeBookingMeetingExists = meetingRequestRepository.existsByBookingIdAndStatusIn(
                    booking.getId(),
                    activeMeetingStatuses()
            );

            if (activeBookingMeetingExists) {
                throw new RuntimeException("Meeting already in progress for this booking");
            }
        } else {
            if (request.getBuilderId() == null) {
                throw new RuntimeException("Builder ID is required");
            }

            builder = builderRepository.findById(request.getBuilderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Builder not found"));

            if (request.getProjectId() != null) {
                project = projectRepository.findById(request.getProjectId())
                        .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
            }

            if (request.getPlotId() != null) {
                plot = plotRepository.findById(request.getPlotId())
                        .orElseThrow(() -> new ResourceNotFoundException("Plot not found"));

                if (!Boolean.TRUE.equals(plot.getAvailable())) {
                    throw new RuntimeException("This plot is not available for meeting request");
                }

                if (plot.getBookingStatus() != null) {
                    throw new RuntimeException("This plot is already under booking/meeting hold");
                }

                if (project == null && plot.getProject() != null) {
                    project = plot.getProject();
                }

                boolean activeMeetingExists = meetingRequestRepository.existsByPlotIdAndStatusIn(
                        plot.getId(),
                        activeMeetingStatuses()
                );

                if (activeMeetingExists) {
                    throw new RuntimeException("Meeting already in progress for this plot");
                }
            }
        }

        MeetingRequest meeting = new MeetingRequest();
        meeting.setInvestor(investor);
        meeting.setBuilder(builder);
        meeting.setTenantId(builder.getTenantId());
        meeting.setProject(project);
        meeting.setPlot(plot);
        meeting.setBooking(booking);

        meeting.setBudget(request.getBudget());
        meeting.setExpectedTimeline(request.getExpectedTimeline());
        meeting.setPurpose(request.getPurpose());

        meeting.setFromMarketplace(fromMarketplace);
        meeting.setFromBooking(fromBooking);
        meeting.setReminderSent(false);
        meeting.setBoughtFromMeeting(false);
        meeting.setRejectionReason(null);
        meeting.setStatus(MeetingStatus.REQUESTED);
        meeting.setMeetingRound(1);
        meeting.setHoldActive(false);

        if (fromMarketplace && plot != null) {
            plot.setBookingStatus(BookingStatus.MEETING_HOLD);
            plot.setBlockedAt(LocalDateTime.now());
            plotRepository.save(plot);

            meeting.setHoldActive(true);
        }

        meeting = meetingRequestRepository.save(meeting);

        auditLogService.log(
                AuditAction.MEETING_REQUESTED,
                "MEETING",
                meeting.getId(),
                fromBooking
                        ? "Investor requested meeting for booking " + booking.getId()
                        : "Investor requested marketplace meeting with builder " + builder.getId()
        );

        notificationService.notifyBuilderMeetingRequested(meeting);

        return map(meeting);
    }
    
    private List<MeetingStatus> activeMeetingStatuses() {
        return List.of(
                MeetingStatus.REQUESTED,
                MeetingStatus.APPROVED,
                MeetingStatus.FOLLOW_UP_REQUESTED,
                MeetingStatus.RESCHEDULE_REQUESTED
        );
    }
    @Transactional(readOnly = true)
    public List<MeetingResponse> getInvestorMeetings() {
        Long userId = SecurityUtil.getCurrentUserId();

        Investor investor = investorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Investor not found"));

        return meetingRequestRepository.findByInvestorIdOrderByCreatedAtDesc(investor.getId())
                .stream()
                .map(this::map)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MeetingResponse> getBuilderMeetings() {
        Long builderId = SecurityUtil.getCurrentBuilderId();

        if (builderId == null) {
            throw new RuntimeException("Builder ID missing in token. Please logout and login again.");
        }

        return meetingRequestRepository.findByBuilderIdOrderByCreatedAtDesc(builderId)
                .stream()
                .map(this::map)
                .toList();
    }

    public MeetingResponse approveOrReject(Long meetingId, MeetingApprovalRequest request) {
        Long builderId = SecurityUtil.getCurrentBuilderId();

        if (builderId == null) {
            throw new RuntimeException("Builder ID missing in token. Please logout and login again.");
        }

        MeetingRequest meeting = meetingRequestRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        if (!meeting.getBuilder().getId().equals(builderId)) {
            throw new RuntimeException("Unauthorized");
        }

        if (Boolean.TRUE.equals(request.getApproved())) {
            meeting.setStatus(MeetingStatus.APPROVED);
            meeting.setScheduledAt(request.getScheduledAt());
            meeting.setMeetingLink(request.getMeetingLink());
            meeting.setRejectionReason(null);
            meeting.setReminderSent(false);

            auditLogService.log(
                    AuditAction.MEETING_APPROVED,
                    "MEETING",
                    meeting.getId(),
                    "Builder approved meeting request"
            );

            notificationService.notifyInvestorMeetingApproved(meeting);
        } else {
            meeting.setStatus(MeetingStatus.REJECTED);
            meeting.setRejectionReason(
                    request.getRejectionReason() != null && !request.getRejectionReason().isBlank()
                            ? request.getRejectionReason()
                            : "Builder rejected the meeting request."
            );

            auditLogService.log(
                    AuditAction.MEETING_REJECTED,
                    "MEETING",
                    meeting.getId(),
                    "Builder rejected meeting request"
            );

            releasePlotIfHeld(meeting, "Builder rejected meeting request");
            notificationService.notifyInvestorMeetingRejected(meeting);
        }

        meeting = meetingRequestRepository.save(meeting);
        return map(meeting);
    }
    public ResponseEntity<Void> joinMeeting(Long meetingId) {
        Long currentUserId = SecurityUtil.getCurrentUserId();

        MeetingRequest meeting = meetingRequestRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        Long investorUserId = meeting.getInvestor() != null && meeting.getInvestor().getUser() != null
                ? meeting.getInvestor().getUser().getId()
                : null;

        Long builderUserId = meeting.getBuilder() != null && meeting.getBuilder().getUser() != null
                ? meeting.getBuilder().getUser().getId()
                : null;

        if (!currentUserId.equals(investorUserId) && !currentUserId.equals(builderUserId)) {
            throw new RuntimeException("Unauthorized");
        }

        if (meeting.getMeetingLink() == null || meeting.getMeetingLink().isBlank()) {
            throw new RuntimeException("Meeting link not available");
        }

        return ResponseEntity.status(302)
                .header("Location", meeting.getMeetingLink())
                .build();
    }

    public MeetingResponse cancelMeeting(Long meetingId) {
        Long userId = SecurityUtil.getCurrentUserId();

        Investor investor = investorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Investor not found"));

        MeetingRequest meeting = meetingRequestRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        if (!meeting.getInvestor().getId().equals(investor.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        meeting.setStatus(MeetingStatus.CANCELLED);

        releasePlotIfHeld(meeting, "Investor cancelled meeting flow");
        notificationService.notifyBuilderPlotReturnedToMarketplace(meeting);

        meeting = meetingRequestRepository.save(meeting);
        return map(meeting);
    }

    public MeetingResponse markMeetingCompleted(Long meetingId) {
        Long builderId = SecurityUtil.getCurrentBuilderId();

        if (builderId == null) {
            throw new RuntimeException("Builder ID missing in token. Please logout and login again.");
        }

        MeetingRequest meeting = meetingRequestRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        if (!meeting.getBuilder().getId().equals(builderId)) {
            throw new RuntimeException("Unauthorized");
        }

        if (meeting.getStatus() != MeetingStatus.APPROVED) {
            throw new RuntimeException("Only approved meetings can be marked as completed");
        }

        meeting.setStatus(MeetingStatus.COMPLETED);

        meeting = meetingRequestRepository.save(meeting);

        auditLogService.log(
                AuditAction.MEETING_COMPLETED,
                "MEETING",
                meeting.getId(),
                "Builder marked meeting as completed"
        );

        notificationService.notifyInvestorMeetingCompletedActionRequired(meeting);

        return map(meeting);
    }
    
    public MeetingResponse rejectAfterMeeting(Long meetingId, MeetingApprovalRequest request) {
        Long builderId = SecurityUtil.getCurrentBuilderId();

        if (builderId == null) {
            throw new RuntimeException("Builder ID missing in token. Please logout and login again.");
        }

        MeetingRequest meeting = meetingRequestRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        if (!meeting.getBuilder().getId().equals(builderId)) {
            throw new RuntimeException("Unauthorized");
        }

        if (meeting.getStatus() != MeetingStatus.COMPLETED) {
            throw new RuntimeException("Builder can reject only after meeting is completed");
        }

        meeting.setStatus(MeetingStatus.BUILDER_REJECTED_AFTER_MEETING);
        meeting.setRejectionReason(
                request.getRejectionReason() != null && !request.getRejectionReason().isBlank()
                        ? request.getRejectionReason()
                        : "Builder decided not to proceed after the meeting."
        );

        if (Boolean.TRUE.equals(meeting.getFromMarketplace())) {
            releasePlotIfHeld(meeting, "Builder rejected investor after meeting");
            notificationService.notifyInvestorMeetingRejected(meeting);
            notificationService.notifyBuilderPlotReturnedToMarketplace(meeting);
        } else {
            notificationService.notifyInvestorMeetingRejected(meeting);
        }

        auditLogService.log(
                AuditAction.MEETING_REJECTED,
                "MEETING",
                meeting.getId(),
                "Builder rejected after completed meeting"
        );

        meeting = meetingRequestRepository.save(meeting);
        return map(meeting);
    }
    public MeetingResponse requestFollowUpMeeting(Long meetingId) {
        Long userId = SecurityUtil.getCurrentUserId();

        Investor investor = investorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Investor not found"));

        MeetingRequest meeting = meetingRequestRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        if (!meeting.getInvestor().getId().equals(investor.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        if (Boolean.TRUE.equals(meeting.getFromMarketplace())
                && !Boolean.TRUE.equals(meeting.getHoldActive())) {
            throw new RuntimeException("This plot is no longer under meeting hold");
        }
       

        if (meeting.getStatus() != MeetingStatus.COMPLETED
                && meeting.getStatus() != MeetingStatus.APPROVED) {
            throw new RuntimeException("Follow-up meeting can only be requested after an active/completed meeting");
        }

        meeting.setStatus(MeetingStatus.FOLLOW_UP_REQUESTED);
        meeting.setMeetingRound(meeting.getMeetingRound() == null ? 2 : meeting.getMeetingRound() + 1);
        meeting.setReminderSent(false);
        meeting.setScheduledAt(null);
        meeting.setMeetingLink(null);
        meeting.setRejectionReason(null);

        meeting = meetingRequestRepository.save(meeting);
        notificationService.notifyBuilderFollowUpMeetingRequested(meeting);

        return map(meeting);
    }

    public MeetingResponse proceedToBuy(Long meetingId) {
        Long userId = SecurityUtil.getCurrentUserId();

        Investor investor = investorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Investor not found"));

        MeetingRequest meeting = meetingRequestRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        if (!meeting.getInvestor().getId().equals(investor.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        if (meeting.getPlot() == null) {
            throw new RuntimeException("This meeting is not linked to any plot");
        }

        if (!Boolean.TRUE.equals(meeting.getHoldActive())) {
            throw new RuntimeException("This plot is not under meeting hold anymore");
        }

        // release hold so existing booking flow can continue
        Plot plot = meeting.getPlot();
        plot.setBookingStatus(null);
        plot.setBlockedAt(null);
        plotRepository.save(plot);

        meeting.setHoldActive(false);
        meeting.setBoughtFromMeeting(true);
        meeting.setStatus(MeetingStatus.BUY_REQUESTED);
        meeting = meetingRequestRepository.save(meeting);

        notificationService.notifyBuilderMeetingBuyRequested(meeting);

        return map(meeting);
    }

    public MeetingResponse releasePlotToMarketplace(Long meetingId) {
        Long userId = SecurityUtil.getCurrentUserId();

        Investor investor = investorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Investor not found"));

        MeetingRequest meeting = meetingRequestRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        if (!meeting.getInvestor().getId().equals(investor.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        if (!Boolean.TRUE.equals(meeting.getHoldActive())) {
            throw new RuntimeException("Plot already released");
        }

        meeting.setStatus(MeetingStatus.RELEASED);
        releasePlotIfHeld(meeting, "Investor chose not to proceed after meeting");

        meeting = meetingRequestRepository.save(meeting);
        notificationService.notifyBuilderPlotReturnedToMarketplace(meeting);

        return map(meeting);
    }
    
    public MeetingResponse requestReschedule(Long meetingId, MeetingRescheduleRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();

        Investor investor = investorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Investor not found"));

        MeetingRequest meeting = meetingRequestRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        if (!meeting.getInvestor().getId().equals(investor.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        if (meeting.getStatus() != MeetingStatus.APPROVED) {
            throw new RuntimeException("Only approved meetings can be rescheduled");
        }

        if (Boolean.TRUE.equals(meeting.getFromMarketplace())
                && !Boolean.TRUE.equals(meeting.getHoldActive())) {
            throw new RuntimeException("This plot is no longer under meeting hold");
        }

        meeting.setStatus(MeetingStatus.RESCHEDULE_REQUESTED);
        meeting.setRequestedRescheduleAt(request.getRequestedRescheduleAt());
        meeting.setRescheduleReason(
                request.getRescheduleReason() != null && !request.getRescheduleReason().isBlank()
                        ? request.getRescheduleReason()
                        : "Investor requested to reschedule the meeting."
        );
        meeting.setRescheduleRequestedAt(LocalDateTime.now());

        meeting = meetingRequestRepository.save(meeting);

        notificationService.notifyBuilderMeetingRequested(meeting);

        return map(meeting);
    }

    public MeetingResponse approveReschedule(Long meetingId) {
        Long builderId = SecurityUtil.getCurrentBuilderId();

        if (builderId == null) {
            throw new RuntimeException("Builder ID missing in token. Please logout and login again.");
        }

        MeetingRequest meeting = meetingRequestRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        if (!meeting.getBuilder().getId().equals(builderId)) {
            throw new RuntimeException("Unauthorized");
        }

        if (meeting.getStatus() != MeetingStatus.RESCHEDULE_REQUESTED) {
            throw new RuntimeException("No reschedule request is pending for this meeting");
        }

        if (meeting.getRequestedRescheduleAt() == null) {
            throw new RuntimeException("Requested reschedule date/time is missing");
        }

        meeting.setScheduledAt(meeting.getRequestedRescheduleAt());
        meeting.setStatus(MeetingStatus.APPROVED);
        meeting.setReminderSent(false);

        meeting.setRequestedRescheduleAt(null);
        meeting.setRescheduleReason(null);
        meeting.setRescheduleRequestedAt(null);

        meeting = meetingRequestRepository.save(meeting);

        notificationService.notifyInvestorMeetingApproved(meeting);

        return map(meeting);
    }

    public MeetingResponse rejectReschedule(Long meetingId) {
        Long builderId = SecurityUtil.getCurrentBuilderId();

        if (builderId == null) {
            throw new RuntimeException("Builder ID missing in token. Please logout and login again.");
        }

        MeetingRequest meeting = meetingRequestRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        if (!meeting.getBuilder().getId().equals(builderId)) {
            throw new RuntimeException("Unauthorized");
        }

        if (meeting.getStatus() != MeetingStatus.RESCHEDULE_REQUESTED) {
            throw new RuntimeException("No reschedule request is pending for this meeting");
        }

        meeting.setStatus(MeetingStatus.APPROVED);

        meeting.setRequestedRescheduleAt(null);
        meeting.setRescheduleReason(null);
        meeting.setRescheduleRequestedAt(null);

        meeting = meetingRequestRepository.save(meeting);

        notificationService.notifyInvestorMeetingApproved(meeting);

        return map(meeting);
    }

    private void releasePlotIfHeld(MeetingRequest meeting, String reason) {
        if (meeting.getPlot() != null && Boolean.TRUE.equals(meeting.getHoldActive())) {
            Plot plot = meeting.getPlot();
            plot.setBookingStatus(null);
            plot.setBlockedAt(null);
            plotRepository.save(plot);

            meeting.setHoldActive(false);
            meeting.setReleasedAt(LocalDateTime.now());
            meeting.setReleaseReason(reason);
        }
    }

    private MeetingResponse map(MeetingRequest m) {
        MeetingResponse r = new MeetingResponse();

        r.setId(m.getId());
        r.setInvestorId(m.getInvestor().getId());
        r.setBuilderId(m.getBuilder().getId());
        r.setProjectId(m.getProject() != null ? m.getProject().getId() : null);
        r.setPlotId(m.getPlot() != null ? m.getPlot().getId() : null);

        r.setInvestorName(
                m.getInvestor() != null && m.getInvestor().getUser() != null
                        ? m.getInvestor().getUser().getFullName()
                        : null
        );
        r.setBuilderName(
                m.getBuilder() != null && m.getBuilder().getUser() != null
                        ? m.getBuilder().getUser().getFullName()
                        : null
        );
        r.setProjectName(m.getProject() != null ? m.getProject().getProjectName() : null);
        r.setPlotNumber(m.getPlot() != null ? m.getPlot().getPlotNumber() : null);

        r.setBudget(m.getBudget());
        r.setExpectedTimeline(m.getExpectedTimeline());
        r.setPurpose(m.getPurpose());
        r.setStatus(m.getStatus());
        r.setScheduledAt(m.getScheduledAt());
        r.setMeetingLink(m.getMeetingLink());
        r.setRejectionReason(m.getRejectionReason());
        r.setReminderSent(m.getReminderSent());
        r.setFromMarketplace(m.getFromMarketplace());
        r.setHoldActive(m.getHoldActive());
        r.setMeetingRound(m.getMeetingRound());
        r.setBoughtFromMeeting(m.getBoughtFromMeeting());
        r.setReleasedAt(m.getReleasedAt());
        r.setReleaseReason(m.getReleaseReason());
        r.setCreatedAt(m.getCreatedAt());
        r.setRequestedRescheduleAt(m.getRequestedRescheduleAt());
        r.setRescheduleReason(m.getRescheduleReason());
        r.setRescheduleRequestedAt(m.getRescheduleRequestedAt());
        r.setBookingId(m.getBooking() != null ? m.getBooking().getId() : null);
        r.setFromBooking(m.getFromBooking());

        boolean holdActive = Boolean.TRUE.equals(m.getHoldActive());
        boolean fromMarketplace = Boolean.TRUE.equals(m.getFromMarketplace());
        boolean fromBooking = Boolean.TRUE.equals(m.getFromBooking());

        boolean completedOrApproved =
                m.getStatus() == MeetingStatus.COMPLETED || m.getStatus() == MeetingStatus.APPROVED;

        r.setCanRequestFollowUp(
                completedOrApproved &&
                        (
                                (fromMarketplace && holdActive) ||
                                fromBooking
                        )
        );

        r.setCanProceedToBuy(
                fromMarketplace &&
                        holdActive &&
                        completedOrApproved &&
                        m.getPlot() != null
        );

        r.setCanReleasePlot(
                fromMarketplace &&
                        holdActive
        );

        r.setCanRequestReschedule(
                m.getStatus() == MeetingStatus.APPROVED &&
                        (
                                (fromMarketplace && holdActive) ||
                                fromBooking
                        )
        );

        r.setCanApproveReschedule(m.getStatus() == MeetingStatus.RESCHEDULE_REQUESTED);

        r.setCanBuilderRejectAfterMeet(
                m.getStatus() == MeetingStatus.COMPLETED &&
                        fromMarketplace &&
                        holdActive
        );

        return r;
    }
}