package com.lms.modules.course.repository;

import com.lms.modules.course.entity.SectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SectionRepository extends JpaRepository<SectionEntity, Long> {
    List<SectionEntity> findByCourseIdOrderByOrderIndexAsc(Long courseId);
    int countByCourseId(Long courseId);
}
