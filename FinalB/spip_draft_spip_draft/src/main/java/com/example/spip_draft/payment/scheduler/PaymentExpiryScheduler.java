package com.example.spip_draft.payment.scheduler;

import com.example.spip_draft.entity.Booking;
import com.example.spip_draft.entity.Payment;
import com.example.spip_draft.entity.Plot;
import com.example.spip_draft.enums.BookingStatus;
import com.example.spip_draft.enums.PaymentStatus;
import com.example.spip_draft.repository.BookingRepository;
import com.example.spip_draft.repository.PaymentRepository;
import com.example.spip_draft.repository.PlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentExpiryScheduler {

    private final PaymentRepository paymentRepository;
    private final PlotRepository plotRepository;
    private final BookingRepository bookingRepository;

    /**
     * Runs every 1 minute to expire only token-paid payments
     * that are still waiting for builder/payment progression.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expirePayments() {

        List<Payment> payments = paymentRepository.findByPaymentStatus(PaymentStatus.TOKEN_PAID);
        LocalDateTime now = LocalDateTime.now();

        for (Payment payment : payments) {

            if (payment.getExpiryTime() == null || !payment.getExpiryTime().isBefore(now)) {
                continue;
            }

            Booking booking = payment.getBooking();
            if (booking == null) {
                continue;
            }

            /*
             * IMPORTANT:
             * Only expire if booking is still in token-stage review flow.
             * Do NOT expire progressed bookings like BUILDER_APPROVED / CONFIRMED / EMI_ACTIVE / REFUNDED.
             */
            if (booking.getStatus() != BookingStatus.TOKEN_PAID
                    && booking.getStatus() != BookingStatus.AWAITING_BUILDER_APPROVAL
                    && booking.getStatus() != BookingStatus.BUILDER_REJECTED) {
                continue;
            }

            Plot plot = plotRepository.findPlotForUpdate(booking.getPlot().getId()).orElse(null);
            if (plot == null) {
                continue;
            }

            payment.setPaymentStatus(PaymentStatus.FAILED);
            payment.setExpiryTime(null);

            booking.setStatus(BookingStatus.EXPIRED);
            booking.setTokenExpiry(null);

            plot.setAvailable(true);
            plot.setBookingStatus(null);
            plot.setBlockedAt(null);

            paymentRepository.save(payment);
            bookingRepository.save(booking);
            plotRepository.save(plot);

            log.info("Token payment expired safely | paymentId={} | bookingId={} | plotId={}",
                    payment.getId(), booking.getId(), plot.getId());
        }
    }
}