package com.lms.modules.ai.repository;

import com.lms.modules.ai.entity.AiChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiChatMessageRepository extends JpaRepository<AiChatMessageEntity, Long> {
    List<AiChatMessageEntity> findByUserIdOrderByCreatedAtAsc(Long userId);
    List<AiChatMessageEntity> findByThreadIdOrderByCreatedAtAsc(Long threadId);
}
