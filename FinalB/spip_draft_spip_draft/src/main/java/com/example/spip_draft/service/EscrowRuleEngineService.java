package com.example.spip_draft.service;

import com.example.spip_draft.entity.Payment;
import com.example.spip_draft.enums.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

@Service
public class EscrowRuleEngineService {

    private static final Set<EscrowStatus> MANUAL_RELEASE_ALLOWED_STATUSES =
            Set.of(EscrowStatus.HELD, EscrowStatus.READY_FOR_RELEASE, EscrowStatus.UNDER_REVIEW);

    public boolean isEligibleForAutoRelease(Payment payment) {
        return baseReleaseChecks(payment)
                && payment.getEscrowStatus() == EscrowStatus.HELD
                && isHoldWindowCompleted(payment)
                && payment.getRiskLevel() != RiskLevel.HIGH;
    }

    public boolean isEligibleForSystemRelease(Payment payment) {
        return baseReleaseChecks(payment)
                && payment.getEscrowStatus() == EscrowStatus.READY_FOR_RELEASE
                && isHoldWindowCompleted(payment)
                && payment.getRiskLevel() != RiskLevel.HIGH;
    }

    public boolean isEligibleForManualRelease(Payment payment) {
        return baseReleaseChecks(payment)
                && MANUAL_RELEASE_ALLOWED_STATUSES.contains(payment.getEscrowStatus())
                && isHoldWindowCompleted(payment);
    }

    // Backward compatibility for existing calls
    public boolean isEligibleForRelease(Payment payment) {
        return isEligibleForManualRelease(payment);
    }

    public String firstAutoFailureReason(Payment payment) {
        String base = baseFailureReason(payment);
        if (base != null) return base;

        if (payment.getEscrowStatus() != EscrowStatus.HELD) {
            return "Escrow is not in HELD state for auto evaluation";
        }
        if (!isHoldWindowCompleted(payment)) {
            return "Hold window not completed";
        }
        if (payment.getRiskLevel() == RiskLevel.HIGH) {
            return "High-risk transaction requires manual review";
        }
        return "Unknown auto release restriction";
    }

    public String firstSystemFailureReason(Payment payment) {
        String base = baseFailureReason(payment);
        if (base != null) return base;

        if (payment.getEscrowStatus() != EscrowStatus.READY_FOR_RELEASE) {
            return "Escrow is not READY_FOR_RELEASE";
        }
        if (!isHoldWindowCompleted(payment)) {
            return "Hold window not completed";
        }
        if (payment.getRiskLevel() == RiskLevel.HIGH) {
            return "High-risk transaction cannot be auto released";
        }
        return "Unknown system release restriction";
    }

    public String firstManualFailureReason(Payment payment) {
        String base = baseFailureReason(payment);
        if (base != null) return base;

        if (!MANUAL_RELEASE_ALLOWED_STATUSES.contains(payment.getEscrowStatus())) {
            return "Escrow status does not allow manual release";
        }
        if (!isHoldWindowCompleted(payment)) {
            return "Hold window not completed";
        }
        return "Unknown manual release restriction";
    }

    // Backward compatibility for existing calls
    public String firstFailureReason(Payment payment) {
        return firstManualFailureReason(payment);
    }

    private boolean baseReleaseChecks(Payment payment) {
        return payment != null
                && payment.getPaymentStatus() == PaymentStatus.COMPLETED
                && payment.getBooking() != null
                && payment.getBooking().getStatus() == BookingStatus.CONFIRMED
                && payment.getEscrowStatus() != null
                && !payment.isRefunded()
                && !payment.isEscrowReleased()
                && !payment.isDisputeOpen()
                && !payment.isPayoutBlocked()
                && isBuilderKycApproved(payment)
                && isProjectApproved(payment)
                && isAgreementSatisfied(payment)
                && isAmountReconciled(payment);
    }

    private String baseFailureReason(Payment payment) {
        if (payment == null) return "Payment not found";

        if (payment.getPaymentStatus() != PaymentStatus.COMPLETED) {
            return "Payment not completed";
        }
        if (payment.getBooking() == null) {
            return "Booking not found";
        }
        if (payment.getBooking().getStatus() != BookingStatus.CONFIRMED) {
            return "Booking not confirmed";
        }
        if (payment.getEscrowStatus() == null) {
            return "Escrow status missing";
        }
        if (payment.isRefunded()) {
            return "Payment already refunded";
        }
        if (payment.isEscrowReleased()) {
            return "Escrow already released";
        }
        if (payment.isDisputeOpen()) {
            return "Dispute is open";
        }
        if (payment.isPayoutBlocked()) {
            return "Payout is blocked";
        }
        if (!isBuilderKycApproved(payment)) {
            return "Builder KYC not approved";
        }
        if (!isProjectApproved(payment)) {
            return "Project not approved";
        }
        if (!isAgreementSatisfied(payment)) {
            return "Agreement not fully signed";
        }
        if (!isAmountReconciled(payment)) {
            return "Amount reconciliation failed";
        }

        return null;
    }

    private boolean isBuilderKycApproved(Payment payment) {
        return payment.getBooking().getPlot().getProject().getBuilder().getKycStatus() == KycStatus.APPROVED;
    }

    private boolean isProjectApproved(Payment payment) {
        return payment.getBooking().getPlot().getProject().getStatus() == ProjectStatus.APPROVED;
    }

    private boolean isAgreementSatisfied(Payment payment) {
        return payment.getAgreementStatus() == AgreementStatus.SIGNED;
    }

    private boolean isAmountReconciled(Payment payment) {
        if (payment.getAmount() == null || payment.getBooking().getPlot() == null) {
            return false;
        }
        return payment.getAmount().compareTo(payment.getBooking().getPlot().getPrice()) == 0;
    }

    private boolean isHoldWindowCompleted(Payment payment) {
        return payment.getHoldUntil() != null && !payment.getHoldUntil().isAfter(LocalDateTime.now());
    }
}