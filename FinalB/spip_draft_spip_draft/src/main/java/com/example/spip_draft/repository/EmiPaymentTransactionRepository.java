package com.example.spip_draft.repository;

import com.example.spip_draft.entity.EmiPaymentTransaction;
import com.example.spip_draft.enums.EmiPaymentTransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmiPaymentTransactionRepository extends JpaRepository<EmiPaymentTransaction, Long> {

    List<EmiPaymentTransaction> findByEmiInstallmentIdOrderByCreatedAtAsc(Long installmentId);

    List<EmiPaymentTransaction> findByEmiPlanIdOrderByCreatedAtAsc(Long emiPlanId);
    List<EmiPaymentTransaction> findByInvestorIdAndStatus(Long investorId, EmiPaymentTransactionStatus status);
}