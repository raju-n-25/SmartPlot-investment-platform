package com.example.spip_draft.service;

import com.example.spip_draft.dto.complaint.ComplaintActionRequest;
import com.example.spip_draft.dto.complaint.ComplaintRequest;
import com.example.spip_draft.dto.complaint.ComplaintResponse;
import com.example.spip_draft.entity.Booking;
import com.example.spip_draft.entity.BookingAgreement;
import com.example.spip_draft.entity.Complaint;
import com.example.spip_draft.entity.Notification;
import com.example.spip_draft.entity.Payment;
import com.example.spip_draft.entity.User;
import com.example.spip_draft.enums.AuditAction;
import com.example.spip_draft.enums.ComplaintRaisedByRole;
import com.example.spip_draft.enums.ComplaintStatus;
import com.example.spip_draft.enums.ComplaintType;
import com.example.spip_draft.mapper.ComplaintMapper;
import com.example.spip_draft.repository.BookingAgreementRepository;
import com.example.spip_draft.repository.BookingRepository;
import com.example.spip_draft.repository.ComplaintRepository;
import com.example.spip_draft.repository.PaymentRepository;
import com.example.spip_draft.repository.UserRepository;
import com.example.spip_draft.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final BookingAgreementRepository bookingAgreementRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<ComplaintResponse> getMyComplaints() {
        User currentUser = getCurrentUser();

        return complaintRepository.findByRaisedByUserOrderByCreatedAtDesc(currentUser)
                .stream()
                .map(ComplaintMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponse> getAllComplaintsForAdmin() {
        ensureAdmin();

        return complaintRepository.findAll()
                .stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(ComplaintMapper::toResponse)
                .toList();
    }

    public ComplaintResponse createGeneralComplaint(ComplaintRequest request) {
        User currentUser = getCurrentUser();

        ComplaintRaisedByRole raisedByRole = resolveRaisedByRole();

        ComplaintType type = request != null && request.getComplaintType() != null
                ? request.getComplaintType()
                : ComplaintType.GENERAL;

        String reason = request != null ? request.getReason() : null;

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Complaint reason is required");
        }

        boolean paymentRelated = safeBool(request != null ? request.getPaymentRelated() : null)
                || type == ComplaintType.PAYMENT_ISSUE
                || type == ComplaintType.REFUND_ISSUE;

        boolean escrowRelated = safeBool(request != null ? request.getEscrowRelated() : null)
                || type == ComplaintType.ESCROW_ISSUE
                || type == ComplaintType.PAYMENT_ISSUE
                || type == ComplaintType.REFUND_ISSUE;

        boolean agreementRelated = safeBool(request != null ? request.getAgreementRelated() : null)
                || type == ComplaintType.AGREEMENT_ISSUE;

        Complaint complaint = new Complaint();
        complaint.setTenantId(resolveTenantIdForGeneral());
        complaint.setComplaintType(type);
        complaint.setRaisedByRole(raisedByRole);
        complaint.setStatus(ComplaintStatus.OPEN);
        complaint.setReason(reason.trim());
        complaint.setDescription(request != null ? request.getDescription() : null);
        complaint.setRaisedByUser(currentUser);
        complaint.setPaymentRelated(paymentRelated);
        complaint.setEscrowRelated(escrowRelated);
        complaint.setAgreementRelated(agreementRelated);

        Complaint saved = complaintRepository.save(complaint);

        auditLogService.logDetailed(
                AuditAction.COMPLAINT_CREATED,
                "COMPLAINT",
                "COMPLAINT",
                saved.getId(),
                "Complaint #" + saved.getId() + " - " + saved.getReason(),
                saved.getRaisedByRole() + " raised a general complaint: " + saved.getReason(),
                null,
                saved.getStatus().name(),
                buildComplaintMetadata(saved)
        );

        notifyAdminsComplaintRaised(saved);

        return ComplaintMapper.toResponse(saved);
    }

    public ComplaintResponse createBookingComplaint(Long bookingId, ComplaintRequest request) {
        User currentUser = getCurrentUser();

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        ComplaintRaisedByRole raisedByRole = resolveRaisedByRole();

        validateCanRaiseForBooking(booking, raisedByRole);

        ComplaintType type = request != null && request.getComplaintType() != null
                ? request.getComplaintType()
                : ComplaintType.GENERAL;

        String reason = request != null ? request.getReason() : null;

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Complaint reason is required");
        }

        Payment payment = paymentRepository.findByBooking(booking).orElse(null);
        BookingAgreement agreement = bookingAgreementRepository.findByBookingId(bookingId).orElse(null);

        boolean paymentRelated = safeBool(request != null ? request.getPaymentRelated() : null)
                || type == ComplaintType.PAYMENT_ISSUE
                || type == ComplaintType.REFUND_ISSUE;

        boolean escrowRelated = safeBool(request != null ? request.getEscrowRelated() : null)
                || type == ComplaintType.ESCROW_ISSUE
                || type == ComplaintType.PAYMENT_ISSUE
                || type == ComplaintType.REFUND_ISSUE;

        boolean agreementRelated = safeBool(request != null ? request.getAgreementRelated() : null)
                || type == ComplaintType.AGREEMENT_ISSUE;

        Complaint complaint = new Complaint();
        complaint.setTenantId(resolveTenantId(booking, currentUser));
        complaint.setComplaintType(type);
        complaint.setRaisedByRole(raisedByRole);
        complaint.setStatus(ComplaintStatus.OPEN);
        complaint.setReason(reason.trim());
        complaint.setDescription(request != null ? request.getDescription() : null);
        complaint.setRaisedByUser(currentUser);
        complaint.setBooking(booking);
        complaint.setPayment(payment);
        complaint.setAgreement(agreement);
        complaint.setProject(booking.getPlot() != null ? booking.getPlot().getProject() : null);
        complaint.setPlot(booking.getPlot());
        complaint.setPaymentRelated(paymentRelated);
        complaint.setEscrowRelated(escrowRelated);
        complaint.setAgreementRelated(agreementRelated);

        Complaint saved = complaintRepository.save(complaint);

        auditLogService.logDetailed(
                AuditAction.COMPLAINT_CREATED,
                "COMPLAINT",
                "COMPLAINT",
                saved.getId(),
                "Complaint #" + saved.getId() + " - Booking #" + booking.getId(),
                saved.getRaisedByRole()
                        + " raised complaint for booking #"
                        + booking.getId()
                        + ": "
                        + saved.getReason(),
                null,
                saved.getStatus().name(),
                buildComplaintMetadata(saved)
        );

        if (escrowRelated || paymentRelated) {
            try {
                paymentService.openDispute(
                        bookingId,
                        "Complaint #" + saved.getId() + ": " + saved.getReason()
                );
            } catch (Exception ignored) {
                // Complaint should still be saved even if escrow review cannot be opened
            }
        }

        notifyAdminsComplaintRaised(saved);

        return ComplaintMapper.toResponse(saved);
    }

    public ComplaintResponse markUnderReview(Long complaintId, ComplaintActionRequest request) {
        ensureAdmin();

        Complaint complaint = getComplaint(complaintId);
        User admin = getCurrentUser();

        ComplaintStatus oldStatus = complaint.getStatus();

        complaint.markUnderReview(admin, request != null ? request.getNote() : null);

        Complaint saved = complaintRepository.save(complaint);

        auditLogService.logDetailed(
                AuditAction.COMPLAINT_MARKED_UNDER_REVIEW,
                "COMPLAINT",
                "COMPLAINT",
                saved.getId(),
                "Complaint #" + saved.getId() + " - " + saved.getReason(),
                "Admin marked complaint #" + saved.getId() + " as under review.",
                oldStatus != null ? oldStatus.name() : null,
                saved.getStatus().name(),
                buildComplaintMetadata(saved)
        );

        notifyComplaintCreator(
                saved,
                "Complaint Under Review",
                "Your complaint #" + saved.getId() + " is now under admin review."
        );

        return ComplaintMapper.toResponse(saved);
    }

    public ComplaintResponse resolveComplaint(Long complaintId, ComplaintActionRequest request) {
        ensureAdmin();

        Complaint complaint = getComplaint(complaintId);
        User admin = getCurrentUser();

        ComplaintStatus oldStatus = complaint.getStatus();

        String note = request != null ? request.getResolutionNote() : null;

        if (note == null || note.isBlank()) {
            note = "Complaint has been resolved by admin.";
        }

        complaint.markResolved(admin, note);

        Complaint saved = complaintRepository.save(complaint);

        auditLogService.logDetailed(
                AuditAction.COMPLAINT_RESOLVED,
                "COMPLAINT",
                "COMPLAINT",
                saved.getId(),
                "Complaint #" + saved.getId() + " - " + saved.getReason(),
                "Admin resolved complaint #" + saved.getId() + ". Resolution: " + note,
                oldStatus != null ? oldStatus.name() : null,
                saved.getStatus().name(),
                buildComplaintMetadata(saved)
        );

        notifyComplaintCreator(
                saved,
                "Complaint Resolved",
                "Your complaint #" + saved.getId() + " has been resolved. Resolution: " + note
        );

        return ComplaintMapper.toResponse(saved);
    }

    public ComplaintResponse rejectComplaint(Long complaintId, ComplaintActionRequest request) {
        ensureAdmin();

        Complaint complaint = getComplaint(complaintId);
        User admin = getCurrentUser();

        ComplaintStatus oldStatus = complaint.getStatus();

        String reason = request != null ? request.getRejectionReason() : null;

        if (reason == null || reason.isBlank()) {
            reason = "Complaint was rejected after admin review.";
        }

        complaint.markRejected(admin, reason);

        Complaint saved = complaintRepository.save(complaint);

        auditLogService.logDetailed(
                AuditAction.COMPLAINT_REJECTED,
                "COMPLAINT",
                "COMPLAINT",
                saved.getId(),
                "Complaint #" + saved.getId() + " - " + saved.getReason(),
                "Admin rejected complaint #" + saved.getId() + ". Reason: " + reason,
                oldStatus != null ? oldStatus.name() : null,
                saved.getStatus().name(),
                buildComplaintMetadata(saved)
        );

        notifyComplaintCreator(
                saved,
                "Complaint Rejected",
                "Your complaint #" + saved.getId() + " was rejected. Reason: " + reason
        );

        return ComplaintMapper.toResponse(saved);
    }

    private Complaint getComplaint(Long id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found"));
    }

    private User getCurrentUser() {
        Long userId = SecurityUtil.getCurrentUserId();

        if (userId == null) {
            throw new IllegalStateException("User not authenticated");
        }

        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Current user not found"));
    }

    private ComplaintRaisedByRole resolveRaisedByRole() {
        if (SecurityUtil.isInvestor()) {
            return ComplaintRaisedByRole.INVESTOR;
        }

        if (SecurityUtil.isBuilder()) {
            return ComplaintRaisedByRole.BUILDER;
        }

        if (SecurityUtil.isAdmin()) {
            return ComplaintRaisedByRole.ADMIN;
        }

        throw new IllegalStateException("Only investor, builder, or admin can raise complaints");
    }

    private void validateCanRaiseForBooking(Booking booking, ComplaintRaisedByRole role) {
        if (role == ComplaintRaisedByRole.ADMIN) {
            return;
        }

        Long currentUserId = SecurityUtil.getCurrentUserId();

        if (role == ComplaintRaisedByRole.INVESTOR) {
            if (booking.getInvestor() == null
                    || booking.getInvestor().getUser() == null
                    || !booking.getInvestor().getUser().getId().equals(currentUserId)) {
                throw new IllegalStateException("This booking does not belong to current investor");
            }
            return;
        }

        if (role == ComplaintRaisedByRole.BUILDER) {
            if (booking.getPlot() == null
                    || booking.getPlot().getProject() == null
                    || booking.getPlot().getProject().getBuilder() == null
                    || booking.getPlot().getProject().getBuilder().getUser() == null
                    || !booking.getPlot().getProject().getBuilder().getUser().getId().equals(currentUserId)) {
                throw new IllegalStateException("This booking does not belong to current builder");
            }
        }
    }

    private Long resolveTenantIdForGeneral() {
        try {
            Long tenantId = SecurityUtil.getCurrentTenantId();
            if (tenantId != null) {
                return tenantId;
            }
        } catch (Exception ignored) {
            // fallback below
        }

        return 1L;
    }

    private Long resolveTenantId(Booking booking, User user) {
        if (booking != null && booking.getTenantId() != null) {
            return booking.getTenantId();
        }

        try {
            Long tenantId = SecurityUtil.getCurrentTenantId();
            if (tenantId != null) {
                return tenantId;
            }
        } catch (Exception ignored) {
            // fallback below
        }

        if (user != null && user.getTenantId() != null) {
            return user.getTenantId();
        }

        return 1L;
    }

    private void ensureAdmin() {
        if (!SecurityUtil.isAdmin()) {
            throw new IllegalStateException("Only admin can perform this action");
        }
    }

    private boolean safeBool(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    private void notifyAdminsComplaintRaised(Complaint complaint) {
        List<User> admins = userRepository.findAll()
                .stream()
                .filter(user -> user.getRole() != null && String.valueOf(user.getRole()).contains("ADMIN"))
                .toList();

        for (User admin : admins) {
            Notification n = new Notification();
            n.setUser(admin);
            n.setTitle("New Complaint Raised");
            n.setMessage(
                    "Complaint #" + complaint.getId()
                            + " raised by " + complaint.getRaisedByRole()
                            + " for booking #"
                            + (complaint.getBooking() != null ? complaint.getBooking().getId() : "N/A")
                            + ". Reason: " + complaint.getReason()
            );
            n.setType("COMPLAINT_RAISED");
            n.setReferenceType("COMPLAINT");
            n.setReferenceId(complaint.getId());
            n.setIsRead(false);

            notificationService.save(n);
        }
    }

    private void notifyComplaintCreator(Complaint complaint, String title, String message) {
        if (complaint.getRaisedByUser() == null) {
            return;
        }

        Notification n = new Notification();
        n.setUser(complaint.getRaisedByUser());
        n.setTitle(title);
        n.setMessage(message);
        n.setType("COMPLAINT_UPDATE");
        n.setReferenceType("COMPLAINT");
        n.setReferenceId(complaint.getId());
        n.setIsRead(false);

        notificationService.save(n);
    }

    private String buildComplaintMetadata(Complaint complaint) {
        if (complaint == null) {
            return "{}";
        }

        Long bookingId = complaint.getBooking() != null ? complaint.getBooking().getId() : null;
        Long projectId = complaint.getProject() != null ? complaint.getProject().getId() : null;
        Long plotId = complaint.getPlot() != null ? complaint.getPlot().getId() : null;

        String projectName = complaint.getProject() != null
                ? complaint.getProject().getProjectName()
                : null;

        String plotNumber = complaint.getPlot() != null
                ? complaint.getPlot().getPlotNumber()
                : null;

        String raisedByName = complaint.getRaisedByUser() != null
                ? complaint.getRaisedByUser().getFullName()
                : null;

        return "{"
                + "\"complaintId\":" + complaint.getId()
                + ",\"status\":\"" + jsonSafe(complaint.getStatus() != null ? complaint.getStatus().name() : null) + "\""
                + ",\"complaintType\":\"" + jsonSafe(complaint.getComplaintType() != null ? complaint.getComplaintType().name() : null) + "\""
                + ",\"raisedByRole\":\"" + jsonSafe(complaint.getRaisedByRole() != null ? complaint.getRaisedByRole().name() : null) + "\""
                + ",\"raisedByName\":\"" + jsonSafe(raisedByName) + "\""
                + ",\"bookingId\":" + nullableNumber(bookingId)
                + ",\"projectId\":" + nullableNumber(projectId)
                + ",\"plotId\":" + nullableNumber(plotId)
                + ",\"projectName\":\"" + jsonSafe(projectName) + "\""
                + ",\"plotNumber\":\"" + jsonSafe(plotNumber) + "\""
                + ",\"reason\":\"" + jsonSafe(complaint.getReason()) + "\""
                + ",\"escrowRelated\":" + Boolean.TRUE.equals(complaint.getEscrowRelated())
                + ",\"paymentRelated\":" + Boolean.TRUE.equals(complaint.getPaymentRelated())
                + ",\"agreementRelated\":" + Boolean.TRUE.equals(complaint.getAgreementRelated())
                + "}";
    }

    private String nullableNumber(Long value) {
        return value == null ? "null" : String.valueOf(value);
    }

    private String jsonSafe(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", " ")
                .trim();
    }
}