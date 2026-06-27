package com.lms.modules.course.controller;

import com.lms.common.dto.ApiResponse;
import com.lms.modules.course.entity.EnrollmentEntity;
import com.lms.modules.course.service.EnrollmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    @Autowired
    private EnrollmentService enrollmentService;

    @PostMapping("/enroll/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<EnrollmentEntity>> enroll(
            @PathVariable Long courseId, Authentication authentication) {
        EnrollmentEntity enrollment = enrollmentService.enrollStudent(courseId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Enrolled successfully", enrollment));
    }

    @GetMapping("/my-courses")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<EnrollmentEntity>>> getMyEnrollments(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Enrolled courses", enrollmentService.getStudentEnrollments(authentication.getName())));
    }

    @PatchMapping("/progress/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<EnrollmentEntity>> updateProgress(
            @PathVariable Long courseId, @RequestParam int percent, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Progress updated", enrollmentService.updateProgress(courseId, authentication.getName(), percent)));
    }

    @GetMapping("/check/{courseId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Boolean>> checkEnrollment(
            @PathVariable Long courseId, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Enrollment status", enrollmentService.isEnrolled(courseId, authentication.getName())));
    }
}
