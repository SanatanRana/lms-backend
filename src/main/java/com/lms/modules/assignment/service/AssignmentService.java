package com.lms.modules.assignment.service;

import com.lms.common.event.DomainEvents;
import com.lms.modules.assignment.dto.AssignmentRequest;
import com.lms.modules.assignment.dto.SubmissionRequest;
import com.lms.modules.assignment.entity.AssignmentEntity;
import com.lms.modules.assignment.entity.AssignmentSubmissionEntity;
import com.lms.modules.assignment.repository.AssignmentRepository;
import com.lms.modules.assignment.repository.AssignmentSubmissionRepository;
import com.lms.modules.course.entity.CourseEntity;
import com.lms.modules.course.repository.CourseRepository;
import com.lms.modules.user.entity.UserEntity;
import com.lms.modules.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AssignmentService {

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private AssignmentSubmissionRepository submissionRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Transactional
    public AssignmentEntity createAssignment(AssignmentRequest request) {
        CourseEntity course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));

        AssignmentEntity assignment = new AssignmentEntity();
        assignment.setCourse(course);
        assignment.setTitle(request.getTitle());
        assignment.setDescription(request.getDescription());
        assignment.setDueDate(request.getDueDate());
        assignment.setMaxScore(request.getMaxScore() != null ? request.getMaxScore() : 100);

        return assignmentRepository.save(assignment);
    }

    @Transactional(readOnly = true)
    public List<AssignmentEntity> getCourseAssignments(Long courseId) {
        return assignmentRepository.findByCourseId(courseId);
    }

    @Transactional
    public AssignmentSubmissionEntity submitAssignment(SubmissionRequest request, String studentEmail) {
        UserEntity student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        AssignmentEntity assignment = assignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        // Check if already submitted
        if (submissionRepository.findByAssignmentIdAndStudentId(assignment.getId(), student.getId()).isPresent()) {
            throw new RuntimeException("You have already submitted this assignment");
        }

        AssignmentSubmissionEntity submission = new AssignmentSubmissionEntity();
        submission.setAssignment(assignment);
        submission.setStudent(student);
        submission.setSubmissionUrl(request.getSubmissionUrl());
        submission.setAnswerText(request.getAnswerText());

        AssignmentSubmissionEntity saved = submissionRepository.save(submission);

        eventPublisher.publishEvent(new DomainEvents.AssignmentSubmittedEvent(
                saved.getId(), student.getId(), assignment.getId()
        ));

        return saved;
    }

    @Transactional(readOnly = true)
    public List<AssignmentSubmissionEntity> getSubmissions(Long assignmentId) {
        return submissionRepository.findByAssignmentId(assignmentId);
    }

    @Transactional
    public AssignmentSubmissionEntity gradeSubmission(Long submissionId, Integer grade, String feedback) {
        AssignmentSubmissionEntity submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));
        submission.setGrade(grade);
        submission.setFeedback(feedback);
        return submissionRepository.save(submission);
    }

    @org.springframework.context.event.EventListener
    @Transactional
    public void onCourseDeleted(DomainEvents.CourseDeletedEvent event) {
        List<AssignmentEntity> assignments = assignmentRepository.findByCourseId(event.courseId());
        for (AssignmentEntity assignment : assignments) {
            List<AssignmentSubmissionEntity> submissions = submissionRepository.findByAssignmentId(assignment.getId());
            submissionRepository.deleteAll(submissions);
        }
        assignmentRepository.deleteAll(assignments);
    }
}
