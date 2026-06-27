package com.lms.modules.user.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;
import com.lms.common.enums.Role;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email"),
    @Index(name = "idx_user_role", columnList = "role")
})
@Data
public class UserEntity {
    
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    @jakarta.persistence.Convert(converter = com.lms.common.enums.RoleConverter.class)
    private Role role;

    @Column(nullable = false)
    private boolean active = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

