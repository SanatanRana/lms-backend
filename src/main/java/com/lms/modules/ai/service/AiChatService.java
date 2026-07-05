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
    public AiChatMessageEntity sendMessage(String userEmail, String prompt, String providerId) {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new RuntimeException("Message cannot be empty");
        }
        if (prompt.length() > 2000) {
            throw new RuntimeException("Message is too long. Please keep it under 2000 characters.");
        }

        UserEntity user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        AiProvider provider = getProvider(providerId);
        String response = provider.generateResponse(prompt.trim());

        AiChatMessageEntity message = new AiChatMessageEntity();
        message.setUser(user);
        message.setMessage(prompt.trim());
        message.setResponse(response);

        return chatMessageRepository.save(message);
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
