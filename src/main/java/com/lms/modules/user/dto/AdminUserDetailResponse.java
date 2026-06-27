package com.lms.modules.user.dto;

import com.lms.common.enums.Role;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminUserDetailResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private Role role;
    private boolean active;
    private LocalDateTime createdAt;
    
    // For Students
    private List<EnrolledCourseDetail> enrolledCourses;
    
    // For Teachers
    private List<CreatedCourseDetail> createdCourses;

    @Data
    public static class EnrolledCourseDetail {
        private Long courseId;
        private String title;
        private String category;
        private Integer progressPercent;
        private LocalDateTime enrolledAt;
    }

    @Data
    public static class CreatedCourseDetail {
        private Long courseId;
        private String title;
        private String category;
        private Double price;
        private long enrolledStudentsCount;
    }
}
