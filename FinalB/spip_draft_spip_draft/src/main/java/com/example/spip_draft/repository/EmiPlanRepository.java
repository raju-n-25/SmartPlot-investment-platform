package com.example.spip_draft.repository;

import com.example.spip_draft.entity.Booking;
import com.example.spip_draft.entity.EmiPlan;
import com.example.spip_draft.enums.EmiPlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmiPlanRepository extends JpaRepository<EmiPlan, Long> {

    Optional<EmiPlan> findByBooking(Booking booking);

    Optional<EmiPlan> findByBookingId(Long bookingId);

    List<EmiPlan> findByTenantId(Long tenantId);

    List<EmiPlan> findByInvestorId(Long investorId);

    List<EmiPlan> findByStatus(EmiPlanStatus status);

    boolean existsByBookingId(Long bookingId);
}