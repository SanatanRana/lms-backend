package com.lms.modules.payment.gateway;

import com.lms.common.strategy.PaymentGateway;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mock payment gateway for development and testing.
 *
 * SYSTEM DESIGN: Strategy Pattern
 * To add Razorpay: create RazorpayGateway implements PaymentGateway,
 * annotate with @Component, and set payment.gateway=razorpay in config.
 * ZERO changes to PaymentService.
 */
@Component
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public String getGatewayId() {
        return "mock";
    }

    @Override
    public Map<String, String> createOrder(Double amount, String currency, String description) {
        Map<String, String> order = new HashMap<>();
        order.put("orderId", "MOCK_ORDER_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.put("amount", String.valueOf(amount));
        order.put("currency", currency);
        order.put("status", "created");
        order.put("gateway", "mock");
        return order;
    }

    @Override
    public boolean verifyPayment(Map<String, String> paymentData) {
        // Mock gateway always verifies successfully
        return paymentData.containsKey("orderId") && paymentData.containsKey("transactionId");
    }
}
