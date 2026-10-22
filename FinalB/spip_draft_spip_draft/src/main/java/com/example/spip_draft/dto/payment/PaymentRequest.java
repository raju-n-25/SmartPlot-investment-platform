package com.example.spip_draft.dto.payment;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentRequest {

    // Booking associated with this payment
    private Long bookingId;

    // Payment amount
    private BigDecimal amount;

    // Payment method (UPI, CARD, NETBANKING etc.)
    private String paymentType;

    // Payment gateway transaction id
    private String transactionId;

}