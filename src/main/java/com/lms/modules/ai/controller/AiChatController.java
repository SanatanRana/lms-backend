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
        AiChatMessageEntity message = aiChatService.sendMessage(email, request.getMessage(), resolvedProvider, request.getThreadId(), request.getCourseId());
        return ResponseEntity.ok(ApiResponse.success("AI response generated", message));
    }

    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<AiChatMessageEntity>>> getChatHistory(
            @RequestParam(required = false) Long threadId,
            Authentication authentication) {
        String email = authentication.getName();
        List<AiChatMessageEntity> history;
        if (threadId != null) {
            history = aiChatService.getChatMessagesForThread(email, threadId);
        } else {
            history = aiChatService.getChatHistory(email);
        }
        return ResponseEntity.ok(ApiResponse.success("Chat history retrieved", history));
    }

    @PostMapping("/threads")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<com.lms.modules.ai.entity.AiChatThreadEntity>> createThread(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        String email = authentication.getName();
        Long courseId = Long.valueOf(body.get("courseId").toString());
        String title = (String) body.get("title");
        com.lms.modules.ai.entity.AiChatThreadEntity thread = aiChatService.createThread(email, courseId, title);
        return ResponseEntity.ok(ApiResponse.success("Chat thread created", thread));
    }

    @GetMapping("/threads")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<com.lms.modules.ai.entity.AiChatThreadEntity>>> getThreads(
            @RequestParam Long courseId,
            Authentication authentication) {
        String email = authentication.getName();
        List<com.lms.modules.ai.entity.AiChatThreadEntity> threads = aiChatService.getThreads(email, courseId);
        return ResponseEntity.ok(ApiResponse.success("Chat threads retrieved", threads));
    }

    @DeleteMapping("/threads/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteThread(
            @PathVariable Long id,
            Authentication authentication) {
        String email = authentication.getName();
        aiChatService.deleteThread(email, id);
        return ResponseEntity.ok(ApiResponse.success("Chat thread deleted"));
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
