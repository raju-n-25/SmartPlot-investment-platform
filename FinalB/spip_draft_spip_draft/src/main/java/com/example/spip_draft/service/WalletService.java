package com.example.spip_draft.service;

import com.example.spip_draft.dto.wallet.WalletTransactionResponse;
import com.example.spip_draft.entity.Investor;
import com.example.spip_draft.entity.User;
import com.example.spip_draft.entity.WalletTransaction;
import com.example.spip_draft.exception.ResourceNotFoundException;
import com.example.spip_draft.repository.InvestorRepository;
import com.example.spip_draft.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final InvestorRepository investorRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final InvestorService investorService;

    // =========================
    // 👤 INVESTOR HISTORY (SELF)
    // =========================
    public List<WalletTransactionResponse> getMyWalletHistory() {

        Investor investor = investorService.getCurrentInvestor();

        return walletTransactionRepository.findByInvestorOrderByCreatedAtDesc(investor)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // =========================
    // 🛠 ADMIN HISTORY
    // =========================
    public List<WalletTransactionResponse> getWalletHistoryByInvestorId(Long investorId) {

        Investor investor = investorRepository.findById(investorId)
                .orElseThrow(() -> new ResourceNotFoundException("Investor not found"));

        return walletTransactionRepository.findByInvestorOrderByCreatedAtDesc(investor)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // =========================
    // 🔄 MAPPER
    // =========================
    private WalletTransactionResponse mapToResponse(WalletTransaction tx) {

        Investor investor = tx.getInvestor();
        User user = investor.getUser();

        WalletTransactionResponse response = new WalletTransactionResponse();

        response.setTransactionId(tx.getId());

        response.setInvestorId(investor.getId());
        response.setUserId(user.getId());
        response.setInvestorName(user.getFullName());
        response.setInvestorEmail(user.getEmail());

        response.setAmount(tx.getAmount());
        response.setTransactionType(tx.getTransactionType());
        response.setDescription(tx.getDescription());
        response.setReferenceId(tx.getReferenceId());

        response.setCreatedAt(tx.getCreatedAt());

        return response;
    }
}