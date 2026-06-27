package com.lms.modules.payment.dto;

import lombok.Data;

@Data
public class CreateOrderRequest {
    private Long courseId;
    private String couponCode; // optional
}
