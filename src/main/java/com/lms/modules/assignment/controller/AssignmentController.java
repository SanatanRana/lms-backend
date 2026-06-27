package com.lms.modules.assignment.controller;

import com.lms.common.dto.ApiResponse;
import com.lms.modules.assignment.dto.AssignmentRequest;
import com.lms.modules.assignment.dto.SubmissionRequest;
import com.lms.modules.assignment.entity.AssignmentEntity;
import com.lms.modules.assignment.entity.AssignmentSubmissionEntity;
import com.lms.modules.assignment.service.AssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    @Autowired
    private AssignmentService assignmentService;

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<AssignmentEntity>> createAssignment(@RequestBody AssignmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Assignment created", assignmentService.createAssignment(request)));
    }

    @GetMapping("/course/{courseId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<AssignmentEntity>>> getCourseAssignments(@PathVariable Long courseId) {
        return ResponseEntity.ok(ApiResponse.success("Assignments", assignmentService.getCourseAssignments(courseId)));
    }

    @PostMapping("/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<AssignmentSubmissionEntity>> submitAssignment(
            @RequestBody SubmissionRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Submitted",
                assignmentService.submitAssignment(request, authentication.getName())));
    }

    @GetMapping("/{assignmentId}/submissions")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<AssignmentSubmissionEntity>>> getSubmissions(@PathVariable Long assignmentId) {
        return ResponseEntity.ok(ApiResponse.success("Submissions", assignmentService.getSubmissions(assignmentId)));
    }

    @PatchMapping("/submissions/{submissionId}/grade")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<AssignmentSubmissionEntity>> gradeSubmission(
            @PathVariable Long submissionId, @RequestParam Integer grade, @RequestParam(required = false) String feedback) {
        return ResponseEntity.ok(ApiResponse.success("Graded", assignmentService.gradeSubmission(submissionId, grade, feedback)));
    }
}
