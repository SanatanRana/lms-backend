package com.lms.modules.payment.dto;

import lombok.Data;
import java.util.Map;

@Data
public class VerifyPaymentRequest {
    private String orderId;
    private String transactionId;
    private Map<String, String> gatewayData;
}
