package com.example.spip_draft.service;

import com.example.spip_draft.entity.Booking;
import com.example.spip_draft.entity.Payment;
import com.example.spip_draft.entity.Plot;
import com.example.spip_draft.enums.KycStatus;
import com.example.spip_draft.enums.ProjectStatus;
import com.example.spip_draft.enums.RiskLevel;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class EscrowRiskService {

    public int calculateRiskScore(Payment payment) {
        int score = 0;

        Booking booking = payment.getBooking();
        Plot plot = booking.getPlot();

        if (plot.getProject() == null || plot.getProject().getStatus() != ProjectStatus.APPROVED) {
            score += 50;
        }

        if (plot.getProject() == null
                || plot.getProject().getBuilder() == null
                || plot.getProject().getBuilder().getKycStatus() != KycStatus.APPROVED) {
            score += 50;
        }

        if (plot.getProject() != null && plot.getProject().getLegalDetails() == null) {
            score += 30;
        }

        if (booking.getInvestor() != null && booking.getInvestor().getRiskScore() != null) {
            score += Math.min(booking.getInvestor().getRiskScore(), 20);
        }

        if (payment.getAmount() != null && plot.getPrice() != null) {
            BigDecimal expected = plot.getPrice();
            if (payment.getAmount().compareTo(expected) != 0) {
                score += 40;
            }
        }

        return score;
    }

    public RiskLevel resolveRiskLevel(int score) {
        if (score >= 60) {
            return RiskLevel.HIGH;
        }
        if (score >= 25) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }
}