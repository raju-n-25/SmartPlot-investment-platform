package com.example.spip_draft.repository;

import com.example.spip_draft.entity.Booking;
import com.example.spip_draft.entity.Payment;
import com.example.spip_draft.enums.EscrowStatus;
import com.example.spip_draft.enums.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("""
        SELECT p
        FROM Payment p
        JOIN FETCH p.booking b
        JOIN FETCH b.plot pl
        JOIN FETCH pl.project pr
        JOIN FETCH pr.builder bu
        JOIN FETCH bu.user
        JOIN FETCH b.investor i
        JOIN FETCH i.user
        WHERE b = :booking
    """)
    Optional<Payment> findByBooking(@Param("booking") Booking booking);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p
        FROM Payment p
        JOIN FETCH p.booking b
        JOIN FETCH b.plot pl
        JOIN FETCH pl.project pr
        JOIN FETCH pr.builder bu
        JOIN FETCH b.investor i
        WHERE b.id = :bookingId
    """)
    Optional<Payment> findByBookingIdForUpdate(@Param("bookingId") Long bookingId);

    List<Payment> findByPaymentStatus(PaymentStatus status);

    List<Payment> findByPaymentStatusAndExpiryTimeBefore(
            PaymentStatus status,
            LocalDateTime time
    );

    List<Payment> findByBooking_Plot_Project_Builder_Id(Long builderId);

    List<Payment> findByBooking_Investor_Id(Long investorId);

    List<Payment> findByEscrowStatus(EscrowStatus escrowStatus);

    List<Payment> findByEscrowStatusAndHoldUntilBefore(EscrowStatus escrowStatus, LocalDateTime time);

    List<Payment> findByEscrowStatusIn(List<EscrowStatus> statuses);
    java.util.List<com.example.spip_draft.entity.Payment> findByBooking_Plot_Project_Id(Long projectId);
    List<Payment> findByEscrowStatusAndFreezeDeadlineAtBefore(
            EscrowStatus escrowStatus,
            LocalDateTime now
    );
}
