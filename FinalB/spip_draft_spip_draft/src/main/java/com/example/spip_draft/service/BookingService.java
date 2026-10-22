package com.example.spip_draft.service;

import com.example.spip_draft.dto.booking.BookingRequest;

import com.example.spip_draft.dto.booking.BookingResponse;
import com.example.spip_draft.dto.booking.PaymentOptionRequest;
import com.example.spip_draft.entity.Booking;
import com.example.spip_draft.entity.BookingAgreement;
import com.example.spip_draft.entity.BookingConflictLog;
import com.example.spip_draft.entity.Builder;
import com.example.spip_draft.entity.Investor;
import com.example.spip_draft.entity.Payment;
import com.example.spip_draft.entity.Plot;
import com.example.spip_draft.enums.BookingStatus;
import com.example.spip_draft.exception.ResourceAlreadyExistsException;
import com.example.spip_draft.mapper.BookingMapper;
import com.example.spip_draft.multitenant.TenantContext;
import com.example.spip_draft.repository.BookingAgreementRepository;
import com.example.spip_draft.repository.BookingConflictRepository;
import com.example.spip_draft.repository.BookingRepository;
import com.example.spip_draft.repository.BuilderRepository;
import com.example.spip_draft.repository.InvestorRepository;
import com.example.spip_draft.repository.PaymentRepository;
import com.example.spip_draft.repository.PlotRepository;
import com.example.spip_draft.util.SecurityUtil;
import com.example.spip_draft.enums.AuditAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final PlotRepository plotRepository;
    private final InvestorRepository investorRepository;
    private final BuilderRepository builderRepository;
    private final PaymentRepository paymentRepository;
    private final BookingAgreementRepository bookingAgreementRepository;
    private final BookingConflictRepository conflictRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    

    @Value("${booking.token.expiry:10}")
    private int expiryMinutes;

    @Transactional
    public BookingResponse createBooking(BookingRequest request) {

        Long userId = SecurityUtil.getCurrentUserId();

        Investor investor = investorRepository
                .findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Investor not found"));

        // ✅ NEW: hard backend KYC gate
        if (!investor.isApproved()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Complete investor KYC verification before booking any plot"
            );
        }

        Plot plot = plotRepository.findPlotForUpdate(request.getPlotId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plot not found"));

        Long tenantId = plot.getTenantId();

        if (!plot.isBookable()) {
            saveConflict(
                    plot.getId(),
                    investor.getId(),
                    tenantId,
                    null,
                    "PLOT_NOT_AVAILABLE",
                    "Plot not available for booking"
            );
            throw new ResourceAlreadyExistsException("Plot not available");
        }

        List<Booking> activeBookings = bookingRepository.findActiveBookingsByPlot(plot.getId());
        if (!activeBookings.isEmpty()) {
            Booking existing = activeBookings.get(0);
            saveConflict(
                    plot.getId(),
                    investor.getId(),
                    tenantId,
                    existing.getId(),
                    "ALREADY_BOOKED",
                    "Another active booking already exists for this plot"
            );
            throw new ResourceAlreadyExistsException("Plot already has an active booking");
        }

        BigDecimal tokenAmount = plot.getPrice()
                .multiply(BigDecimal.valueOf(0.01))
                .setScale(2, RoundingMode.HALF_UP);

        LocalDateTime now = LocalDateTime.now();

        Booking booking = new Booking();
        booking.setPlot(plot);
        booking.setInvestor(investor);
        booking.setAmount(tokenAmount);
        booking.setStatus(BookingStatus.TOKEN_BLOCKED);
        booking.setBookingTime(now);
        booking.setTokenExpiry(now.plusMinutes(expiryMinutes));
        booking.setTenantId(tenantId);

        plot.setAvailable(false);
        plot.setBookingStatus(BookingStatus.TOKEN_BLOCKED);
        plot.setBlockedAt(now);

        plotRepository.save(plot);
        Booking saved = bookingRepository.save(booking);
        auditLogService.log(
                AuditAction.BOOKING_CREATED,
                "BOOKING",
                saved.getId(),
                "Investor blocked plot " + plot.getId() + " with token booking",
                null,
                saved.getStatus().name()
        );

        log.info(
                "Booking created | bookingId={} | plotId={} | investorId={} | tenantId={}",
                saved.getId(), plot.getId(), investor.getId(), tenantId
        );

        return BookingMapper.toResponse(saved);
    }

    @Transactional
    public void cancelBooking(Long bookingId) {

        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        validateInvestorOwnsBooking(booking);

        if (booking.getStatus() == BookingStatus.CONFIRMED || booking.getStatus() == BookingStatus.EMI_ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot cancel confirmed or EMI-active booking");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED
                || booking.getStatus() == BookingStatus.EXPIRED
                || booking.getStatus() == BookingStatus.REFUNDED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking already closed");
        }

        Plot plot = plotRepository.findPlotForUpdate(booking.getPlot().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plot not found"));

        String oldStatus = booking.getStatus() != null ? booking.getStatus().name() : null;

        booking.setStatus(BookingStatus.CANCELLED);

        plot.setAvailable(true);
        plot.setBookingStatus(null);
        plot.setBlockedAt(null);

        plotRepository.save(plot);
        bookingRepository.save(booking);

        auditLogService.log(
                AuditAction.BOOKING_CANCELLED,
                "BOOKING",
                booking.getId(),
                "Investor cancelled booking",
                oldStatus,
                BookingStatus.CANCELLED.name()
        );

        log.info("Booking cancelled | bookingId={} | plotId={}", bookingId, plot.getId());
    }
    

    @Transactional(readOnly = true)
    public List<BookingResponse> getInvestorBookings() {

        Long userId = SecurityUtil.getCurrentUserId();

        Investor investor = investorRepository
                .findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Investor not found"));

        return bookingRepository.findInvestorBookings(investor.getId())
                .stream()
                .map(this::enrichBookingResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getBuilderBookings() {

        Long tenantId = TenantContext.getTenantId();

        Builder builder = builderRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Builder not found"));

        return bookingRepository.findBuilderBookings(builder.getId())
                .stream()
                .map(this::enrichBookingResponse)
                .toList();
    }

    @Transactional
    public BookingResponse approveBookingByBuilder(Long bookingId) {
        Long tenantId = TenantContext.getTenantId();

        Builder builder = builderRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Builder not found"));

        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        validateBuilderOwnsBooking(builder, booking);

        if (booking.getStatus() != BookingStatus.AWAITING_BUILDER_APPROVAL
                && booking.getStatus() != BookingStatus.REAPPROVAL_REQUESTED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only awaiting-builder-approval or reapproval-requested bookings can be approved"
            );
        }

        String oldStatus = booking.getStatus() != null ? booking.getStatus().name() : null;

        booking.setStatus(BookingStatus.PAYMENT_OPTION_PENDING);
        booking.setBuilderResponseAt(LocalDateTime.now());
        booking.setBuilderApprovedBy(builder.getId());
        booking.setBuilderResponseNote("Builder approved. Investor can now select payment option.");

        Plot plot = booking.getPlot();
        plot.setBookingStatus(BookingStatus.PAYMENT_OPTION_PENDING);

        bookingRepository.save(booking);
        plotRepository.save(plot);

        auditLogService.log(
                AuditAction.BOOKING_APPROVED_BY_BUILDER,
                "BOOKING",
                booking.getId(),
                "Builder approved booking",
                oldStatus,
                booking.getStatus().name()
        );

        try {
            notificationService.notifyInvestorBuilderApproved(booking);
            log.info("APPROVE NOTIFICATION SENT | bookingId={}", booking.getId());
        } catch (Exception ex) {
            log.error("APPROVE NOTIFICATION FAILED | bookingId={} | message={}",
                    booking.getId(), ex.getMessage(), ex);
        }

        return enrichBookingResponse(booking);
    }
    @Transactional
    public BookingResponse rejectBookingByBuilder(Long bookingId, String note) {
        Long tenantId = TenantContext.getTenantId();

        Builder builder = builderRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Builder not found"));

        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        validateBuilderOwnsBooking(builder, booking);

        if (booking.getStatus() != BookingStatus.AWAITING_BUILDER_APPROVAL
                && booking.getStatus() != BookingStatus.REAPPROVAL_REQUESTED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only awaiting-builder-approval or reapproval-requested bookings can be rejected"
            );
        }

        booking.setStatus(BookingStatus.BUILDER_REJECTED);
        booking.setBuilderResponseAt(LocalDateTime.now());
        booking.setBuilderApprovedBy(builder.getId());
        booking.setBuilderResponseNote(
                note == null || note.isBlank()
                        ? "Builder rejected the booking. Investor may request re-approval after discussion."
                        : note
        );

        Plot plot = booking.getPlot();
        plot.setBookingStatus(BookingStatus.BUILDER_REJECTED);

        bookingRepository.save(booking);
        plotRepository.save(plot);

        try {
            notificationService.notifyInvestorBuilderRejected(booking);
            log.info("REJECT NOTIFICATION SENT | bookingId={}", booking.getId());
        } catch (Exception ex) {
            log.error("REJECT NOTIFICATION FAILED | bookingId={} | message={}",
                    booking.getId(), ex.getMessage(), ex);
        }

        return enrichBookingResponse(booking);
    }

    @Transactional
    public BookingResponse reApproveBookingByBuilder(Long bookingId, String note) {
        Long tenantId = TenantContext.getTenantId();

        Builder builder = builderRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Builder not found"));

        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        validateBuilderOwnsBooking(builder, booking);

        if (booking.getStatus() != BookingStatus.REAPPROVAL_REQUESTED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only reapproval-requested bookings can be re-approved"
            );
        }

        booking.setStatus(BookingStatus.PAYMENT_OPTION_PENDING);
        booking.setBuilderResponseAt(LocalDateTime.now());
        booking.setBuilderApprovedBy(builder.getId());
        booking.setBuilderResponseNote(
                note == null || note.isBlank()
                        ? "Builder re-approved the booking. Investor can now select payment option."
                        : note
        );

        Plot plot = booking.getPlot();
        plot.setBookingStatus(BookingStatus.PAYMENT_OPTION_PENDING);

        bookingRepository.save(booking);
        plotRepository.save(plot);

        log.info("REAPPROVED | bookingId={} | status={} | plotId={}",
                booking.getId(), booking.getStatus(), plot.getId());

        try {
            notificationService.notifyInvestorBuilderApproved(booking);
            log.info("REAPPROVE NOTIFICATION SENT | bookingId={}", booking.getId());
        } catch (Exception ex) {
            log.error("REAPPROVE NOTIFICATION FAILED | bookingId={} | message={}",
                    booking.getId(), ex.getMessage(), ex);
        }

        return enrichBookingResponse(booking);
    }

    @Transactional
    public BookingResponse requestReapproval(Long bookingId, String note) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        validateInvestorOwnsBooking(booking);

        if (booking.getStatus() != BookingStatus.BUILDER_REJECTED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Re-approval can be requested only after builder rejection"
            );
        }

        booking.setStatus(BookingStatus.REAPPROVAL_REQUESTED);
        booking.setBuilderResponseNote(
                note == null || note.isBlank()
                        ? "Investor requested re-approval after discussion/meeting."
                        : note
        );

        Plot plot = booking.getPlot();
        plot.setBookingStatus(BookingStatus.REAPPROVAL_REQUESTED);

        bookingRepository.save(booking);
        plotRepository.save(plot);

        return enrichBookingResponse(booking);
    }

    @Transactional
    public BookingResponse choosePaymentOption(Long bookingId, PaymentOptionRequest request) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        validateInvestorOwnsBooking(booking);

        if (booking.getStatus() != BookingStatus.PAYMENT_OPTION_PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment option can be selected only when builder approval is completed"
            );
        }

        String option = request.getOption() == null
                ? ""
                : request.getOption().trim().toUpperCase(Locale.ROOT);

        Plot plot = booking.getPlot();

        switch (option) {
            case "FULL_PAYMENT" -> {
                booking.setStatus(BookingStatus.BUILDER_APPROVED);
                booking.setBuilderResponseNote(
                        mergeNotes(
                                "Investor selected FULL_PAYMENT and can proceed to remaining payment.",
                                request.getNote()
                        )
                );
                plot.setBookingStatus(BookingStatus.BUILDER_APPROVED);
            }
            case "EMI" -> {
                booking.setStatus(BookingStatus.BUILDER_APPROVED);
                booking.setBuilderResponseNote(
                        mergeNotes(
                                "Investor selected EMI option and can proceed to EMI plan creation.",
                                request.getNote()
                        )
                );
                plot.setBookingStatus(BookingStatus.BUILDER_APPROVED);
            }
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid option. Use FULL_PAYMENT or EMI"
            );
        }

        bookingRepository.save(booking);
        plotRepository.save(plot);

        return enrichBookingResponse(booking);
    }

    private String mergeNotes(String base, String extra) {
        if (extra == null || extra.isBlank()) {
            return base;
        }
        return base + " Note: " + extra.trim();
    }

    private BookingResponse enrichBookingResponse(Booking booking) {
        Payment payment = paymentRepository.findByBooking(booking).orElse(null);
        BookingAgreement agreement = bookingAgreementRepository.findByBookingId(booking.getId()).orElse(null);
        return BookingMapper.toResponse(booking, payment, agreement);
    }

    private void validateInvestorOwnsBooking(Booking booking) {
        if (booking.getInvestor() == null
                || booking.getInvestor().getUser() == null
                || !booking.getInvestor().getUser().getId().equals(SecurityUtil.getCurrentUserId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This booking does not belong to the current investor"
            );
        }
    }

    private void validateBuilderOwnsBooking(Builder builder, Booking booking) {
        if (booking.getPlot() == null
                || booking.getPlot().getProject() == null
                || booking.getPlot().getProject().getBuilder() == null
                || !booking.getPlot().getProject().getBuilder().getId().equals(builder.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This booking does not belong to your plots"
            );
        }
    }

    private void saveConflict(Long plotId,
                              Long investorId,
                              Long tenantId,
                              Long bookingId,
                              String conflictType,
                              String reason) {

        BookingConflictLog logEntity = new BookingConflictLog();
        logEntity.setPlotId(plotId);
        logEntity.setInvestorId(investorId);
        logEntity.setTenantId(tenantId);
        logEntity.setBookingId(bookingId);
        logEntity.setConflictType(conflictType);
        logEntity.setReason(reason);
        logEntity.setAttemptTime(LocalDateTime.now());

        conflictRepository.save(logEntity);
    }
}