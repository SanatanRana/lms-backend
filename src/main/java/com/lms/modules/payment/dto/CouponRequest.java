package com.lms.modules.payment.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CouponRequest {
    private String code;
    private Integer discountPercent;
    private LocalDate expiryDate;
    private Integer maxUses;
    private boolean active = true;
}
