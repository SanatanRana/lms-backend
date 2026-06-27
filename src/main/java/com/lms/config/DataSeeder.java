package com.lms.config;

import com.lms.common.enums.Role;
import com.lms.modules.user.entity.UserEntity;
import com.lms.modules.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        // Drop check constraint users_chk_1 if it exists
        try {
            jdbcTemplate.execute("ALTER TABLE users DROP CHECK users_chk_1");
            System.out.println("Constraint 'users_chk_1' dropped successfully.");
        } catch (Exception e) {
            System.out.println("Could not drop check constraint (expected if it is already dropped): " + e.getMessage());
        }
        
        // 1. Create or Update Default Admin
        UserEntity admin = userRepository.findByEmail("admin@lms.com").orElse(null);
        if (admin == null) {
            admin = new UserEntity();
            admin.setEmail("admin@lms.com");
            System.out.println("Creating default Admin...");
        } else {
            System.out.println("Updating existing Admin seeder settings...");
        }
        admin.setName("Super Admin");
        admin.setPhone("0000000000");
        admin.setPassword(passwordEncoder.encode("admin123")); // Reset to admin123 hashed
        admin.setRole(Role.ADMIN);
        admin.setActive(true); // Ensure they are active!
        userRepository.save(admin);

        // 2. Create or Update Default Teacher
        UserEntity teacher = userRepository.findByEmail("teacher@lms.com").orElse(null);
        if (teacher == null) {
            teacher = new UserEntity();
            teacher.setEmail("teacher@lms.com");
            System.out.println("Creating default Teacher...");
        } else {
            System.out.println("Updating existing Teacher seeder settings...");
        }
        teacher.setName("Expert Teacher");
        teacher.setPhone("1111111111");
        teacher.setPassword(passwordEncoder.encode("teacher123")); // Reset to teacher123 hashed
        teacher.setRole(Role.TEACHER);
        teacher.setActive(true); // Ensure they are active!
        userRepository.save(teacher);

        // 3. Create or Update Default Student
        UserEntity student = userRepository.findByEmail("student@lms.com").orElse(null);
        if (student == null) {
            student = new UserEntity();
            student.setEmail("student@lms.com");
            System.out.println("Creating default Student...");
        } else {
            System.out.println("Updating existing Student seeder settings...");
        }
        student.setName("Default Student");
        student.setPhone("2222222222");
        student.setPassword(passwordEncoder.encode("student123")); // Reset to student123 hashed
        student.setRole(Role.STUDENT);
        student.setActive(true); // Ensure they are active!
        userRepository.save(student);

        System.out.println("Data seeding / alignment successfully completed.");
    }
}