package com.lms.modules.course.repository;

import com.lms.modules.course.entity.CourseResourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CourseResourceRepository extends JpaRepository<CourseResourceEntity, Long> {
    List<CourseResourceEntity> findByCourseId(Long courseId);
}
