package com.lms.modules.ai.service;

import com.lms.common.strategy.AiProvider;
import com.lms.modules.ai.entity.AiChatMessageEntity;
import com.lms.modules.ai.repository.AiChatMessageRepository;
import com.lms.modules.user.entity.UserEntity;
import com.lms.modules.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AiChatService {

    @Autowired
    private AiChatMessageRepository chatMessageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.lms.modules.ai.repository.AiChatThreadRepository chatThreadRepository;

    @Autowired
    private com.lms.modules.course.repository.CourseRepository courseRepository;

    @Autowired
    private List<AiProvider> aiProviders;

    @Value("${ai.provider:mock}")
    private String defaultProvider;

    /**
     * Resolves the AI provider by ID.
     * Uses the configured default (ai.provider in application.properties) if not specified.
     * Falls back to 'mock' if the requested provider is not found.
     */
    private AiProvider getProvider(String requestedProviderId) {
        if (aiProviders == null || aiProviders.isEmpty()) {
            throw new RuntimeException("No AI providers configured in the system.");
        }

        // Use requested provider, or fall back to configured default
        String providerId = (requestedProviderId != null && !requestedProviderId.isBlank())
                ? requestedProviderId
                : defaultProvider;

        return aiProviders.stream()
                .filter(p -> p.getProviderId().equalsIgnoreCase(providerId))
                .findFirst()
                .orElseGet(() -> {
                    // Fall back to mock if requested provider not available
                    return aiProviders.stream()
                            .filter(p -> p.getProviderId().equals("mock"))
                            .findFirst()
                            .orElse(aiProviders.get(0));
                });
    }

    @Transactional
    public AiChatMessageEntity sendMessage(String userEmail, String prompt, String providerId, Long threadId, Long courseId) {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new RuntimeException("Message cannot be empty");
        }
        if (prompt.length() > 2000) {
            throw new RuntimeException("Message is too long. Please keep it under 2000 characters.");
        }

        UserEntity user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Resolve or create thread
        com.lms.modules.ai.entity.AiChatThreadEntity thread = null;
        if (threadId != null) {
            thread = chatThreadRepository.findById(threadId)
                    .orElseThrow(() -> new RuntimeException("Chat thread not found"));
            // Security check
            if (!thread.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("Access denied: You do not own this chat thread");
            }
        } else if (courseId != null) {
            // Auto-create thread if not provided
            String title = prompt.trim();
            if (title.length() > 40) {
                title = title.substring(0, 37) + "...";
            }
            thread = createThreadInternal(user, courseId, title);
        } else {
            throw new RuntimeException("Either threadId or courseId must be provided");
        }

        // Fetch thread history
        List<AiChatMessageEntity> history = java.util.Collections.emptyList();
        if (thread != null && thread.getId() != null) {
            history = chatMessageRepository.findByThreadIdOrderByCreatedAtAsc(thread.getId());
        }

        AiProvider provider = getProvider(providerId);
        String response = provider.generateResponse(prompt.trim(), history);

        AiChatMessageEntity message = new AiChatMessageEntity();
        message.setUser(user);
        message.setThread(thread);
        message.setMessage(prompt.trim());
        message.setResponse(response);

        return chatMessageRepository.save(message);
    }

    @Transactional
    public com.lms.modules.ai.entity.AiChatThreadEntity createThread(String email, Long courseId, String title) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return createThreadInternal(user, courseId, title);
    }

    private com.lms.modules.ai.entity.AiChatThreadEntity createThreadInternal(UserEntity user, Long courseId, String title) {
        com.lms.modules.course.entity.CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        com.lms.modules.ai.entity.AiChatThreadEntity thread = new com.lms.modules.ai.entity.AiChatThreadEntity();
        thread.setUser(user);
        thread.setCourse(course);
        thread.setTitle(title != null && !title.isBlank() ? title : "New Chat Session");

        return chatThreadRepository.save(thread);
    }

    @Transactional(readOnly = true)
    public List<com.lms.modules.ai.entity.AiChatThreadEntity> getThreads(String email, Long courseId) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return chatThreadRepository.findByUserIdAndCourseIdOrderByCreatedAtDesc(user.getId(), courseId);
    }

    @Transactional
    public void deleteThread(String email, Long threadId) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        com.lms.modules.ai.entity.AiChatThreadEntity thread = chatThreadRepository.findById(threadId)
                .orElseThrow(() -> new RuntimeException("Chat thread not found"));

        if (!thread.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied: You do not own this chat thread");
        }

        // Delete all messages in the thread first
        List<AiChatMessageEntity> messages = chatMessageRepository.findByThreadIdOrderByCreatedAtAsc(threadId);
        chatMessageRepository.deleteAll(messages);

        chatThreadRepository.delete(thread);
    }

    @Transactional(readOnly = true)
    public List<AiChatMessageEntity> getChatMessagesForThread(String email, Long threadId) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        com.lms.modules.ai.entity.AiChatThreadEntity thread = chatThreadRepository.findById(threadId)
                .orElseThrow(() -> new RuntimeException("Chat thread not found"));

        if (!thread.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied: You do not own this chat thread");
        }

        return chatMessageRepository.findByThreadIdOrderByCreatedAtAsc(threadId);
    }

    @Transactional(readOnly = true)
    public List<AiChatMessageEntity> getChatHistory(String userEmail) {
        UserEntity user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return chatMessageRepository.findByUserIdOrderByCreatedAtAsc(user.getId());
    }

    /**
     * Returns the list of available AI provider IDs.
     * Used by frontend to show which provider is active.
     */
    public List<String> getAvailableProviders() {
        return aiProviders.stream()
                .map(AiProvider::getProviderId)
                .toList();
    }
}
