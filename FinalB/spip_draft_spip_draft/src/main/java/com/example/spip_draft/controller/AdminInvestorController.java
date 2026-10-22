package com.example.spip_draft.controller;

import com.example.spip_draft.entity.Investor;
import com.example.spip_draft.service.InvestorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/investors")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminInvestorController {

    private final InvestorService investorService;

    /**
     * Get all investors
     */
    @GetMapping
    public ResponseEntity<List<Investor>> getAllInvestors() {
        return ResponseEntity.ok(investorService.getAllInvestors());
    }

    /**
     * Approve investor account
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<String> approveInvestor(@PathVariable Long id) {
        investorService.approveInvestor(id);
        return ResponseEntity.ok("Investor approved successfully");
    }

    /**
     * Delete investor
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteInvestor(@PathVariable Long id) {
        investorService.deleteInvestor(id);
        return ResponseEntity.ok("Investor deleted successfully");
    }
}