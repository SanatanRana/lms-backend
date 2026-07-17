package com.lms.modules.assignment.repository;

import com.lms.modules.assignment.entity.AssignmentSubmissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmissionEntity, Long> {
    @org.springframework.data.jpa.repository.Query("SELECT s FROM AssignmentSubmissionEntity s LEFT JOIN FETCH s.assignment LEFT JOIN FETCH s.student WHERE s.assignment.id = :assignmentId")
    List<AssignmentSubmissionEntity> findByAssignmentId(@org.springframework.data.repository.query.Param("assignmentId") Long assignmentId);

    List<AssignmentSubmissionEntity> findByStudentId(Long studentId);
    Optional<AssignmentSubmissionEntity> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);
}
