package com.example.spip_draft.controller;

import com.example.spip_draft.dto.emi.CreateEmiPlanRequest;
import com.example.spip_draft.dto.emi.EmiInstallmentResponse;
import com.example.spip_draft.dto.emi.EmiPaymentTransactionResponse;
import com.example.spip_draft.dto.emi.EmiPlanResponse;
import com.example.spip_draft.dto.emi.ForeclosureResponse;
import com.example.spip_draft.dto.emi.PayEmiInstallmentRequest;
import com.example.spip_draft.service.EmiPaymentService;
import com.example.spip_draft.service.EmiPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/emi")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EmiController {

    private final EmiPlanService emiPlanService;
    private final EmiPaymentService emiPaymentService;

    // =========================
    // CREATE EMI PLAN
    // =========================
    @PostMapping("/bookings/{bookingId}/create")
    public ResponseEntity<EmiPlanResponse> createEmiPlan(
            @PathVariable Long bookingId,
            @Valid @RequestBody CreateEmiPlanRequest request
    ) {
        return ResponseEntity.ok(emiPlanService.createPlan(bookingId, request));
    }

    // =========================
    // GET EMI PLAN BY BOOKING ID
    // =========================
    @GetMapping("/bookings/{bookingId}")
    public ResponseEntity<EmiPlanResponse> getPlanByBookingId(@PathVariable Long bookingId) {
        return ResponseEntity.ok(emiPlanService.getPlanByBookingId(bookingId));
    }

    // =========================
    // GET EMI SCHEDULE
    // =========================
    @GetMapping("/plans/{emiPlanId}/schedule")
    public ResponseEntity<List<EmiInstallmentResponse>> getPlanSchedule(@PathVariable Long emiPlanId) {
        return ResponseEntity.ok(emiPlanService.getPlanSchedule(emiPlanId));
    }

    // =========================
    // PAY EMI INSTALLMENT
    // =========================
    @PostMapping("/installments/{installmentId}/pay")
    public ResponseEntity<EmiPaymentTransactionResponse> payInstallment(
            @PathVariable Long installmentId,
            @Valid @RequestBody PayEmiInstallmentRequest request
    ) {
        return ResponseEntity.ok(emiPaymentService.payInstallment(installmentId, request));
    }

    // =========================
    // GET INSTALLMENT TRANSACTIONS
    // =========================
    @GetMapping("/installments/{installmentId}/transactions")
    public ResponseEntity<List<EmiPaymentTransactionResponse>> getInstallmentTransactions(
            @PathVariable Long installmentId
    ) {
        return ResponseEntity.ok(emiPaymentService.getInstallmentTransactions(installmentId));
    }

    // =========================
    // RECOVER DEFAULTED PLAN
    // =========================
    @PostMapping("/plans/{emiPlanId}/recover")
    public ResponseEntity<String> recoverDefaultedPlan(@PathVariable Long emiPlanId) {
        return ResponseEntity.ok(emiPaymentService.recoverDefaultedPlan(emiPlanId));
    }

    // =========================
    // GET FORECLOSURE AMOUNT
    // =========================
    @GetMapping("/plans/{emiPlanId}/foreclosure")
    public ResponseEntity<ForeclosureResponse> getForeclosureAmount(@PathVariable Long emiPlanId) {
        return ResponseEntity.ok(emiPaymentService.getForeclosureAmount(emiPlanId));
    }

    // =========================
    // FORECLOSE EMI PLAN
    // =========================
    @PostMapping("/plans/{emiPlanId}/foreclose")
    public ResponseEntity<String> foreclosePlan(
            @PathVariable Long emiPlanId,
            @RequestParam(defaultValue = "false") boolean useWallet,
            @RequestParam(required = false) String gatewayType
    ) {
        return ResponseEntity.ok(emiPaymentService.foreclosePlan(emiPlanId, useWallet, gatewayType));
    }
}