package com.lms.modules.ai.controller;

import com.lms.common.dto.ApiResponse;
import com.lms.modules.ai.dto.AiChatRequest;
import com.lms.modules.ai.entity.AiChatMessageEntity;
import com.lms.modules.ai.service.AiChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    @Autowired
    private AiChatService aiChatService;

    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AiChatMessageEntity>> chat(
            @RequestBody AiChatRequest request,
            @RequestParam(required = false, defaultValue = "mock") String provider,
            Authentication authentication) {
        String email = authentication.getName();
        AiChatMessageEntity message = aiChatService.sendMessage(email, request.getMessage(), provider);
        return ResponseEntity.ok(ApiResponse.success("AI response generated", message));
    }

    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<AiChatMessageEntity>>> getChatHistory(Authentication authentication) {
        String email = authentication.getName();
        List<AiChatMessageEntity> history = aiChatService.getChatHistory(email);
        return ResponseEntity.ok(ApiResponse.success("Chat history retrieved", history));
    }
}
