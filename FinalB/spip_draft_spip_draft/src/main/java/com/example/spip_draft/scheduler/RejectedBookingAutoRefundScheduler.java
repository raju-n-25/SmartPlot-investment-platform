package com.example.spip_draft.scheduler;

import com.example.spip_draft.entity.Booking;
import com.example.spip_draft.entity.Payment;
import com.example.spip_draft.enums.BookingStatus;
import com.example.spip_draft.enums.PaymentStatus;
import com.example.spip_draft.enums.RefundMode;
import com.example.spip_draft.repository.BookingRepository;
import com.example.spip_draft.repository.PaymentRepository;
import com.example.spip_draft.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RejectedBookingAutoRefundScheduler {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    @Value("${booking.rejection.auto-refund.days:4}")
    private int refundAfterDays;

    @Scheduled(cron = "${booking.rejection.auto-refund.cron:0 0 */6 * * *}")
    public void autoRefundRejectedBookings() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(refundAfterDays);

        List<Booking> rejectedBookings = bookingRepository.findByStatus(BookingStatus.BUILDER_REJECTED);

        for (Booking booking : rejectedBookings) {
            try {
                if (booking.getBuilderResponseAt() == null || booking.getBuilderResponseAt().isAfter(threshold)) {
                    continue;
                }

                Payment payment = paymentRepository.findByBooking(booking).orElse(null);
                if (payment == null) {
                    continue;
                }

                if (payment.isRefunded()) {
                    continue;
                }

                if (payment.getPaymentStatus() != PaymentStatus.TOKEN_PAID
                        && payment.getPaymentStatus() != PaymentStatus.COMPLETED) {
                    continue;
                }

                paymentService.refund(booking.getId(), RefundMode.WALLET);
                booking.setStatus(BookingStatus.REFUNDED);
                bookingRepository.save(booking);

                log.info("Auto refund processed for rejected bookingId={}", booking.getId());

            } catch (Exception ex) {
                log.error("Auto refund failed for bookingId={} : {}", booking.getId(), ex.getMessage(), ex);
            }
        }
    }
}