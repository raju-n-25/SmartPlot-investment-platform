package com.example.spip_draft.scheduler;

import com.example.spip_draft.entity.MeetingRequest;
import com.example.spip_draft.entity.Notification;
import com.example.spip_draft.enums.MeetingStatus;
import com.example.spip_draft.repository.MeetingRequestRepository;
import com.example.spip_draft.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MeetingReminderScheduler {

    private final MeetingRequestRepository meetingRequestRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 */10 * * * *")
    public void sendMeetingReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next30Minutes = now.plusMinutes(30);

        List<MeetingRequest> meetings =
                meetingRequestRepository.findByStatusAndScheduledAtBetweenAndReminderSentFalse(
                        MeetingStatus.APPROVED,
                        now,
                        next30Minutes
                );

        for (MeetingRequest meeting : meetings) {
            Notification investorNotification = new Notification();
            investorNotification.setUser(meeting.getInvestor().getUser());
            investorNotification.setTitle("Meeting reminder");
            investorNotification.setMessage("Your meeting is scheduled soon.");
            notificationService.save(investorNotification);

            Notification builderNotification = new Notification();
            builderNotification.setUser(meeting.getBuilder().getUser());
            builderNotification.setTitle("Meeting reminder");
            builderNotification.setMessage("Your investor meeting is scheduled soon.");
            notificationService.save(builderNotification);

            meeting.setReminderSent(true);
            meetingRequestRepository.save(meeting);
        }
    }
}