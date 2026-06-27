package com.lms.modules.course.controller;

import com.lms.common.dto.ApiResponse;
import com.lms.modules.course.dto.LessonRequest;
import com.lms.modules.course.dto.ResourceRequest;
import com.lms.modules.course.dto.SectionRequest;
import com.lms.modules.course.entity.CourseResourceEntity;
import com.lms.modules.course.entity.LessonEntity;
import com.lms.modules.course.entity.SectionEntity;
import com.lms.modules.course.service.SyllabusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SyllabusController {

    @Autowired
    private SyllabusService syllabusService;

    @Autowired
    private com.lms.modules.course.service.EnrollmentService enrollmentService;

    // ── Sections ─────────────────────────────────────────────────────

    @PostMapping("/courses/{courseId}/sections")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<SectionEntity>> addSection(
            @PathVariable Long courseId,
            @RequestBody SectionRequest request) {
        SectionEntity section = syllabusService.addSection(courseId, request.getTitle());
        return ResponseEntity.ok(ApiResponse.success("Section added successfully", section));
    }

    @GetMapping("/courses/{courseId}/sections")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<SectionEntity>>> getSections(
            @PathVariable Long courseId,
            Authentication authentication) {
        if (!enrollmentService.isEnrolled(courseId, authentication.getName())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: You are not enrolled in this course");
        }
        List<SectionEntity> sections = syllabusService.getSections(courseId);
        return ResponseEntity.ok(ApiResponse.success("Sections retrieved successfully", sections));
    }

    @PutMapping("/sections/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<SectionEntity>> updateSection(
            @PathVariable Long id,
            @RequestBody SectionRequest request) {
        SectionEntity section = syllabusService.updateSection(id, request.getTitle());
        return ResponseEntity.ok(ApiResponse.success("Section updated successfully", section));
    }

    @DeleteMapping("/sections/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSection(@PathVariable Long id) {
        syllabusService.deleteSection(id);
        return ResponseEntity.ok(ApiResponse.success("Section deleted successfully", null));
    }

    // ── Lessons ──────────────────────────────────────────────────────

    @PostMapping("/sections/{sectionId}/lessons")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<LessonEntity>> addLesson(
            @PathVariable Long sectionId,
            @RequestBody LessonRequest request) {
        LessonEntity lesson = syllabusService.addLesson(
                sectionId,
                request.getTitle(),
                request.getDescription(),
                request.getVideoUrl()
        );
        return ResponseEntity.ok(ApiResponse.success("Lesson added successfully", lesson));
    }

    @GetMapping("/sections/{sectionId}/lessons")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<LessonEntity>>> getLessons(
            @PathVariable Long sectionId,
            Authentication authentication) {
        Long courseId = syllabusService.getCourseIdForSection(sectionId);
        if (!enrollmentService.isEnrolled(courseId, authentication.getName())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: You are not enrolled in this course");
        }
        List<LessonEntity> lessons = syllabusService.getLessons(sectionId);
        return ResponseEntity.ok(ApiResponse.success("Lessons retrieved successfully", lessons));
    }

    @PutMapping("/lessons/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<LessonEntity>> updateLesson(
            @PathVariable Long id,
            @RequestBody LessonRequest request) {
        LessonEntity lesson = syllabusService.updateLesson(
                id,
                request.getTitle(),
                request.getDescription(),
                request.getVideoUrl()
        );
        return ResponseEntity.ok(ApiResponse.success("Lesson updated successfully", lesson));
    }

    @DeleteMapping("/lessons/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteLesson(@PathVariable Long id) {
        syllabusService.deleteLesson(id);
        return ResponseEntity.ok(ApiResponse.success("Lesson deleted successfully", null));
    }

    // ── Resources ────────────────────────────────────────────────────

    @PostMapping("/courses/{courseId}/resources")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CourseResourceEntity>> addResource(
            @PathVariable Long courseId,
            @RequestBody ResourceRequest request) {
        CourseResourceEntity resource = syllabusService.addResource(
                courseId,
                request.getFileName(),
                request.getFileType(),
                request.getFileUrl(),
                request.getFileSize()
        );
        return ResponseEntity.ok(ApiResponse.success("Resource added successfully", resource));
    }

    @GetMapping("/courses/{courseId}/resources")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<CourseResourceEntity>>> getResources(
            @PathVariable Long courseId,
            Authentication authentication) {
        if (!enrollmentService.isEnrolled(courseId, authentication.getName())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: You are not enrolled in this course");
        }
        List<CourseResourceEntity> resources = syllabusService.getResources(courseId);
        return ResponseEntity.ok(ApiResponse.success("Resources retrieved successfully", resources));
    }

    @PutMapping("/resources/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CourseResourceEntity>> updateResource(
            @PathVariable Long id,
            @RequestBody ResourceRequest request) {
        CourseResourceEntity resource = syllabusService.updateResource(
                id,
                request.getFileName(),
                request.getFileType(),
                request.getFileUrl(),
                request.getFileSize()
        );
        return ResponseEntity.ok(ApiResponse.success("Resource updated successfully", resource));
    }

    @DeleteMapping("/resources/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteResource(@PathVariable Long id) {
        syllabusService.deleteResource(id);
        return ResponseEntity.ok(ApiResponse.success("Resource deleted successfully", null));
    }
}
