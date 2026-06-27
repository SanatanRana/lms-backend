package com.lms.common.strategy;

import java.util.Map;

/**
 * SYSTEM DESIGN: Strategy Pattern for Payment Gateways
 * ─────────────────────────────────────────────────────
 * To add a new payment provider (e.g., Stripe, PayPal):
 *   1. Create a new class implementing this interface.
 *   2. Annotate it with @Component.
 *   3. Done – the PaymentService will discover it via Spring DI.
 *   NO existing code is modified.
 */
public interface PaymentGateway {

    /** Unique identifier for this gateway (e.g., "razorpay", "stripe", "mock") */
    String getGatewayId();

    /** Create a payment order and return gateway-specific metadata */
    Map<String, String> createOrder(Double amount, String currency, String description);

    /** Verify a payment using gateway-specific signature/token */
    boolean verifyPayment(Map<String, String> paymentData);
}
