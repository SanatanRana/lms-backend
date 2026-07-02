package com.lms.modules.live.repository;

import com.lms.modules.live.entity.LiveSessionEntity;
import com.lms.common.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LiveSessionRepository extends JpaRepository<LiveSessionEntity, Long> {
    List<LiveSessionEntity> findByCourseId(Long courseId);
    List<LiveSessionEntity> findByTeacherId(Long teacherId);
    List<LiveSessionEntity> findByStatus(SessionStatus status);
    List<LiveSessionEntity> findByCourseIdAndStatus(Long courseId, SessionStatus status);
    List<LiveSessionEntity> findByCourseIdIn(List<Long> courseIds);
    Optional<LiveSessionEntity> findByRoomToken(String roomToken);
    List<LiveSessionEntity> findByStatusAndEndTimeBefore(SessionStatus status, LocalDateTime endTime);
}
