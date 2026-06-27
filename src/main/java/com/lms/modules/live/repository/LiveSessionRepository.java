package com.lms.modules.live.repository;

import com.lms.modules.live.entity.LiveSessionEntity;
import com.lms.common.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LiveSessionRepository extends JpaRepository<LiveSessionEntity, Long> {
    List<LiveSessionEntity> findByCourseId(Long courseId);
    List<LiveSessionEntity> findByTeacherId(Long teacherId);
    List<LiveSessionEntity> findByStatus(SessionStatus status);
    List<LiveSessionEntity> findByCourseIdAndStatus(Long courseId, SessionStatus status);
    List<LiveSessionEntity> findByCourseIdIn(List<Long> courseIds);
}
