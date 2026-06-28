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

    List<CourseEntity> findByCategoryAndActiveTrue(String category);

    @Query("SELECT c FROM CourseEntity c WHERE c.active = true AND (LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<CourseEntity> searchActiveByTitleOrDescription(@Param("keyword") String keyword);
}
