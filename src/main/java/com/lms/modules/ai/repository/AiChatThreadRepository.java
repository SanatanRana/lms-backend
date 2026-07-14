package com.lms.modules.ai.repository;

import com.lms.modules.ai.entity.AiChatThreadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiChatThreadRepository extends JpaRepository<AiChatThreadEntity, Long> {
    List<AiChatThreadEntity> findByUserIdAndCourseIdOrderByCreatedAtDesc(Long userId, Long courseId);
}
