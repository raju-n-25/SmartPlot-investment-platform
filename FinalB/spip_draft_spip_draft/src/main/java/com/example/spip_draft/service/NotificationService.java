package com.example.spip_draft.service;

import com.example.spip_draft.entity.Booking;

import com.example.spip_draft.entity.Builder;
import com.example.spip_draft.entity.Investor;
import com.example.spip_draft.entity.MeetingRequest;
import com.example.spip_draft.entity.Notification;
import com.example.spip_draft.entity.User;
import com.example.spip_draft.repository.NotificationRepository;
import com.example.spip_draft.repository.UserRepository;
import com.example.spip_draft.util.SecurityUtil;
import com.example.spip_draft.entity.Payment;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public Notification save(Notification notification) {
        return saveAndPush(notification);
    }

    private Notification saveAndPush(Notification notification) {
        Notification saved = notificationRepository.save(notification);
        pushLive(saved);
        return saved;
    }

    private void pushLive(Notification notification) {
        if (notification == null || notification.getUser() == null || notification.getUser().getId() == null) {
            return;
        }

        messagingTemplate.convertAndSend(
                "/topic/notifications/" + notification.getUser().getId(),
                notification
        );
    }

    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public List<Notification> getMyNotifications() {
        Long userId = SecurityUtil.getCurrentUserId();
        return getUserNotifications(userId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount() {
        Long userId = SecurityUtil.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        Long currentUserId = SecurityUtil.getCurrentUserId();

        if (notification.getUser() == null || !notification.getUser().getId().equals(currentUserId)) {
            throw new IllegalStateException("Not allowed");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    public void notifyBuilderTokenPaid(Booking booking) {
        Builder builder = booking.getPlot().getProject().getBuilder();
        if (builder == null || builder.getUser() == null) return;

        String plotNumber = booking.getPlot() != null ? booking.getPlot().getPlotNumber() : "N/A";

        Notification n = new Notification();
        n.setUser(builder.getUser());
        n.setTitle("Token Payment Received");
        n.setMessage(
                "Investor " + booking.getInvestor().getUser().getFullName()
                        + " paid token for Plot " + plotNumber
                        + ". Please review and approve or reject the booking."
        );
        n.setType("TOKEN_PAID");
        n.setReferenceType("BOOKING");
        n.setReferenceId(booking.getId());
        n.setIsRead(false);

        saveAndPush(n);
    }

    public void notifyBuilderFinalPaymentDone(Booking booking) {
        Builder builder = booking.getPlot().getProject().getBuilder();
        if (builder == null || builder.getUser() == null) return;

        String plotNumber = booking.getPlot() != null ? booking.getPlot().getPlotNumber() : "N/A";

        Notification n = new Notification();
        n.setUser(builder.getUser());
        n.setTitle("Final Payment Completed");
        n.setMessage(
                "Investor " + booking.getInvestor().getUser().getFullName()
                        + " completed final payment for Plot " + plotNumber
                        + ". Agreement review and escrow flow can now proceed."
        );
        n.setType("FINAL_PAYMENT_COMPLETED");
        n.setReferenceType("BOOKING");
        n.setReferenceId(booking.getId());
        n.setIsRead(false);

        saveAndPush(n);
    }

    public void notifyBuilderAgreementSent(Booking booking) {
        Builder builder = booking.getPlot().getProject().getBuilder();
        if (builder == null || builder.getUser() == null) return;

        Notification n = new Notification();
        n.setUser(builder.getUser());
        n.setTitle("Agreement Sent");
        n.setMessage("Agreement has been sent to the investor. Builder-side review visibility is now available.");
        n.setType("AGREEMENT_SENT");
        n.setReferenceType("BOOKING");
        n.setReferenceId(booking.getId());
        n.setIsRead(false);

        saveAndPush(n);
    }

    public void notifyBuilderAgreementSigned(Booking booking) {
        Builder builder = booking.getPlot().getProject().getBuilder();
        if (builder == null || builder.getUser() == null) return;

        Notification n = new Notification();
        n.setUser(builder.getUser());
        n.setTitle("Agreement Signed");
        n.setMessage("Agreement for booking #" + booking.getId() + " has been fully signed.");
        n.setType("AGREEMENT_SIGNED");
        n.setReferenceType("BOOKING");
        n.setReferenceId(booking.getId());
        n.setIsRead(false);

        saveAndPush(n);
    }

    public void notifyInvestorBuilderApproved(Booking booking) {
        Investor investor = booking.getInvestor();
        if (investor == null || investor.getUser() == null) return;

        String plotNumber = booking.getPlot() != null ? booking.getPlot().getPlotNumber() : "N/A";

        Notification n = new Notification();
        n.setUser(investor.getUser());
        n.setTitle("Booking Approved");
        n.setMessage("Your booking for Plot " + plotNumber + " is approved. You can now proceed to final payment.");
        n.setType("BUILDER_APPROVED");
        n.setReferenceType("BOOKING");
        n.setReferenceId(booking.getId());
        n.setIsRead(false);

        saveAndPush(n);
    }

    public void notifyInvestorBuilderRejected(Booking booking) {
        Investor investor = booking.getInvestor();
        if (investor == null || investor.getUser() == null) return;

        String plotNumber = booking.getPlot() != null ? booking.getPlot().getPlotNumber() : "N/A";

        Notification n = new Notification();
        n.setUser(investor.getUser());
        n.setTitle("Booking Not Approved");
        n.setMessage("Your booking for Plot " + plotNumber + " was not approved. You can request a meeting or refund based on the stage.");
        n.setType("BUILDER_REJECTED");
        n.setReferenceType("BOOKING");
        n.setReferenceId(booking.getId());
        n.setIsRead(false);

        saveAndPush(n);
    }

    public void notifyBuilderMeetingRequested(MeetingRequest meeting) {
        if (meeting.getBuilder() == null || meeting.getBuilder().getUser() == null) return;

        String plotNumber = meeting.getPlot() != null ? meeting.getPlot().getPlotNumber() : "General";

        Notification n = new Notification();
        n.setUser(meeting.getBuilder().getUser());
        n.setTitle("Meeting Request");
        n.setMessage("Investor requested a meeting regarding Plot " + plotNumber);
        n.setType("MEETING_REQUESTED");
        n.setReferenceType("MEETING");
        n.setReferenceId(meeting.getId());
        n.setIsRead(false);

        saveAndPush(n);
    }

    public void notifyInvestorMeetingApproved(MeetingRequest meeting) {
        if (meeting.getInvestor() == null || meeting.getInvestor().getUser() == null) return;

        Notification n = new Notification();
        n.setUser(meeting.getInvestor().getUser());
        n.setTitle("Meeting Approved");
        n.setMessage("Your meeting request has been approved.");
        n.setType("MEETING_APPROVED");
        n.setReferenceType("MEETING");
        n.setReferenceId(meeting.getId());
        n.setIsRead(false);

        saveAndPush(n);
    }

    public void notifyInvestorMeetingRejected(MeetingRequest meeting) {
        if (meeting.getInvestor() == null || meeting.getInvestor().getUser() == null) return;

        Notification n = new Notification();
        n.setUser(meeting.getInvestor().getUser());
        n.setTitle("Meeting Rejected");
        n.setMessage("Your meeting request has been rejected.");
        n.setType("MEETING_REJECTED");
        n.setReferenceType("MEETING");
        n.setReferenceId(meeting.getId());
        n.setIsRead(false);

        saveAndPush(n);
    }

    public void notifyInvestorAgreementGenerated(Booking booking) {
        Investor investor = booking.getInvestor();
        if (investor == null || investor.getUser() == null) return;

        Notification n = new Notification();
        n.setUser(investor.getUser());
        n.setTitle("Agreement Generated");
        n.setMessage("Your BrickSquare agreement is ready. Open the agreement, review it, then accept or reject with refund.");
        n.setType("AGREEMENT_GENERATED");
        n.setReferenceType("BOOKING");
        n.setReferenceId(booking.getId());
        n.setIsRead(false);

        saveAndPush(n);
    }

    public void notifyBuilderAgreementGenerated(Booking booking) {
        Builder builder = booking.getPlot().getProject().getBuilder();
        if (builder == null || builder.getUser() == null) return;

        Notification n = new Notification();
        n.setUser(builder.getUser());
        n.setTitle("Agreement Generated");
        n.setMessage("Agreement has been generated for booking #" + booking.getId() + ". Builder-side review card can now display the document status.");
        n.setType("AGREEMENT_GENERATED");
        n.setReferenceType("BOOKING");
        n.setReferenceId(booking.getId());
        n.setIsRead(false);

        saveAndPush(n);
    }

    public void notifyInvestorAgreementSent(Booking booking) {
        Investor investor = booking.getInvestor();
        if (investor == null || investor.getUser() == null) return;

        Notification n = new Notification();
        n.setUser(investor.getUser());
        n.setTitle("Agreement Sent");
        n.setMessage("Agreement has been sent. Please open the document first, then accept or reject it.");
        n.setType("AGREEMENT_SENT");
        n.setReferenceType("BOOKING");
        n.setReferenceId(booking.getId());
        n.setIsRead(false);

        saveAndPush(n);
    }

    public void notifyBuilderInvestorAcceptedAgreement(Booking booking) {
        Builder builder = booking.getPlot().getProject().getBuilder();
        if (builder == null || builder.getUser() == null) return;

        Notification n = new Notification();
        n.setUser(builder.getUser());
        n.setTitle("Investor Accepted Agreement");
        n.setMessage("Investor has accepted the agreement for booking #" + booking.getId() + ". Open, review, and accept before builder signature.");
        n.setType("INVESTOR_ACCEPTED_AGREEMENT");
        n.setReferenceType("BOOKING");
        n.setReferenceId(booking.getId());
        n.setIsRead(false);

        saveAndPush(n);
    }

    public void notifyInvestorBuilderAcceptedAgreement(Booking booking) {
        Investor investor = booking.getInvestor();
        if (investor == null || investor.getUser() == null) return;

        Notification n = new Notification();
        n.setUser(investor.getUser());
        n.setTitle("Builder Accepted Agreement");
        n.setMessage("Builder has accepted the agreement. Investor signature step is now available after document review.");
        n.setType("BUILDER_ACCEPTED_AGREEMENT");
        n.setReferenceType("BOOKING");
        n.setReferenceId(booking.getId());
        n.setIsRead(false);

        saveAndPush(n);
    }

    public void notifyBuilderInvestorSignedAgreement(Booking booking) {
        Builder builder = booking.getPlot().getProject().getBuilder();
        if (builder == null || builder.getUser() == null) return;

        Notification n = new Notification();
        n.setUser(builder.getUser());
        n.setTitle("Investor Signed Agreement");
        n.setMessage("Investor has signed the agreement for booking #" + booking.getId() + ". Builder signature is pending.");
        n.setType("INVESTOR_SIGNED_AGREEMENT");
        n.setReferenceType("BOOKING");
        n.setReferenceId(booking.getId());
        n.setIsRead(false);

        saveAndPush(n);
    }

    public void notifyInvestorBuilderSignedAgreement(Booking booking) {
        Investor investor = booking.getInvestor();
        if (investor == null || investor.getUser() == null) return;

        Notification n = new Notification();
        n.setUser(investor.getUser());
        n.setTitle("Builder Signed Agreement");
        n.setMessage("Builder has signed the agreement for booking #" + booking.getId() + ". If your signature is done, the executed agreement will be ready.");
        n.setType("BUILDER_SIGNED_AGREEMENT");
        n.setReferenceType("BOOKING");
        n.setReferenceId(booking.getId());
        n.setIsRead(false);

        saveAndPush(n);
    }

    public void notifyAgreementCompleted(Booking booking) {
        Builder builder = booking.getPlot().getProject().getBuilder();
        Investor investor = booking.getInvestor();

        if (builder != null && builder.getUser() != null) {
            Notification nb = new Notification();
            nb.setUser(builder.getUser());
            nb.setTitle("Agreement Completed");
            nb.setMessage("Agreement is fully completed for booking #" + booking.getId() + ". The executed agreement PDF is now the final reference document.");
            nb.setType("AGREEMENT_COMPLETED");
            nb.setReferenceType("BOOKING");
            nb.setReferenceId(booking.getId());
            nb.setIsRead(false);
            saveAndPush(nb);
        }

        if (investor != null && investor.getUser() != null) {
            Notification ni = new Notification();
            ni.setUser(investor.getUser());
            ni.setTitle("Agreement Completed");
            ni.setMessage("Your BrickSquare agreement is fully completed for booking #" + booking.getId() + ". You can now download the executed agreement PDF.");
            ni.setType("AGREEMENT_COMPLETED");
            ni.setReferenceType("BOOKING");
            ni.setReferenceId(booking.getId());
            ni.setIsRead(false);
            saveAndPush(ni);
        }
    }

    public void notifyAgreementRejected(Booking booking, String reason) {
        Builder builder = booking.getPlot().getProject().getBuilder();
        Investor investor = booking.getInvestor();

        if (builder != null && builder.getUser() != null) {
            Notification nb = new Notification();
            nb.setUser(builder.getUser());
            nb.setTitle("Agreement Rejected");
            nb.setMessage("Investor rejected the agreement for booking #" + booking.getId() + ". Reason: " + (reason == null || reason.isBlank() ? "Not specified" : reason));
            nb.setType("AGREEMENT_REJECTED");
            nb.setReferenceType("BOOKING");
            nb.setReferenceId(booking.getId());
            nb.setIsRead(false);
            saveAndPush(nb);
        }

        if (investor != null && investor.getUser() != null) {
            Notification ni = new Notification();
            ni.setUser(investor.getUser());
            ni.setTitle("Agreement Rejected");
            ni.setMessage("Agreement rejection has been recorded for booking #" + booking.getId() + ". Refund flow has been initiated.");
            ni.setType("AGREEMENT_REJECTED");
            ni.setReferenceType("BOOKING");
            ni.setReferenceId(booking.getId());
            ni.setIsRead(false);
            saveAndPush(ni);
        }
    }

    public void notifyInvestorMeetingCompletedActionRequired(MeetingRequest meeting) {
        if (meeting.getInvestor() == null || meeting.getInvestor().getUser() == null) return;

        Notification n = new Notification();
        n.setUser(meeting.getInvestor().getUser());
        n.setTitle("Meeting Completed");
        n.setMessage("Your meeting is completed. You can now request another meeting, proceed to buy, or release the plot back to marketplace.");
        n.setType(com.example.spip_draft.enums.NotificationType.MEETING_COMPLETED);
        n.setReferenceType("MEETING");
        n.setReferenceId(meeting.getId());
        n.setIsRead(false);

        saveAndPush(n);
    }

    public void notifyBuilderFollowUpMeetingRequested(MeetingRequest meeting) {
        if (meeting.getBuilder() == null || meeting.getBuilder().getUser() == null) return;

        Notification n = new Notification();
        n.setUser(meeting.getBuilder().getUser());
        n.setTitle("Follow-up Meeting Requested");
        n.setMessage("Investor requested another meeting for Plot "
                + (meeting.getPlot() != null ? meeting.getPlot().getPlotNumber() : "General")
                + ".");
        n.setType(com.example.spip_draft.enums.NotificationType.FOLLOW_UP_MEETING_REQUESTED);
        n.setReferenceType("MEETING");
        n.setReferenceId(meeting.getId());
        n.setIsRead(false);

        saveAndPush(n);
    }

    public void notifyBuilderPlotReturnedToMarketplace(MeetingRequest meeting) {
        if (meeting.getBuilder() == null || meeting.getBuilder().getUser() == null) return;

        Notification n = new Notification();
        n.setUser(meeting.getBuilder().getUser());
        n.setTitle("Plot Back on Marketplace");
        n.setMessage("The investor did not proceed further. Plot "
                + (meeting.getPlot() != null ? meeting.getPlot().getPlotNumber() : "")
                + " is now back on marketplace.");
        n.setType(com.example.spip_draft.enums.NotificationType.PLOT_RELEASED_TO_MARKETPLACE);
        n.setReferenceType("MEETING");
        n.setReferenceId(meeting.getId());
        n.setIsRead(false);

        saveAndPush(n);
    }

    public void notifyBuilderMeetingBuyRequested(MeetingRequest meeting) {
        if (meeting.getBuilder() == null || meeting.getBuilder().getUser() == null) return;

        Notification n = new Notification();
        n.setUser(meeting.getBuilder().getUser());
        n.setTitle("Investor Chose to Buy");
        n.setMessage("Investor chose to proceed with buying Plot "
                + (meeting.getPlot() != null ? meeting.getPlot().getPlotNumber() : "")
                + " after the meeting.");
        n.setType(com.example.spip_draft.enums.NotificationType.MEETING_BUY_REQUESTED);
        n.setReferenceType("MEETING");
        n.setReferenceId(meeting.getId());
        n.setIsRead(false);

        saveAndPush(n);
    }
    
    public void notifyEscrowFrozenToInvestor(Payment payment, String reason, String actionRequired, LocalDateTime deadline) {
        if (payment == null || payment.getBooking() == null) return;

        Investor investor = payment.getBooking().getInvestor();
        if (investor == null || investor.getUser() == null) return;

        Notification n = new Notification();
        n.setUser(investor.getUser());
        n.setTitle("Escrow Temporarily Frozen");
        n.setMessage(
                "Your escrow for booking #" + payment.getBooking().getId()
                        + " has been temporarily frozen. Reason: "
                        + safe(reason)
                        + ". Required action: "
                        + safe(actionRequired)
                        + ". Deadline: "
                        + formatDeadline(deadline)
                        + ". If the issue is not resolved before the deadline, refund/cancellation action may be processed."
        );
        n.setType("ESCROW_FROZEN");
        n.setReferenceType("BOOKING");
        n.setReferenceId(payment.getBooking().getId());
        n.setIsRead(false);

        saveAndPush(n);
    }

    public void notifyEscrowFrozenToBuilder(Payment payment, String reason, String actionRequired, LocalDateTime deadline) {
        if (payment == null || payment.getBooking() == null || payment.getBooking().getPlot() == null) return;

        Builder builder = payment.getBooking().getPlot().getProject().getBuilder();
        if (builder == null || builder.getUser() == null) return;

        Notification n = new Notification();
        n.setUser(builder.getUser());
        n.setTitle("Escrow Release Paused");
        n.setMessage(
                "Escrow release for booking #" + payment.getBooking().getId()
                        + " has been paused by admin. Reason: "
                        + safe(reason)
                        + ". Required action: "
                        + safe(actionRequired)
                        + ". Deadline: "
                        + formatDeadline(deadline)
                        + ". Please complete the pending requirement so the escrow can move back to review."
        );
        n.setType("ESCROW_FROZEN");
        n.setReferenceType("BOOKING");
        n.setReferenceId(payment.getBooking().getId());
        n.setIsRead(false);

        saveAndPush(n);
    }

    public void notifyEscrowUnfrozenToInvestor(Payment payment) {
        if (payment == null || payment.getBooking() == null) return;

        Investor investor = payment.getBooking().getInvestor();
        if (investor == null || investor.getUser() == null) return;

        Notification n = new Notification();
        n.setUser(investor.getUser());
        n.setTitle("Escrow Moved Back to Review");
        n.setMessage(
                "Escrow for booking #" + payment.getBooking().getId()
                        + " has been unfrozen and moved back to admin review. Release will happen only after all escrow rules are satisfied."
        );
        n.setType("ESCROW_UNFROZEN");
        n.setReferenceType("BOOKING");
        n.setReferenceId(payment.getBooking().getId());
        n.setIsRead(false);

        saveAndPush(n);
    }

    public void notifyEscrowUnfrozenToBuilder(Payment payment) {
        if (payment == null || payment.getBooking() == null || payment.getBooking().getPlot() == null) return;

        Builder builder = payment.getBooking().getPlot().getProject().getBuilder();
        if (builder == null || builder.getUser() == null) return;

        Notification n = new Notification();
        n.setUser(builder.getUser());
        n.setTitle("Escrow Review Resumed");
        n.setMessage(
                "Escrow for booking #" + payment.getBooking().getId()
                        + " has been unfrozen and moved back to review. Complete any pending KYC/agreement/project requirement if still pending."
        );
        n.setType("ESCROW_UNFROZEN");
        n.setReferenceType("BOOKING");
        n.setReferenceId(payment.getBooking().getId());
        n.setIsRead(false);

        saveAndPush(n);
    }

    public void notifyEscrowAutoRefundedToInvestor(Payment payment, String reason) {
        if (payment == null || payment.getBooking() == null) return;

        Investor investor = payment.getBooking().getInvestor();
        if (investor == null || investor.getUser() == null) return;

        Notification n = new Notification();
        n.setUser(investor.getUser());
        n.setTitle("Refund Processed");
        n.setMessage(
                "Refund has been processed for booking #" + payment.getBooking().getId()
                        + ". Reason: " + safe(reason)
                        + ". The plot will be released back according to platform rules."
        );
        n.setType("ESCROW_AUTO_REFUNDED");
        n.setReferenceType("BOOKING");
        n.setReferenceId(payment.getBooking().getId());
        n.setIsRead(false);

        saveAndPush(n);
    }

    public void notifyEscrowAutoRefundedToBuilder(Payment payment, String reason) {
        if (payment == null || payment.getBooking() == null || payment.getBooking().getPlot() == null) return;

        Builder builder = payment.getBooking().getPlot().getProject().getBuilder();
        if (builder == null || builder.getUser() == null) return;

        Notification n = new Notification();
        n.setUser(builder.getUser());
        n.setTitle("Booking Cancelled and Refunded");
        n.setMessage(
                "Booking #" + payment.getBooking().getId()
                        + " has been cancelled and refunded because the escrow freeze issue was not resolved. Reason: "
                        + safe(reason)
        );
        n.setType("ESCROW_AUTO_REFUNDED");
        n.setReferenceType("BOOKING");
        n.setReferenceId(payment.getBooking().getId());
        n.setIsRead(false);

        saveAndPush(n);
    }

    public void notifyEscrowFreezeDeadlineExpiredToAdmin(Payment payment) {
        if (payment == null || payment.getBooking() == null) return;

        List<User> admins = userRepository.findAll()
                .stream()
                .filter(user -> user.getRole() != null && String.valueOf(user.getRole()).contains("ADMIN"))
                .toList();

        for (User admin : admins) {
            Notification n = new Notification();
            n.setUser(admin);
            n.setTitle("Frozen Escrow Deadline Expired");
            n.setMessage(
                    "Frozen escrow deadline expired for booking #"
                            + payment.getBooking().getId()
                            + ". System will process refund/cancellation if no manual action is taken."
            );
            n.setType("ESCROW_FREEZE_DEADLINE_EXPIRED");
            n.setReferenceType("BOOKING");
            n.setReferenceId(payment.getBooking().getId());
            n.setIsRead(false);

            saveAndPush(n);
        }
    }
    
    public void notifyAgreementExpired(Booking booking) {
        if (booking == null) return;

        Builder builder = booking.getPlot() != null
                && booking.getPlot().getProject() != null
                ? booking.getPlot().getProject().getBuilder()
                : null;

        Investor investor = booking.getInvestor();

        if (builder != null && builder.getUser() != null) {
            Notification nb = new Notification();
            nb.setUser(builder.getUser());
            nb.setTitle("Agreement Expired");
            nb.setMessage("Agreement for booking #" + booking.getId()
                    + " expired before completion. You can regenerate the agreement if the deal should continue.");
            nb.setType("AGREEMENT_EXPIRED");
            nb.setReferenceType("BOOKING");
            nb.setReferenceId(booking.getId());
            nb.setIsRead(false);
            saveAndPush(nb);
        }

        if (investor != null && investor.getUser() != null) {
            Notification ni = new Notification();
            ni.setUser(investor.getUser());
            ni.setTitle("Agreement Expired");
            ni.setMessage("Your agreement for booking #" + booking.getId()
                    + " expired before completion. Please contact the builder or wait for regenerated agreement.");
            ni.setType("AGREEMENT_EXPIRED");
            ni.setReferenceType("BOOKING");
            ni.setReferenceId(booking.getId());
            ni.setIsRead(false);
            saveAndPush(ni);
        }
    }

    public void notifyAgreementRegenerated(Booking booking) {
        if (booking == null) return;

        Builder builder = booking.getPlot() != null
                && booking.getPlot().getProject() != null
                ? booking.getPlot().getProject().getBuilder()
                : null;

        Investor investor = booking.getInvestor();

        if (builder != null && builder.getUser() != null) {
            Notification nb = new Notification();
            nb.setUser(builder.getUser());
            nb.setTitle("Agreement Regenerated");
            nb.setMessage("Agreement regenerated successfully for booking #" + booking.getId()
                    + ". Send it again for review.");
            nb.setType("AGREEMENT_REGENERATED");
            nb.setReferenceType("BOOKING");
            nb.setReferenceId(booking.getId());
            nb.setIsRead(false);
            saveAndPush(nb);
        }

        if (investor != null && investor.getUser() != null) {
            Notification ni = new Notification();
            ni.setUser(investor.getUser());
            ni.setTitle("Agreement Regenerated");
            ni.setMessage("Agreement has been regenerated for booking #" + booking.getId()
                    + ". Please review it again once it is sent.");
            ni.setType("AGREEMENT_REGENERATED");
            ni.setReferenceType("BOOKING");
            ni.setReferenceId(booking.getId());
            ni.setIsRead(false);
            saveAndPush(ni);
        }
    }
    

    private String safe(String value) {
        return value == null || value.isBlank() ? "Not specified" : value;
    }

    private String formatDeadline(LocalDateTime deadline) {
        return deadline == null ? "Not specified" : deadline.toString();
    }
    
    public void notifyAdminsUnfreezeRequested(Payment payment) {
        if (payment == null || payment.getBooking() == null) return;

        Booking booking = payment.getBooking();

        Long bookingId = booking.getId();

        String investorName = "Investor";
        String projectName = "Project not available";
        String plotNumber = "N/A";
        String builderName = "Builder not available";

        try {
            if (booking.getInvestor() != null && booking.getInvestor().getUser() != null) {
                investorName = booking.getInvestor().getUser().getFullName();
            }
        } catch (Exception ignored) {
            investorName = "Investor";
        }

        try {
            if (booking.getPlot() != null) {
                plotNumber = booking.getPlot().getPlotNumber() != null
                        ? booking.getPlot().getPlotNumber()
                        : "N/A";

                if (booking.getPlot().getProject() != null) {
                    projectName = booking.getPlot().getProject().getProjectName() != null
                            ? booking.getPlot().getProject().getProjectName()
                            : "Project not available";

                    if (booking.getPlot().getProject().getBuilder() != null
                            && booking.getPlot().getProject().getBuilder().getUser() != null) {
                        builderName = booking.getPlot().getProject().getBuilder().getUser().getFullName();
                    }
                }
            }
        } catch (Exception ignored) {
            plotNumber = plotNumber == null ? "N/A" : plotNumber;
            projectName = projectName == null ? "Project not available" : projectName;
            builderName = builderName == null ? "Builder not available" : builderName;
        }

        String reason = safe(payment.getReviewReason());
        String actionRequired = safe(payment.getFreezeActionRequired());
        String deadline = formatDeadline(payment.getFreezeDeadlineAt());
        String investorNote = safe(payment.getUnfreezeRequestNote());

        List<User> admins = userRepository.findAll()
                .stream()
                .filter(user -> user.getRole() != null && user.getRole().contains("ADMIN"))
                .toList();

        for (User admin : admins) {
            Notification n = new Notification();
            n.setUser(admin);
            n.setTitle("Unfreeze Review Requested");
            n.setMessage(
                    investorName
                            + " requested admin review for booking #"
                            + bookingId
                            + ". Project: " + projectName
                            + ", Plot: " + plotNumber
                            + ", Builder: " + builderName
                            + ". Freeze reason: " + reason
                            + ". Required action: " + actionRequired
                            + ". Deadline: " + deadline
                            + ". Investor note: " + investorNote
            );
            n.setType("ESCROW_UNFREEZE_REQUESTED");
            n.setReferenceType("BOOKING");
            n.setReferenceId(bookingId);
            n.setIsRead(false);

            saveAndPush(n);
        }

        messagingTemplate.convertAndSend(
                "/topic/admin/escrow",
                "ESCROW_UNFREEZE_REQUESTED:" + bookingId
        );
    }
}