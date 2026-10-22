package com.example.spip_draft.payment.gateway;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class StripeGateway implements PaymentGateway {

    @Override
    public String processPayment(BigDecimal amount) {
        return "STRIPE_TXN_" + UUID.randomUUID();
    }
}