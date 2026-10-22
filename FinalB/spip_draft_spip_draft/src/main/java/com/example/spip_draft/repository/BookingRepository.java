package com.example.spip_draft.repository;

import com.example.spip_draft.entity.Booking;
import com.example.spip_draft.entity.Investor;
import com.example.spip_draft.entity.Plot;
import com.example.spip_draft.enums.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByPlot(Plot plot);

    List<Booking> findByInvestor(Investor investor);

    List<Booking> findByInvestorId(Long investorId);

    List<Booking> findByPlot_Project_Builder_Id(Long builderId);

    List<Booking> findByStatus(BookingStatus status);

    List<Booking> findByTokenExpiryBefore(LocalDateTime time);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT b
        FROM Booking b
        JOIN FETCH b.plot p
        JOIN FETCH p.project pr
        JOIN FETCH pr.builder bu
        JOIN FETCH b.investor i
        WHERE b.id = :bookingId
    """)
    Optional<Booking> findByIdForUpdate(@Param("bookingId") Long bookingId);

    @Query("""
        SELECT b FROM Booking b
        WHERE b.plot.id = :plotId
        AND b.status IN (
            'TOKEN_BLOCKED',
            'TOKEN_PAID',
            'AWAITING_BUILDER_APPROVAL',
            'PAYMENT_OPTION_PENDING',
            'BUILDER_APPROVED',
            'REAPPROVAL_REQUESTED',
            'EMI_ACTIVE',
            'CONFIRMED'
        )
        ORDER BY b.id DESC
    """)
    List<Booking> findActiveBookingsByPlot(@Param("plotId") Long plotId);

    @Query("""
        SELECT b
        FROM Booking b
        JOIN FETCH b.plot p
        JOIN FETCH p.project pr
        JOIN FETCH pr.builder bu
        JOIN FETCH b.investor i
        WHERE b.tokenExpiry < :now
        AND b.status = 'TOKEN_BLOCKED'
    """)
    List<Booking> findExpiredBookings(@Param("now") LocalDateTime now);

    @Query("""
        SELECT b
        FROM Booking b
        JOIN FETCH b.plot p
        JOIN FETCH p.project pr
        JOIN FETCH pr.builder bu
        JOIN FETCH bu.user
        JOIN FETCH b.investor i
        JOIN FETCH i.user
        WHERE pr.builder.id = :builderId
        ORDER BY b.id DESC
    """)
    List<Booking> findBuilderBookings(@Param("builderId") Long builderId);

    @Query("""
        SELECT b
        FROM Booking b
        JOIN FETCH b.plot p
        JOIN FETCH p.project pr
        JOIN FETCH pr.builder bu
        JOIN FETCH bu.user
        JOIN FETCH b.investor i
        JOIN FETCH i.user
        WHERE i.id = :investorId
        ORDER BY b.id DESC
    """)
    List<Booking> findInvestorBookings(@Param("investorId") Long investorId);

    long countByPlot_Project_Builder_Id(Long builderId);

    long countByPlot_Project_Builder_IdAndStatusIn(Long builderId, List<BookingStatus> statuses);

    long countByInvestorIdAndStatusIn(Long investorId, List<BookingStatus> statuses);

    List<Booking> findByPlot_Project_Builder_IdAndCreatedAtAfter(Long builderId, LocalDateTime after);
    
    long countByPlot_Project_Id(Long projectId);

    long countByPlot_Project_IdAndStatus(Long projectId, com.example.spip_draft.enums.BookingStatus status);

    java.util.List<com.example.spip_draft.entity.Booking> findByPlot_Project_Id(Long projectId);
}