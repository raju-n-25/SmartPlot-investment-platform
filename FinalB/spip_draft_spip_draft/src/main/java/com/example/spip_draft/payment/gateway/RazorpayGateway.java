package com.example.spip_draft.payment.gateway;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class RazorpayGateway implements PaymentGateway {

    @Override
    public String processPayment(BigDecimal amount) {
        return "RAZORPAY_TXN_" + UUID.randomUUID();
    }
}