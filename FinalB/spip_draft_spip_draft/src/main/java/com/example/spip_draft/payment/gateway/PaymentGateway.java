package com.example.spip_draft.payment.gateway;

import java.math.BigDecimal;

public interface PaymentGateway {
    String processPayment(BigDecimal amount);
}