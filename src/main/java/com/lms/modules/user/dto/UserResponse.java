package com.lms.modules.user.dto;

import com.lms.common.enums.Role;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Safe user response DTO — never includes password hash or sensitive fields.
 * Use this instead of returning UserEntity directly from controllers.
 */
@Data
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private Role role;
    private boolean active;
    private LocalDateTime createdAt;
}
