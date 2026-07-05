package com.lms.modules.course.repository;

import com.lms.modules.course.entity.SectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SectionRepository extends JpaRepository<SectionEntity, Long> {

    /**
     * Loads sections WITH their lessons in a single JOIN query.
     * Eliminates the N+1 problem where each section's lessons were fetched separately.
     * DISTINCT is needed because JOIN FETCH can produce duplicate section rows.
     */
    @Query("SELECT DISTINCT s FROM SectionEntity s LEFT JOIN FETCH s.lessons l WHERE s.course.id = :courseId ORDER BY s.orderIndex ASC")
    List<SectionEntity> findByCourseIdWithLessons(@Param("courseId") Long courseId);

    // Keep original for backward compatibility (used by countByCourseId internally)
    List<SectionEntity> findByCourseIdOrderByOrderIndexAsc(Long courseId);

    int countByCourseId(Long courseId);
}
