package com.example.spip_draft.controller;

import com.example.spip_draft.dto.wallet.WalletTransactionResponse;
import com.example.spip_draft.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/wallet")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminWalletController {

    private final WalletService walletService;

    // =========================
    // 🛠 ADMIN VIEW ANY INVESTOR HISTORY
    // =========================
    @GetMapping("/history/{investorId}")
    public ResponseEntity<List<WalletTransactionResponse>> getWalletHistoryByInvestorId(
            @PathVariable Long investorId
    ) {
        return ResponseEntity.ok(walletService.getWalletHistoryByInvestorId(investorId));
    }
}