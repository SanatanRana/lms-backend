package com.lms.config;

import com.lms.common.enums.Role;
import com.lms.modules.user.entity.UserEntity;
import com.lms.modules.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * DataSeeder runs once at application startup.
 *
 * IMPORTANT DESIGN DECISION:
 * - Admin/Teacher/Student seed users are created ONLY if they don't already exist.
 * - Passwords are NEVER reset on subsequent boots. This ensures admins can
 *   change their passwords via the app without them being overwritten.
 * - To reset a password, delete the user from the DB and restart the app.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        // Drop check constraint users_chk_1 if it exists (compatibility fix)
        try {
            jdbcTemplate.execute("ALTER TABLE users DROP CHECK users_chk_1");
            System.out.println("[DataSeeder] Constraint 'users_chk_1' dropped.");
        } catch (Exception e) {
            // Expected if constraint doesn't exist — safe to ignore
        }

        // Create Default Admin (only if doesn't exist)
        if (userRepository.findByEmail("admin@lms.com").isEmpty()) {
            UserEntity admin = new UserEntity();
            admin.setEmail("admin@lms.com");
            admin.setName("Super Admin");
            admin.setPhone("0000000000");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ADMIN);
            admin.setActive(true);
            userRepository.save(admin);
            System.out.println("[DataSeeder] Default Admin created: admin@lms.com / admin123");
        } else {
            System.out.println("[DataSeeder] Admin already exists — skipped.");
        }

        // Create Default Teacher (only if doesn't exist)
        if (userRepository.findByEmail("teacher@lms.com").isEmpty()) {
            UserEntity teacher = new UserEntity();
            teacher.setEmail("teacher@lms.com");
            teacher.setName("Expert Teacher");
            teacher.setPhone("1111111111");
            teacher.setPassword(passwordEncoder.encode("teacher123"));
            teacher.setRole(Role.TEACHER);
            teacher.setActive(true);
            userRepository.save(teacher);
            System.out.println("[DataSeeder] Default Teacher created: teacher@lms.com / teacher123");
        } else {
            System.out.println("[DataSeeder] Teacher already exists — skipped.");
        }

        // Create Default Student (only if doesn't exist)
        if (userRepository.findByEmail("student@lms.com").isEmpty()) {
            UserEntity student = new UserEntity();
            student.setEmail("student@lms.com");
            student.setName("Default Student");
            student.setPhone("2222222222");
            student.setPassword(passwordEncoder.encode("student123"));
            student.setRole(Role.STUDENT);
            student.setActive(true);
            userRepository.save(student);
            System.out.println("[DataSeeder] Default Student created: student@lms.com / student123");
        } else {
            System.out.println("[DataSeeder] Student already exists — skipped.");
        }

        System.out.println("[DataSeeder] Completed successfully.");
    }
}