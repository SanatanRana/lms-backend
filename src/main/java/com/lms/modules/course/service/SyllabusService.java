package com.lms.modules.course.service;

import com.lms.modules.course.entity.CourseEntity;
import com.lms.modules.course.entity.CourseResourceEntity;
import com.lms.modules.course.entity.LessonEntity;
import com.lms.modules.course.entity.SectionEntity;
import com.lms.modules.course.repository.CourseRepository;
import com.lms.modules.course.repository.CourseResourceRepository;
import com.lms.modules.course.repository.LessonRepository;
import com.lms.modules.course.repository.SectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SyllabusService {

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private CourseResourceRepository resourceRepository;

    @Autowired
    private CourseRepository courseRepository;

    // ── Sections ─────────────────────────────────────────────────────

    @Transactional
    public SectionEntity addSection(Long courseId, String title) {
        CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        int nextOrder = sectionRepository.countByCourseId(courseId) + 1;

        SectionEntity section = new SectionEntity();
        section.setCourse(course);
        section.setTitle(title);
        section.setOrderIndex(nextOrder);

        return sectionRepository.save(section);
    }

    @Transactional(readOnly = true)
    public List<SectionEntity> getSections(Long courseId) {
        // Uses JOIN FETCH to load sections + lessons in a SINGLE query (eliminates N+1)
        return sectionRepository.findByCourseIdWithLessons(courseId);
    }

    @Transactional
    public SectionEntity updateSection(Long id, String title) {
        SectionEntity section = sectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Section not found"));
        section.setTitle(title);
        return sectionRepository.save(section);
    }

    @Transactional
    public void deleteSection(Long id) {
        SectionEntity section = sectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Section not found"));
        sectionRepository.delete(section);
    }

    // ── Lessons ──────────────────────────────────────────────────────

    @Transactional
    public LessonEntity addLesson(Long sectionId, String title, String description, String videoUrl) {
        SectionEntity section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new RuntimeException("Section not found"));

        int nextOrder = lessonRepository.countBySectionId(sectionId) + 1;

        LessonEntity lesson = new LessonEntity();
        lesson.setSection(section);
        lesson.setTitle(title);
        lesson.setDescription(description);
        lesson.setVideoUrl(videoUrl);
        lesson.setOrderIndex(nextOrder);

        return lessonRepository.save(lesson);
    }

    @Transactional(readOnly = true)
    public List<LessonEntity> getLessons(Long sectionId) {
        return lessonRepository.findBySectionIdOrderByOrderIndexAsc(sectionId);
    }

    @Transactional
    public LessonEntity updateLesson(Long id, String title, String description, String videoUrl) {
        LessonEntity lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));
        lesson.setTitle(title);
        lesson.setDescription(description);
        lesson.setVideoUrl(videoUrl);
        return lessonRepository.save(lesson);
    }

    @Transactional
    public void deleteLesson(Long id) {
        LessonEntity lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));
        lessonRepository.delete(lesson);
    }

    // ── Resources ────────────────────────────────────────────────────

    @Transactional
    public CourseResourceEntity addResource(Long courseId, String fileName, String fileType, String fileUrl, Long fileSize) {
        CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        CourseResourceEntity resource = new CourseResourceEntity();
        resource.setCourse(course);
        resource.setFileName(fileName);
        resource.setFileType(fileType);
        resource.setFileUrl(fileUrl);
        resource.setFileSize(fileSize);

        return resourceRepository.save(resource);
    }

    @Transactional(readOnly = true)
    public List<CourseResourceEntity> getResources(Long courseId) {
        return resourceRepository.findByCourseId(courseId);
    }

    @Transactional
    public CourseResourceEntity updateResource(Long id, String fileName, String fileType, String fileUrl, Long fileSize) {
        CourseResourceEntity resource = resourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Resource not found"));
        resource.setFileName(fileName);
        resource.setFileType(fileType);
        resource.setFileUrl(fileUrl);
        resource.setFileSize(fileSize);
        return resourceRepository.save(resource);
    }

    @Transactional
    public void deleteResource(Long id) {
        CourseResourceEntity resource = resourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Resource not found"));
        resourceRepository.delete(resource);
    }

    @Transactional(readOnly = true)
    public int getTotalLessons(Long courseId) {
        return lessonRepository.countBySectionCourseId(courseId);
    }

    @Transactional(readOnly = true)
    public Long getCourseIdForSection(Long sectionId) {
        SectionEntity section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new RuntimeException("Section not found"));
        return section.getCourse().getId();
    }
}
