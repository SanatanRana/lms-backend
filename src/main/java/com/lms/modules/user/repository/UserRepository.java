package com.lms.modules.user.repository;
import com.lms.modules.user.entity.UserEntity;
import com.lms.common.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
    long countByRole(Role role);
}
