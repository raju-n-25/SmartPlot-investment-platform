package com.example.spip_draft.repository;

import com.example.spip_draft.entity.MeetingRequest;
import com.example.spip_draft.enums.MeetingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface MeetingRequestRepository extends JpaRepository<MeetingRequest, Long> {

    List<MeetingRequest> findByBuilderIdOrderByCreatedAtDesc(Long builderId);

    List<MeetingRequest> findByInvestorIdOrderByCreatedAtDesc(Long investorId);

    long countByBuilderIdAndStatus(Long builderId, MeetingStatus status);

    long countByInvestorIdAndStatus(Long investorId, MeetingStatus status);

    List<MeetingRequest> findByStatusAndScheduledAtBetweenAndReminderSentFalse(
            MeetingStatus status,
            LocalDateTime start,
            LocalDateTime end
    );

    boolean existsByPlotIdAndStatusIn(Long plotId, Collection<MeetingStatus> statuses);

    boolean existsByBookingIdAndStatusIn(Long bookingId, Collection<MeetingStatus> statuses);
}