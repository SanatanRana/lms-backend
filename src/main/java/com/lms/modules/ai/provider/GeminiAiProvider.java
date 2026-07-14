package com.lms.modules.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.common.strategy.AiProvider;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * Google Gemini AI Provider.
 *
 * ACTIVATION: This provider is registered when gemini.api.key is set in
 * application.properties (or via GEMINI_API_KEY environment variable).
 *
 * The AiChatService will automatically prefer this over MockAiProvider
 * when provider="gemini" is passed in the API request.
 */
@Component
@ConditionalOnProperty(name = "gemini.api.key", matchIfMissing = false)
public class GeminiAiProvider implements AiProvider {

    private static final Logger logger = LoggerFactory.getLogger(GeminiAiProvider.class);

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model:gemini-1.5-flash}")
    private String model;

    private OkHttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    // System instruction to make the AI behave as an educational assistant
    private static final String SYSTEM_INSTRUCTION =
            "You are an expert AI tutor for LearnGen, an online learning platform. " +
            "Respond like a knowledgeable friend, not a textbook. Be natural, conversational, and to the point. " +
            "\n\nRESPONSE LENGTH RULES (follow strictly):" +
            "\n- Simple question (what is X?) → 3-5 sentences max. Give a clear, crisp answer." +
            "\n- Concept explanation → short paragraph + 1 example if helpful." +
            "\n- If student says 'explain in detail', 'explain thoroughly', 'give full explanation' → then give structured detailed answer." +
            "\n- If student asks for code → give clean code example with brief inline comments only." +
            "\n- Interview prep questions → give concise bullet points, not essays." +
            "\n\nFORMATTING:" +
            "\n- Use **bold** only for the most important terms, not every word." +
            "\n- Use numbered lists only when explaining steps. Avoid over-structuring." +
            "\n- Use code blocks for actual code." +
            "\n- Do NOT use headers (## or ###) for short answers." +
            "\n\nPERSONALITY: Be encouraging, friendly, and smart. If a concept is tricky, say so. " +
            "If you don't know something, admit it. Don't pad answers with unnecessary fluff. " +
            "If a question is off-topic, briefly redirect to learning.";

    @PostConstruct
    public void init() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        logger.info("[GeminiAiProvider] Initialized with model: {}", model);
    }

    @Override
    public String getProviderId() {
        return "gemini";
    }

    @Override
    public String generateResponse(String prompt) {
        return generateResponse(prompt, java.util.Collections.emptyList());
    }

    @Override
    public String generateResponse(String prompt, java.util.List<com.lms.modules.ai.entity.AiChatMessageEntity> history) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new RuntimeException("Gemini API key is not configured. Please set GEMINI_API_KEY.");
        }

        try {
            // Build contents array with history
            java.util.List<java.util.Map<String, Object>> contents = new java.util.ArrayList<>();

            // Add history context (limit to last 10 messages to keep under token limits)
            int historySize = history != null ? history.size() : 0;
            int startIdx = Math.max(0, historySize - 10);
            for (int i = startIdx; i < historySize; i++) {
                com.lms.modules.ai.entity.AiChatMessageEntity msg = history.get(i);
                // Add user message
                contents.add(java.util.Map.of(
                    "role", "user",
                    "parts", java.util.List.of(java.util.Map.of("text", msg.getMessage()))
                ));
                // Add model response
                if (msg.getResponse() != null && !msg.getResponse().isBlank()) {
                    contents.add(java.util.Map.of(
                        "role", "model",
                        "parts", java.util.List.of(java.util.Map.of("text", msg.getResponse()))
                    ));
                }
            }

            // Add current user prompt
            contents.add(java.util.Map.of(
                "role", "user",
                "parts", java.util.List.of(java.util.Map.of("text", prompt))
            ));

            String requestBody = objectMapper.writeValueAsString(
                java.util.Map.of(
                    "system_instruction", java.util.Map.of(
                        "parts", java.util.List.of(java.util.Map.of("text", SYSTEM_INSTRUCTION))
                    ),
                    "contents", contents,
                    "generationConfig", java.util.Map.of(
                        "maxOutputTokens", 2000,
                        "temperature", 0.7,
                        "topP", 0.9
                    )
                )
            );

            String url = String.format(GEMINI_API_URL, model, apiKey);
            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    logger.error("[GeminiAiProvider] API error {}: {}", response.code(), errorBody);
                    throw new RuntimeException("Gemini API error: " + response.code() + ". Please try again.");
                }

                String responseBody = response.body().string();
                JsonNode root = objectMapper.readTree(responseBody);

                // Parse Gemini response: candidates[0].content.parts[0].text
                JsonNode candidates = root.path("candidates");
                if (candidates.isEmpty() || candidates.get(0) == null) {
                    throw new RuntimeException("No response generated. Please rephrase your question.");
                }

                String text = candidates.get(0)
                        .path("content")
                        .path("parts")
                        .get(0)
                        .path("text")
                        .asText();

                if (text.isBlank()) {
                    throw new RuntimeException("Empty response from AI. Please try again.");
                }

                return text.trim();
            }
        } catch (RuntimeException e) {
            throw e; // Re-throw known errors
        } catch (Exception e) {
            logger.error("[GeminiAiProvider] Unexpected error: {}", e.getMessage(), e);
            throw new RuntimeException("AI service temporarily unavailable. Please try again in a moment.");
        }
    }
}
