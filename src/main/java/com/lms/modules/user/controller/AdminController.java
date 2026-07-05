package com.lms.modules.user.controller;

import com.lms.common.dto.ApiResponse;
import com.lms.modules.course.dto.AdminCourseResponse;
import com.lms.modules.course.dto.ManualEnrollmentRequest;
import com.lms.modules.course.entity.EnrollmentEntity;
import com.lms.modules.payment.entity.CouponEntity;
import com.lms.modules.payment.dto.PaymentResponse;
import com.lms.modules.user.dto.AdminUserDetailResponse;
import com.lms.modules.user.dto.AnalyticsResponse;
import com.lms.modules.user.dto.UserResponse;
import com.lms.modules.user.service.AdminService;
import com.lms.modules.auth.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.success("Users retrieved", adminService.getAllUsers()));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> getUserDetails(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("User details retrieved", adminService.getUserDetails(id)));
    }

    @PatchMapping("/users/{id}/toggle-active")
    public ResponseEntity<ApiResponse<UserResponse>> toggleUserActiveStatus(
            @PathVariable Long id, Authentication authentication) {
        UserResponse updatedUser = adminService.toggleUserActiveStatus(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("User status toggled successfully", updatedUser));
    }

    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<AnalyticsResponse>> getAnalytics() {
        return ResponseEntity.ok(ApiResponse.success("Analytics retrieved", adminService.getPlatformAnalytics()));
    }

    @GetMapping("/courses")
    public ResponseEntity<ApiResponse<List<AdminCourseResponse>>> getAllCourses() {
        return ResponseEntity.ok(ApiResponse.success("Courses retrieved", adminService.getAllCourses()));
    }

    @GetMapping("/enrollments")
    public ResponseEntity<ApiResponse<List<EnrollmentEntity>>> getAllEnrollments() {
        return ResponseEntity.ok(ApiResponse.success("Enrollments retrieved", adminService.getAllEnrollments()));
    }

    @PostMapping("/enrollments")
    public ResponseEntity<ApiResponse<EnrollmentEntity>> enrollStudentManually(
            @Valid @RequestBody ManualEnrollmentRequest request) {
        EnrollmentEntity enrollment = adminService.enrollStudentManually(request);
        return ResponseEntity.ok(ApiResponse.success("Student enrolled manually", enrollment));
    }

    @DeleteMapping("/enrollments/{id}")
    public ResponseEntity<ApiResponse<Void>> revokeEnrollment(@PathVariable Long id) {
        adminService.revokeEnrollment(id);
        return ResponseEntity.ok(ApiResponse.success("Enrollment revoked successfully", null));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getAllTransactions() {
        return ResponseEntity.ok(ApiResponse.success("Transactions retrieved", adminService.getAllTransactions()));
    }

    @GetMapping("/coupons")
    public ResponseEntity<ApiResponse<List<CouponEntity>>> getAllCoupons() {
        return ResponseEntity.ok(ApiResponse.success("Coupons retrieved", adminService.getAllCoupons()));
    }

    @PostMapping("/coupons")
    public ResponseEntity<ApiResponse<CouponEntity>> createCoupon(
            @Valid @RequestBody com.lms.modules.payment.dto.CouponRequest request) {
        CouponEntity coupon = adminService.createCoupon(request);
        return ResponseEntity.ok(ApiResponse.success("Coupon created successfully", coupon));
    }

    @PutMapping("/coupons/{id}")
    public ResponseEntity<ApiResponse<CouponEntity>> updateCoupon(
            @PathVariable Long id,
            @Valid @RequestBody com.lms.modules.payment.dto.CouponRequest request) {
        CouponEntity coupon = adminService.updateCoupon(id, request);
        return ResponseEntity.ok(ApiResponse.success("Coupon updated successfully", coupon));
    }

    @DeleteMapping("/coupons/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCoupon(@PathVariable Long id) {
        adminService.deleteCoupon(id);
        return ResponseEntity.ok(ApiResponse.success("Coupon deleted successfully", null));
    }

    @PostMapping("/users/register-teacher")
    public ResponseEntity<ApiResponse<UserResponse>> registerTeacher(
            @Valid @RequestBody RegisterRequest request) {
        UserResponse teacher = adminService.registerTeacher(request);
        return ResponseEntity.ok(ApiResponse.success("Teacher registered successfully", teacher));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long id, Authentication authentication) {
        adminService.deleteUser(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }
}
