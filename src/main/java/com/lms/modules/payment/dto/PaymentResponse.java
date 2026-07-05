package com.lms.modules.payment.dto;

import com.lms.common.enums.PaymentStatus;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Safe payment response DTO.
 * Does NOT include full user or course objects — only IDs and display names.
 */
@Data
public class PaymentResponse {
    private Long id;
    private Long userId;
    private String userName;
    private Long courseId;
    private String courseTitle;
    private Double amount;
    private String paymentMethod;
    private String transactionId;
    private String gatewayOrderId;
    private PaymentStatus paymentStatus;
    private LocalDateTime createdAt;
}
