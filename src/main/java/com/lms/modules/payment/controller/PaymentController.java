package com.lms.modules.payment.controller;

import com.lms.common.dto.ApiResponse;
import com.lms.modules.payment.dto.CreateOrderRequest;
import com.lms.modules.payment.dto.VerifyPaymentRequest;
import com.lms.modules.payment.entity.PaymentEntity;
import com.lms.modules.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/create-order")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createOrder(
            @RequestBody CreateOrderRequest request, Authentication authentication) {
        Map<String, Object> order = paymentService.createOrder(request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Order created", order));
    }

    @PostMapping("/verify")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<PaymentEntity>> verifyPayment(
            @RequestBody VerifyPaymentRequest request, Authentication authentication) {
        PaymentEntity payment = paymentService.verifyPayment(request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Payment verified and enrolled", payment));
    }

    @GetMapping("/my-payments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<PaymentEntity>>> getMyPayments(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Payment history", paymentService.getUserPayments(authentication.getName())));
    }
}
