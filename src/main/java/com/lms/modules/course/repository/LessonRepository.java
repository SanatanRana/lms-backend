package com.lms.modules.course.repository;

import com.lms.modules.course.entity.LessonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LessonRepository extends JpaRepository<LessonEntity, Long> {
    List<LessonEntity> findBySectionIdOrderByOrderIndexAsc(Long sectionId);
    int countBySectionId(Long sectionId);
    int countBySectionCourseId(Long courseId);
}
