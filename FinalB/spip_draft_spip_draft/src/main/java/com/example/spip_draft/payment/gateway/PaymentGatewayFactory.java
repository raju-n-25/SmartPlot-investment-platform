package com.example.spip_draft.payment.gateway;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentGatewayFactory {

    private final RazorpayGateway razorpayGateway;
    private final StripeGateway stripeGateway;

    public PaymentGateway getGateway(String type) {

        if (type == null || type.isBlank()) {
            return razorpayGateway;
        }

        return switch (type.toUpperCase()) {
            case "RAZORPAY" -> razorpayGateway;
            case "STRIPE" -> stripeGateway;
            default -> throw new IllegalArgumentException("Unsupported gateway: " + type);
        };
    }
}