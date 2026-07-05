package com.lms.modules.ai.controller;

import com.lms.common.dto.ApiResponse;
import com.lms.modules.ai.dto.AiChatRequest;
import com.lms.modules.ai.entity.AiChatMessageEntity;
import com.lms.modules.ai.service.AiChatService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    @Autowired
    private AiChatService aiChatService;

    @Value("${ai.provider:mock}")
    private String defaultProvider;

    /**
     * Send a message to the AI assistant.
     * Uses configured ai.provider by default; can be overridden with ?provider=gemini|mock
     */
    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AiChatMessageEntity>> chat(
            @Valid @RequestBody AiChatRequest request,
            @RequestParam(required = false) String provider,
            Authentication authentication) {
        String email = authentication.getName();
        // Use explicitly requested provider, or fall back to configured default
        String resolvedProvider = (provider != null && !provider.isBlank()) ? provider : defaultProvider;
        AiChatMessageEntity message = aiChatService.sendMessage(email, request.getMessage(), resolvedProvider);
        return ResponseEntity.ok(ApiResponse.success("AI response generated", message));
    }

    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<AiChatMessageEntity>>> getChatHistory(Authentication authentication) {
        String email = authentication.getName();
        List<AiChatMessageEntity> history = aiChatService.getChatHistory(email);
        return ResponseEntity.ok(ApiResponse.success("Chat history retrieved", history));
    }

    /**
     * Returns available AI providers — frontend uses this to show which is active.
     */
    @GetMapping("/providers")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProviders() {
        List<String> available = aiChatService.getAvailableProviders();
        return ResponseEntity.ok(ApiResponse.success("Providers retrieved", Map.of(
            "available", available,
            "default", defaultProvider,
            "isGeminiActive", available.contains("gemini")
        )));
    }
}
