package com.lms.modules.course.repository;

import com.lms.modules.course.entity.CourseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<CourseEntity, Long> {

    List<CourseEntity> findByTeacherId(Long teacherId);

    List<CourseEntity> findByActiveTrue();

    /**
     * Fetches all active courses with teacher in ONE query.
     * Eliminates N+1: previously each course.getTeacher() triggered a lazy load.
     */
    @Query("SELECT c FROM CourseEntity c JOIN FETCH c.teacher t WHERE c.active = true ORDER BY c.createdAt DESC")
    List<CourseEntity> findAllActiveWithTeacher();

    /**
     * Returns section counts per course in a single aggregation query.
     * Used instead of course.getSections().size() which triggers lazy loads.
     */
    @Query("SELECT c.id, COUNT(s) FROM CourseEntity c LEFT JOIN c.sections s WHERE c.id IN :courseIds GROUP BY c.id")
    List<Object[]> countSectionsByCourseIds(@Param("courseIds") List<Long> courseIds);

    List<CourseEntity> findByCategoryAndActiveTrue(String category);

    @Query("SELECT c FROM CourseEntity c WHERE c.active = true AND (LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<CourseEntity> searchActiveByTitleOrDescription(@Param("keyword") String keyword);
}
