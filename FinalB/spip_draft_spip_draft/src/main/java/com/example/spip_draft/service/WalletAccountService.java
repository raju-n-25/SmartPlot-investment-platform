package com.example.spip_draft.service;

import com.example.spip_draft.entity.Investor;
import com.example.spip_draft.entity.WalletTransaction;
import com.example.spip_draft.enums.WalletTransactionType;
import com.example.spip_draft.repository.InvestorRepository;
import com.example.spip_draft.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional
public class WalletAccountService {

    private final InvestorRepository investorRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    public BigDecimal getWalletBalance(Investor investor) {
        return investor.getWalletBalance() != null
                ? investor.getWalletBalance()
                : BigDecimal.ZERO;
    }

    public void debitWallet(Investor investor,
                            BigDecimal amount,
                            String description,
                            Long referenceId) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be greater than zero");
        }

        BigDecimal currentBalance = getWalletBalance(investor);

        if (currentBalance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient wallet balance");
        }

        investor.setWalletBalance(currentBalance.subtract(amount));
        investorRepository.save(investor);

        WalletTransaction tx = new WalletTransaction();
        tx.setInvestor(investor);
        tx.setAmount(amount);
        tx.setTransactionType(WalletTransactionType.DEBIT);
        tx.setDescription(description);
        tx.setReferenceId(referenceId);

        walletTransactionRepository.save(tx);
    }

    public void creditWallet(Investor investor,
                             BigDecimal amount,
                             String description,
                             Long referenceId) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be greater than zero");
        }

        BigDecimal currentBalance = getWalletBalance(investor);

        investor.setWalletBalance(currentBalance.add(amount));
        investorRepository.save(investor);

        WalletTransaction tx = new WalletTransaction();
        tx.setInvestor(investor);
        tx.setAmount(amount);
        tx.setTransactionType(WalletTransactionType.CREDIT);
        tx.setDescription(description);
        tx.setReferenceId(referenceId);

        walletTransactionRepository.save(tx);
    }

    public BigDecimal getWalletContribution(Investor investor, BigDecimal requestedAmount) {
        BigDecimal walletBalance = getWalletBalance(investor);

        if (requestedAmount == null || requestedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return walletBalance.min(requestedAmount);
    }
}