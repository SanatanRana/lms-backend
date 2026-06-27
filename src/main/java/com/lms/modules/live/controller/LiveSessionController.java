package com.lms.modules.live.controller;

import com.lms.common.dto.ApiResponse;
import com.lms.modules.live.dto.LiveSessionRequest;
import com.lms.modules.live.entity.AttendanceEntity;
import com.lms.modules.live.entity.LiveSessionEntity;
import com.lms.modules.live.service.LiveSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/live")
public class LiveSessionController {

    @Autowired
    private LiveSessionService liveSessionService;

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<LiveSessionEntity>> createSession(
            @RequestBody LiveSessionRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Session scheduled",
                liveSessionService.createSession(request, authentication.getName())));
    }

    @PatchMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<LiveSessionEntity>> startSession(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Session started", liveSessionService.startSession(id)));
    }

    @PatchMapping("/{id}/end")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<LiveSessionEntity>> endSession(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Session ended", liveSessionService.endSession(id)));
    }

    @PostMapping("/{id}/join")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<AttendanceEntity>> joinSession(
            @PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Joined session",
                liveSessionService.joinSession(id, authentication.getName())));
    }

    @PostMapping("/{id}/leave")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<AttendanceEntity>> leaveSession(
            @PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Left session",
                liveSessionService.leaveSession(id, authentication.getName())));
    }

    @GetMapping("/course/{courseId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<LiveSessionEntity>>> getSessionsByCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(ApiResponse.success("Sessions", liveSessionService.getSessionsByCourse(courseId)));
    }

    @GetMapping("/enrolled")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<LiveSessionEntity>>> getEnrolledSessions(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Enrolled sessions",
                liveSessionService.getSessionsForEnrolledCourses(authentication.getName())));
    }

    @GetMapping("/my-sessions")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<LiveSessionEntity>>> getTeacherSessions(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Teacher sessions",
                liveSessionService.getSessionsByTeacher(authentication.getName())));
    }

    @GetMapping("/{id}/attendance")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<AttendanceEntity>>> getAttendance(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Attendance", liveSessionService.getSessionAttendance(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<LiveSessionEntity>> updateSession(
            @PathVariable Long id, @RequestBody LiveSessionRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Session updated",
                liveSessionService.updateSession(id, request, authentication.getName())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSession(
            @PathVariable Long id, Authentication authentication) {
        liveSessionService.deleteSession(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Session deleted successfully", null));
    }
}
