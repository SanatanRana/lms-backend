package com.lms.modules.course.service;

import com.lms.common.enums.CourseType;
import com.lms.common.event.DomainEvents;
import com.lms.modules.course.dto.CourseRequest;
import com.lms.modules.course.dto.CourseResponse;
import com.lms.modules.course.entity.CourseEntity;
import com.lms.modules.course.repository.CourseRepository;
import com.lms.modules.user.entity.UserEntity;
import com.lms.modules.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Transactional
    public CourseResponse createCourse(CourseRequest request, String teacherEmail) {
        UserEntity teacher = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        CourseEntity course = new CourseEntity();
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setPrice(request.getPrice() != null ? request.getPrice() : 0.0);
        course.setDiscountPrice(request.getDiscountPrice());
        course.setThumbnailUrl(request.getThumbnailUrl());
        course.setIntroVideoUrl(request.getIntroVideoUrl());
        course.setCourseType(request.getPrice() != null && request.getPrice() > 0 ? CourseType.PAID : CourseType.FREE);
        course.setCategory(request.getCategory());
        course.setTeacher(teacher);

        CourseEntity saved = courseRepository.save(course);

        // OCP: publish event
        eventPublisher.publishEvent(new DomainEvents.CourseCreatedEvent(
                saved.getId(), saved.getTitle(), teacher.getId()
        ));

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> getAllCourses() {
        return courseRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CourseResponse getCourseById(Long id) {
        CourseEntity course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + id));
        return mapToResponse(course);
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> getCoursesByTeacher(String teacherEmail) {
        UserEntity teacher = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));
        return courseRepository.findByTeacherId(teacher.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> searchCourses(String keyword) {
        return courseRepository.searchByTitleOrDescription(keyword).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> getCoursesByCategory(String category) {
        return courseRepository.findByCategory(category).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lightweight DTO mapping – keeps serialized payloads small.
     * Avoids sending the full teacher entity with password hash.
     */
    private CourseResponse mapToResponse(CourseEntity course) {
        CourseResponse r = new CourseResponse();
        r.setId(course.getId());
        r.setTitle(course.getTitle());
        r.setDescription(course.getDescription());
        r.setPrice(course.getPrice());
        r.setDiscountPrice(course.getDiscountPrice());
        r.setThumbnailUrl(course.getThumbnailUrl());
        r.setIntroVideoUrl(course.getIntroVideoUrl());
        r.setCourseType(course.getCourseType() != null ? course.getCourseType().name() : "PAID");
        r.setCategory(course.getCategory());
        r.setTeacherName(course.getTeacher() != null ? course.getTeacher().getName() : null);
        r.setTeacherId(course.getTeacher() != null ? course.getTeacher().getId() : null);
        r.setCreatedAt(course.getCreatedAt());
        r.setSectionCount(course.getSections() != null ? course.getSections().size() : 0);
        return r;
    }
}