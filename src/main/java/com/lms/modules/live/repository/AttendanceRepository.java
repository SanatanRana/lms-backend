package com.lms.modules.live.repository;

import com.lms.modules.live.entity.AttendanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<AttendanceEntity, Long> {
    List<AttendanceEntity> findByLiveSessionId(Long liveSessionId);
    List<AttendanceEntity> findByStudentId(Long studentId);
    Optional<AttendanceEntity> findByStudentIdAndLiveSessionIdAndLeaveTimeIsNull(Long studentId, Long liveSessionId);
}
