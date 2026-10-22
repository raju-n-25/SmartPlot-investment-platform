package com.example.spip_draft.controller;

import com.example.spip_draft.dto.meeting.CreateMeetingRequest;
import com.example.spip_draft.dto.meeting.MeetingApprovalRequest;
import com.example.spip_draft.dto.meeting.MeetingRescheduleRequest;
import com.example.spip_draft.dto.meeting.MeetingResponse;
import com.example.spip_draft.service.MeetingRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingRequestService meetingRequestService;

    @PostMapping("/api/investors/meetings")
    public ResponseEntity<MeetingResponse> createMeetingRequest(
            @Valid @RequestBody CreateMeetingRequest request
    ) {
        return ResponseEntity.ok(meetingRequestService.createMeetingRequest(request));
    }

    @GetMapping("/api/investors/meetings")
    public ResponseEntity<List<MeetingResponse>> getInvestorMeetings() {
        return ResponseEntity.ok(meetingRequestService.getInvestorMeetings());
    }

    @GetMapping("/api/builder/meetings")
    public ResponseEntity<List<MeetingResponse>> getBuilderMeetings() {
        return ResponseEntity.ok(meetingRequestService.getBuilderMeetings());
    }

    @PostMapping("/api/builder/meetings/{meetingId}/decision")
    public ResponseEntity<MeetingResponse> approveOrReject(
            @PathVariable Long meetingId,
            @Valid @RequestBody MeetingApprovalRequest request
    ) {
        return ResponseEntity.ok(meetingRequestService.approveOrReject(meetingId, request));
    }

    @PutMapping("/api/builder/meetings/{meetingId}/reject-after-meet")
    public ResponseEntity<MeetingResponse> rejectAfterMeeting(
            @PathVariable Long meetingId,
            @RequestBody MeetingApprovalRequest request
    ) {
        return ResponseEntity.ok(meetingRequestService.rejectAfterMeeting(meetingId, request));
    }

    @GetMapping("/api/meetings/{meetingId}/join")
    public ResponseEntity<Void> joinMeeting(@PathVariable Long meetingId) {
        return meetingRequestService.joinMeeting(meetingId);
    }

    @PutMapping("/api/investors/meetings/{meetingId}/cancel")
    public ResponseEntity<MeetingResponse> cancelMeeting(@PathVariable Long meetingId) {
        return ResponseEntity.ok(meetingRequestService.cancelMeeting(meetingId));
    }

    @PutMapping("/api/builder/meetings/{meetingId}/complete")
    public ResponseEntity<MeetingResponse> markMeetingCompleted(@PathVariable Long meetingId) {
        return ResponseEntity.ok(meetingRequestService.markMeetingCompleted(meetingId));
    }

    @PutMapping("/api/investors/meetings/{meetingId}/follow-up")
    public ResponseEntity<MeetingResponse> requestFollowUpMeeting(@PathVariable Long meetingId) {
        return ResponseEntity.ok(meetingRequestService.requestFollowUpMeeting(meetingId));
    }

    @PutMapping("/api/investors/meetings/{meetingId}/buy")
    public ResponseEntity<MeetingResponse> proceedToBuy(@PathVariable Long meetingId) {
        return ResponseEntity.ok(meetingRequestService.proceedToBuy(meetingId));
    }

    @PutMapping("/api/investors/meetings/{meetingId}/release")
    public ResponseEntity<MeetingResponse> releasePlotToMarketplace(@PathVariable Long meetingId) {
        return ResponseEntity.ok(meetingRequestService.releasePlotToMarketplace(meetingId));
    }

    @PutMapping("/api/investors/meetings/{meetingId}/reschedule-request")
    public ResponseEntity<MeetingResponse> requestReschedule(
            @PathVariable Long meetingId,
            @Valid @RequestBody MeetingRescheduleRequest request
    ) {
        return ResponseEntity.ok(meetingRequestService.requestReschedule(meetingId, request));
    }

    @PutMapping("/api/builder/meetings/{meetingId}/reschedule-approve")
    public ResponseEntity<MeetingResponse> approveReschedule(@PathVariable Long meetingId) {
        return ResponseEntity.ok(meetingRequestService.approveReschedule(meetingId));
    }

    @PutMapping("/api/builder/meetings/{meetingId}/reschedule-reject")
    public ResponseEntity<MeetingResponse> rejectReschedule(@PathVariable Long meetingId) {
        return ResponseEntity.ok(meetingRequestService.rejectReschedule(meetingId));
    }
}