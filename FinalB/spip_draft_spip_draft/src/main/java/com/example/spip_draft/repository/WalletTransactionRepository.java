package com.example.spip_draft.repository;

import com.example.spip_draft.entity.Investor;
import com.example.spip_draft.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    List<WalletTransaction> findByInvestorOrderByCreatedAtDesc(Investor investor);
}