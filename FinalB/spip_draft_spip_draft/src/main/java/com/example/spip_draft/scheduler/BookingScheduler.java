package com.example.spip_draft.scheduler;

import com.example.spip_draft.entity.Booking;
import com.example.spip_draft.entity.Plot;
import com.example.spip_draft.enums.BookingStatus;
import com.example.spip_draft.repository.BookingRepository;
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
public class BookingScheduler {

    private final BookingRepository bookingRepository;
    private final PlotRepository plotRepository;

    /**
     * Runs every 1 minute to release only expired TOKEN_BLOCKED bookings.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void releaseExpiredBookings() {

        LocalDateTime now = LocalDateTime.now();

        List<Booking> expiredBookings = bookingRepository.findExpiredBookings(now);

        for (Booking booking : expiredBookings) {

            if (booking.getTokenExpiry() == null || !booking.getTokenExpiry().isBefore(now)) {
                continue;
            }

            if (booking.getStatus() != BookingStatus.TOKEN_BLOCKED) {
                continue;
            }

            Plot plot = plotRepository.findPlotForUpdate(booking.getPlot().getId())
                    .orElse(null);

            if (plot == null) {
                continue;
            }

            plot.setAvailable(true);
            plot.setBookingStatus(null);
            plot.setBlockedAt(null);

            booking.setStatus(BookingStatus.EXPIRED);
            booking.setTokenExpiry(null);

            plotRepository.save(plot);
            bookingRepository.save(booking);

            log.info("Expired token-blocked booking released | bookingId={} | plotId={}",
                    booking.getId(), plot.getId());
        }
    }
}