package com.example.spip_draft.repository;

import com.example.spip_draft.entity.BookingAgreement;
import com.example.spip_draft.enums.AgreementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingAgreementRepository extends JpaRepository<BookingAgreement, Long> {

    @Query("""
        SELECT a
        FROM BookingAgreement a
        JOIN FETCH a.booking b
        JOIN FETCH b.plot p
        JOIN FETCH p.project pr
        JOIN FETCH pr.builder bu
        JOIN FETCH bu.user
        JOIN FETCH b.investor i
        JOIN FETCH i.user
        LEFT JOIN FETCH a.payment
        WHERE b.id = :bookingId
    """)
    Optional<BookingAgreement> findByBookingId(@Param("bookingId") Long bookingId);

    @Query("""
        SELECT a
        FROM BookingAgreement a
        JOIN FETCH a.booking b
        JOIN FETCH b.plot p
        JOIN FETCH p.project pr
        JOIN FETCH pr.builder bu
        JOIN FETCH bu.user
        JOIN FETCH b.investor i
        JOIN FETCH i.user
        LEFT JOIN FETCH a.payment
        WHERE a.id = :agreementId
    """)
    Optional<BookingAgreement> findDetailedById(@Param("agreementId") Long agreementId);

    List<BookingAgreement> findByStatus(AgreementStatus status);

    List<BookingAgreement> findByStatusAndExpiresAtBefore(AgreementStatus status, LocalDateTime time);
}
