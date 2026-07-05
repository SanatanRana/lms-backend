package com.lms.modules.payment.gateway;

import com.lms.common.strategy.PaymentGateway;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Razorpay Payment Gateway implementation.
 *
 * ACTIVATION: Registered when razorpay.key.id and razorpay.key.secret are configured
 * AND payment.gateway=razorpay is set in application.properties.
 *
 * HOW RAZORPAY WORKS:
 * 1. createOrder() → Creates an order on Razorpay server, returns order ID
 * 2. Frontend opens Razorpay checkout with the order ID
 * 3. User pays → Razorpay returns razorpay_payment_id, razorpay_order_id, razorpay_signature
 * 4. verifyPayment() → Verifies HMAC signature to confirm payment is genuine
 */
@Component
@ConditionalOnProperty(name = "razorpay.key.id")
public class RazorpayGateway implements PaymentGateway {

    private static final Logger logger = LoggerFactory.getLogger(RazorpayGateway.class);

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    private RazorpayClient razorpayClient;

    @PostConstruct
    public void init() {
        try {
            if (keyId != null && !keyId.isBlank() && keySecret != null && !keySecret.isBlank()) {
                this.razorpayClient = new RazorpayClient(keyId, keySecret);
                logger.info("[RazorpayGateway] Initialized with key: {}...{}", 
                    keyId.substring(0, Math.min(8, keyId.length())),
                    keyId.length() > 8 ? keyId.substring(keyId.length() - 4) : "");
            }
        } catch (RazorpayException e) {
            logger.error("[RazorpayGateway] Failed to initialize: {}", e.getMessage());
            throw new RuntimeException("Razorpay initialization failed: " + e.getMessage());
        }
    }

    @Override
    public String getGatewayId() {
        return "razorpay";
    }

    @Override
    public Map<String, String> createOrder(Double amount, String currency, String description) {
        try {
            // Razorpay requires amount in smallest currency unit (paise for INR)
            long amountInPaise = Math.round(amount * 100);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", currency != null ? currency : "INR");
            orderRequest.put("receipt", "receipt_" + System.currentTimeMillis());
            orderRequest.put("notes", new JSONObject().put("description", description));

            Order order = razorpayClient.orders.create(orderRequest);

            Map<String, String> result = new HashMap<>();
            result.put("orderId", order.get("id").toString());       // "order_xxx" for tracking in our DB
            result.put("razorpayOrderId", order.get("id").toString()); // Same, sent to frontend
            result.put("amount", String.valueOf(amountInPaise));
            result.put("currency", order.get("currency").toString());
            result.put("status", order.get("status").toString());
            result.put("keyId", keyId); // Frontend needs this to open checkout

            logger.info("[RazorpayGateway] Order created: {}", result.get("orderId"));
            return result;
        } catch (RazorpayException e) {
            logger.error("[RazorpayGateway] Order creation failed: {}", e.getMessage());
            throw new RuntimeException("Failed to create payment order. Please try again.");
        }
    }

    /**
     * Verifies Razorpay payment signature using HMAC-SHA256.
     *
     * Required keys in paymentData:
     * - "razorpay_order_id": The Razorpay order ID
     * - "razorpay_payment_id": Payment ID returned after successful payment
     * - "razorpay_signature": HMAC signature from Razorpay
     *
     * If orderId and transactionId are present (legacy format), they map to
     * razorpay_order_id and razorpay_payment_id respectively.
     */
    @Override
    public boolean verifyPayment(Map<String, String> paymentData) {
        try {
            // Support both Razorpay field names and generic field names
            String orderId = paymentData.getOrDefault("razorpay_order_id",
                    paymentData.get("orderId"));
            String paymentId = paymentData.getOrDefault("razorpay_payment_id",
                    paymentData.get("transactionId"));
            String signature = paymentData.getOrDefault("razorpay_signature",
                    paymentData.get("signature"));

            if (orderId == null || paymentId == null || signature == null) {
                logger.warn("[RazorpayGateway] Missing payment data fields for verification");
                return false;
            }

            // Razorpay signature verification: HMAC-SHA256(orderId + "|" + paymentId, keySecret)
            String payload = orderId + "|" + paymentId;
            String computedSignature = computeHmacSHA256(payload, keySecret);

            boolean isValid = computedSignature.equals(signature);
            if (!isValid) {
                logger.warn("[RazorpayGateway] Signature mismatch for order: {}", orderId);
            }
            return isValid;
        } catch (Exception e) {
            logger.error("[RazorpayGateway] Verification error: {}", e.getMessage());
            return false;
        }
    }

    private String computeHmacSHA256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        // Convert to hex string
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
