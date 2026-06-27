package com.lms.modules.assignment.repository;

import com.lms.modules.assignment.entity.AssignmentSubmissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmissionEntity, Long> {
    List<AssignmentSubmissionEntity> findByAssignmentId(Long assignmentId);
    List<AssignmentSubmissionEntity> findByStudentId(Long studentId);
    Optional<AssignmentSubmissionEntity> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);
}
