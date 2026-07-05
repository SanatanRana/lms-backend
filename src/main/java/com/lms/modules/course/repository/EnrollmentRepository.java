package com.lms.modules.course.repository;

import com.lms.modules.course.entity.EnrollmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<EnrollmentEntity, Long> {

    Optional<EnrollmentEntity> findByStudentIdAndCourseId(Long studentId, Long courseId);

    List<EnrollmentEntity> findByStudentId(Long studentId);

    List<EnrollmentEntity> findByCourseId(Long courseId);

    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    @Query("SELECT COUNT(e) FROM EnrollmentEntity e WHERE e.course.id = :courseId")
    long countByCourseId(@Param("courseId") Long courseId);

    @Query("SELECT e.course.id, COUNT(e) FROM EnrollmentEntity e WHERE e.course.id IN :courseIds GROUP BY e.course.id")
    List<Object[]> countEnrollmentsByCourseIds(@Param("courseIds") List<Long> courseIds);

    @Query("SELECT e FROM EnrollmentEntity e JOIN FETCH e.course c JOIN FETCH c.teacher JOIN FETCH e.student WHERE e.student.id = :studentId")
    List<EnrollmentEntity> findByStudentIdWithCourse(@Param("studentId") Long studentId);
}
