package com.lms.modules.auth.dto;

import com.lms.common.enums.Role;
import lombok.Data;

@Data
public class RegisterRequest {
    private String name;
    private String email;
    private String phone;
    private String password;
    private Role role; // We will pass 'STUDENT' from frontend automatically
}