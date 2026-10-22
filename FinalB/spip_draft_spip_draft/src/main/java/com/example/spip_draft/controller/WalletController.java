package com.example.spip_draft.controller;

import com.example.spip_draft.dto.wallet.WalletTransactionResponse;
import com.example.spip_draft.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WalletController {

    private final WalletService walletService;

    // =========================
    // 👤 INVESTOR HISTORY
    // =========================
    @GetMapping("/history/me")
    public ResponseEntity<List<WalletTransactionResponse>> getMyWalletHistory() {
        return ResponseEntity.ok(walletService.getMyWalletHistory());
    }
}