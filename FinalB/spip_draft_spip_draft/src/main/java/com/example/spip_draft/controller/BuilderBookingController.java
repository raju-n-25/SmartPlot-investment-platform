package com.example.spip_draft.controller;

import com.example.spip_draft.dto.booking.BookingResponse;
import com.example.spip_draft.dto.booking.BuilderBookingActionRequest;
import com.example.spip_draft.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/builder/bookings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BuilderBookingController {

    private final BookingService bookingService;

    @GetMapping
    public ResponseEntity<java.util.List<BookingResponse>> getBuilderBookings() {
        return ResponseEntity.ok(bookingService.getBuilderBookings());
    }

    @PostMapping("/{bookingId}/approve")
    public ResponseEntity<BookingResponse> approveBooking(@PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingService.approveBookingByBuilder(bookingId));
    }

    @PostMapping("/{bookingId}/reject")
    public ResponseEntity<BookingResponse> rejectBooking(
            @PathVariable Long bookingId,
            @RequestBody(required = false) BuilderBookingActionRequest request
    ) {
        String note = request != null ? request.getNote() : null;
        return ResponseEntity.ok(bookingService.rejectBookingByBuilder(bookingId, note));
    }

    @PostMapping("/{bookingId}/re-approve")
    public ResponseEntity<BookingResponse> reApproveBooking(
            @PathVariable Long bookingId,
            @RequestBody(required = false) BuilderBookingActionRequest request
    ) {
        String note = request != null ? request.getNote() : null;
        return ResponseEntity.ok(bookingService.reApproveBookingByBuilder(bookingId, note));
    }
}