package com.lms.common.strategy;

/**
 * SYSTEM DESIGN: Strategy Pattern for AI Providers
 * ────────────────────────────────────────────────
 * Allows switching between different AI models (Gemini, Claude, GPT, or Mock)
 * without modifying the core AI Chat Service.
 */
public interface AiProvider {

    /** Unique identifier for the AI provider (e.g., "gemini", "mock") */
    String getProviderId();

    /** Generates a response based on the input prompt */
    String generateResponse(String prompt);

    /** Generates a response based on the input prompt and thread history */
    default String generateResponse(String prompt, java.util.List<com.lms.modules.ai.entity.AiChatMessageEntity> history) {
        return generateResponse(prompt);
    }
}
