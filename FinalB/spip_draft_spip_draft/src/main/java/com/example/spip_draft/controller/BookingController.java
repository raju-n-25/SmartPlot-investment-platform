package com.example.spip_draft.controller;

import com.example.spip_draft.dto.booking.BookingRequest;
import com.example.spip_draft.dto.booking.BookingResponse;
import com.example.spip_draft.dto.booking.BuilderBookingActionRequest;
import com.example.spip_draft.dto.booking.PaymentOptionRequest;
import com.example.spip_draft.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody BookingRequest request
    ) {
        return ResponseEntity.ok(bookingService.createBooking(request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<BookingResponse>> getMyBookings() {
        return ResponseEntity.ok(bookingService.getInvestorBookings());
    }

    @GetMapping("/builder")
    public ResponseEntity<List<BookingResponse>> getBuilderBookings() {
        return ResponseEntity.ok(bookingService.getBuilderBookings());
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<String> cancelBooking(@PathVariable Long id) {
        bookingService.cancelBooking(id);
        return ResponseEntity.ok("Booking cancelled successfully");
    }

    @PostMapping("/{bookingId}/payment-option")
    public ResponseEntity<BookingResponse> choosePaymentOption(
            @PathVariable Long bookingId,
            @Valid @RequestBody PaymentOptionRequest request
    ) {
        return ResponseEntity.ok(bookingService.choosePaymentOption(bookingId, request));
    }

    @PostMapping("/{bookingId}/request-reapproval")
    public ResponseEntity<BookingResponse> requestReapproval(
            @PathVariable Long bookingId,
            @RequestBody(required = false) BuilderBookingActionRequest request
    ) {
        String note = request != null ? request.getNote() : null;
        return ResponseEntity.ok(bookingService.requestReapproval(bookingId, note));
    }
}