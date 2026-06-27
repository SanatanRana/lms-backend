package com.lms.modules.ai.service;

import com.lms.common.strategy.AiProvider;
import com.lms.modules.ai.entity.AiChatMessageEntity;
import com.lms.modules.ai.repository.AiChatMessageRepository;
import com.lms.modules.user.entity.UserEntity;
import com.lms.modules.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    /**
     * Resolves the AI provider by ID. Falls back to the first available if not found.
     */
    private AiProvider getProvider(String providerId) {
        if (aiProviders == null || aiProviders.isEmpty()) {
            throw new RuntimeException("No AI providers configured in the system.");
        }
        return aiProviders.stream()
                .filter(p -> p.getProviderId().equalsIgnoreCase(providerId))
                .findFirst()
                .orElse(aiProviders.get(0));
    }

    @Transactional
    public AiChatMessageEntity sendMessage(String userEmail, String prompt, String providerId) {
        UserEntity user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        AiProvider provider = getProvider(providerId != null ? providerId : "mock");
        String response = provider.generateResponse(prompt);

        AiChatMessageEntity message = new AiChatMessageEntity();
        message.setUser(user);
        message.setMessage(prompt);
        message.setResponse(response);

        return chatMessageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public List<AiChatMessageEntity> getChatHistory(String userEmail) {
        UserEntity user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return chatMessageRepository.findByUserIdOrderByCreatedAtAsc(user.getId());
    }
}
