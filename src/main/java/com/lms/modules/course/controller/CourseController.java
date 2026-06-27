package com.lms.modules.course.controller;

import com.lms.common.dto.ApiResponse;
import com.lms.modules.course.dto.CourseRequest;
import com.lms.modules.course.dto.CourseResponse;
import com.lms.modules.course.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    @Autowired
    private CourseService courseService;

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CourseResponse>> createCourse(
            @RequestBody CourseRequest request, Authentication authentication) {
        String email = authentication.getName();
        CourseResponse course = courseService.createCourse(request, email);
        return ResponseEntity.ok(ApiResponse.success("Course created successfully", course));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getAllCourses() {
        return ResponseEntity.ok(ApiResponse.success("Courses fetched", courseService.getAllCourses()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> getCourseById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Course details fetched", courseService.getCourseById(id)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> searchCourses(@RequestParam String keyword) {
        return ResponseEntity.ok(ApiResponse.success("Search results", courseService.searchCourses(keyword)));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getCoursesByCategory(@PathVariable String category) {
        return ResponseEntity.ok(ApiResponse.success("Category courses", courseService.getCoursesByCategory(category)));
    }

    @GetMapping("/my-courses")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getTeacherCourses(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Teacher courses", courseService.getCoursesByTeacher(authentication.getName())));
    }
}