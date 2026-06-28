package com.lms.modules.live.controller;

import com.lms.common.dto.ApiResponse;
import com.lms.common.enums.SessionStatus;
import com.lms.modules.live.entity.LiveSessionEntity;
import com.lms.modules.live.repository.LiveSessionRepository;
import com.lms.modules.live.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.Authentication;
import com.lms.modules.user.repository.UserRepository;
import com.lms.modules.course.repository.EnrollmentRepository;
import com.lms.modules.user.entity.UserEntity;
import java.util.*;

/**
 * REST controller for live room access — supports both authenticated and guest users.
 * Provides room info via shareable token links for the pre-join lobby.
 */
@RestController
@RequestMapping("/api/live/room")
public class LiveRoomController {

    @Autowired
    private LiveSessionRepository liveSessionRepository;

    @Autowired
    private RoomService roomService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    /**
     * Get session info by room token (public endpoint — used by join link).
     * Returns session details for the pre-join lobby without exposing sensitive data.
     */
    @GetMapping("/{roomToken}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRoomByToken(
            @PathVariable String roomToken,
            Authentication authentication) {
        LiveSessionEntity session = liveSessionRepository.findByRoomToken(roomToken)
                .orElse(null);

        if (session == null) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("Live class not found. The link may be invalid or expired."));
        }

        // Security Check: If guest access is disabled, user must be logged in and enrolled in the course (or be teacher/admin)
        if (!session.getGuestAccessEnabled()) {
            if (authentication == null || !authentication.isAuthenticated() || 
                authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("This live class requires authentication. Please log in to join."));
            }

            UserEntity user = userRepository.findByEmail(authentication.getName()).orElse(null);
            if (user == null) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("User not found."));
            }

            boolean isTeacherOrAdmin = user.getRole().name().equals("TEACHER") || user.getRole().name().equals("ADMIN");
            if (!isTeacherOrAdmin) {
                boolean isEnrolled = enrollmentRepository.existsByStudentIdAndCourseId(user.getId(), session.getCourse().getId());
                if (!isEnrolled) {
                    return ResponseEntity.status(403)
                            .body(ApiResponse.error("Access denied: You are not enrolled in the course for this live session."));
                }
            }
        }

        Map<String, Object> roomInfo = new LinkedHashMap<>();
        roomInfo.put("sessionId", session.getId());
        roomInfo.put("title", session.getTitle());
        roomInfo.put("courseName", session.getCourse().getTitle());
        roomInfo.put("teacherName", session.getTeacher().getName());
        roomInfo.put("startTime", session.getStartTime());
        roomInfo.put("endTime", session.getEndTime());
        roomInfo.put("status", session.getStatus().name());
        roomInfo.put("chatEnabled", session.getChatEnabled());
        roomInfo.put("guestAccessEnabled", session.getGuestAccessEnabled());
        roomInfo.put("maxParticipants", session.getMaxParticipants());
        roomInfo.put("currentParticipants", roomService.getParticipantCount(session.getId()));
        roomInfo.put("recordingUrl", session.getRecordingUrl());
        roomInfo.put("recordingStatus", session.getRecordingStatus().name());

        return ResponseEntity.ok(ApiResponse.success("Room info", roomInfo));
    }

    /**
     * Guest join — validates room token and returns minimal session info.
     * No authentication required, but guest access must be enabled on the session.
     */
    @PostMapping("/{roomToken}/guest-join")
    public ResponseEntity<ApiResponse<Map<String, Object>>> guestJoin(
            @PathVariable String roomToken,
            @RequestBody Map<String, String> body) {

        String guestName = body.getOrDefault("name", "Guest");
        if (guestName.isBlank()) guestName = "Guest";

        LiveSessionEntity session = liveSessionRepository.findByRoomToken(roomToken)
                .orElse(null);

        if (session == null) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("Live class not found."));
        }

        if (!session.getGuestAccessEnabled()) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Guest access is not enabled for this class. Please log in to join."));
        }

        if (session.getStatus() == SessionStatus.ENDED) {
            return ResponseEntity.status(400)
                    .body(ApiResponse.error("This session has already ended."));
        }

        int currentCount = roomService.getParticipantCount(session.getId());
        if (currentCount >= session.getMaxParticipants()) {
            return ResponseEntity.status(400)
                    .body(ApiResponse.error("This classroom is full (" + session.getMaxParticipants() + " max)."));
        }

        Map<String, Object> joinInfo = new LinkedHashMap<>();
        joinInfo.put("sessionId", session.getId());
        joinInfo.put("guestName", guestName);
        joinInfo.put("role", "GUEST");
        joinInfo.put("title", session.getTitle());
        joinInfo.put("status", session.getStatus().name());

        return ResponseEntity.ok(ApiResponse.success("Guest access granted", joinInfo));
    }

    /**
     * Fallback endpoint to retrieve session details directly by session database ID.
     * Enforces active enrollment security checks for students if guest access is disabled.
     */
    @GetMapping("/token-bypass-fallback/{sessionId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRoomBySessionId(
            @PathVariable Long sessionId,
            Authentication authentication) {
        LiveSessionEntity session = liveSessionRepository.findById(sessionId)
                .orElse(null);

        if (session == null) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("Live class not found."));
        }

        // Security Check: If guest access is disabled, user must be logged in and enrolled in the course (or be teacher/admin)
        if (!session.getGuestAccessEnabled()) {
            if (authentication == null || !authentication.isAuthenticated() || 
                authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("This live class requires authentication. Please log in to join."));
            }

            UserEntity user = userRepository.findByEmail(authentication.getName()).orElse(null);
            if (user == null) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("User not found."));
            }

            boolean isTeacherOrAdmin = user.getRole().name().equals("TEACHER") || user.getRole().name().equals("ADMIN");
            if (!isTeacherOrAdmin) {
                boolean isEnrolled = enrollmentRepository.existsByStudentIdAndCourseId(user.getId(), session.getCourse().getId());
                if (!isEnrolled) {
                    return ResponseEntity.status(403)
                            .body(ApiResponse.error("Access denied: You are not enrolled in the course for this live session."));
                }
            }
        }

        Map<String, Object> roomInfo = new LinkedHashMap<>();
        roomInfo.put("title", session.getTitle());
        roomInfo.put("courseName", session.getCourse().getTitle());
        roomInfo.put("teacherName", session.getTeacher().getName());

        return ResponseEntity.ok(ApiResponse.success("Room info", roomInfo));
    }
}
