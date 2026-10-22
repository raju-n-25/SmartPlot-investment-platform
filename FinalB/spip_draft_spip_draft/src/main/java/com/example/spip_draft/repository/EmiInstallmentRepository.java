package com.example.spip_draft.repository;

import com.example.spip_draft.entity.EmiInstallment;
import com.example.spip_draft.enums.InstallmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EmiInstallmentRepository extends JpaRepository<EmiInstallment, Long> {

    List<EmiInstallment> findByEmiPlanIdOrderByInstallmentNumberAsc(Long emiPlanId);

    Optional<EmiInstallment> findByEmiPlanIdAndInstallmentNumber(Long emiPlanId, Integer installmentNumber);

    List<EmiInstallment> findByStatusAndDueDateBefore(InstallmentStatus status, LocalDate date);

    List<EmiInstallment> findByEmiPlanIdAndStatusInOrderByInstallmentNumberAsc(
            Long emiPlanId,
            List<InstallmentStatus> statuses
    );

    long countByEmiPlanIdAndStatus(Long emiPlanId, InstallmentStatus status);

    List<EmiInstallment> findByStatusIn(List<InstallmentStatus> statuses);

    List<EmiInstallment> findByEmiPlanIdAndStatus(Long emiPlanId, InstallmentStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EmiInstallment e WHERE e.id = :id")
    Optional<EmiInstallment> findByIdForUpdate(@Param("id") Long id);
}