package com.example.spip_draft.mapper;

import com.example.spip_draft.dto.complaint.ComplaintResponse;
import com.example.spip_draft.entity.Booking;
import com.example.spip_draft.entity.BookingAgreement;
import com.example.spip_draft.entity.Complaint;
import com.example.spip_draft.entity.Payment;
import com.example.spip_draft.entity.Plot;
import com.example.spip_draft.entity.Project;
import com.example.spip_draft.entity.User;

public class ComplaintMapper {

    private ComplaintMapper() {
    }

    public static ComplaintResponse toResponse(Complaint complaint) {
        if (complaint == null) {
            return null;
        }

        User raisedBy = complaint.getRaisedByUser();
        User assignedAdmin = complaint.getAssignedAdmin();
        User resolvedByAdmin = complaint.getResolvedByAdmin();

        Booking booking = complaint.getBooking();
        Payment payment = complaint.getPayment();
        BookingAgreement agreement = complaint.getAgreement();
        Project project = complaint.getProject();
        Plot plot = complaint.getPlot();

        String projectName = "-";
        String plotNumber = "-";
        String builderName = "-";
        String investorName = "-";

        if (booking != null) {
            if (booking.getInvestor() != null && booking.getInvestor().getUser() != null) {
                investorName = safe(booking.getInvestor().getUser().getFullName());
            }

            if (booking.getPlot() != null) {
                plot = booking.getPlot();
                plotNumber = safe(booking.getPlot().getPlotNumber());

                if (booking.getPlot().getProject() != null) {
                    project = booking.getPlot().getProject();
                    projectName = safe(booking.getPlot().getProject().getProjectName());

                    if (booking.getPlot().getProject().getBuilder() != null
                            && booking.getPlot().getProject().getBuilder().getUser() != null) {
                        builderName = safe(booking.getPlot().getProject().getBuilder().getUser().getFullName());
                    }
                }
            }
        } else {
            if (plot != null) {
                plotNumber = safe(plot.getPlotNumber());

                if (plot.getProject() != null) {
                    project = plot.getProject();
                    projectName = safe(plot.getProject().getProjectName());

                    if (plot.getProject().getBuilder() != null
                            && plot.getProject().getBuilder().getUser() != null) {
                        builderName = safe(plot.getProject().getBuilder().getUser().getFullName());
                    }
                }
            }

            if (project != null) {
                projectName = safe(project.getProjectName());
            }
        }

        return ComplaintResponse.builder()
                .id(complaint.getId())
                .tenantId(complaint.getTenantId())
                .complaintType(complaint.getComplaintType())
                .raisedByRole(complaint.getRaisedByRole())
                .status(complaint.getStatus())
                .reason(complaint.getReason())
                .description(complaint.getDescription())
                .adminNote(complaint.getAdminNote())
                .resolutionNote(complaint.getResolutionNote())
                .rejectionReason(complaint.getRejectionReason())
                .escrowRelated(complaint.getEscrowRelated())
                .paymentRelated(complaint.getPaymentRelated())
                .agreementRelated(complaint.getAgreementRelated())
                .raisedByUserId(raisedBy != null ? raisedBy.getId() : null)
                .raisedByUserName(raisedBy != null ? safe(raisedBy.getFullName()) : "-")
                .raisedByUserEmail(raisedBy != null ? safe(raisedBy.getEmail()) : "-")
                .assignedAdminId(assignedAdmin != null ? assignedAdmin.getId() : null)
                .assignedAdminName(assignedAdmin != null ? safe(assignedAdmin.getFullName()) : "-")
                .resolvedByAdminId(resolvedByAdmin != null ? resolvedByAdmin.getId() : null)
                .resolvedByAdminName(resolvedByAdmin != null ? safe(resolvedByAdmin.getFullName()) : "-")
                .bookingId(booking != null ? booking.getId() : null)
                .paymentId(payment != null ? payment.getId() : null)
                .agreementId(agreement != null ? agreement.getId() : null)
                .projectId(project != null ? project.getId() : null)
                .plotId(plot != null ? plot.getId() : null)
                .projectName(projectName)
                .plotNumber(plotNumber)
                .builderName(builderName)
                .investorName(investorName)
                .createdAt(complaint.getCreatedAt())
                .reviewedAt(complaint.getReviewedAt())
                .resolvedAt(complaint.getResolvedAt())
                .rejectedAt(complaint.getRejectedAt())
                .build();
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}