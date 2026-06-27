package com.lms.modules.ai.provider;

import com.lms.common.strategy.AiProvider;
import org.springframework.stereotype.Component;

@Component
public class MockAiProvider implements AiProvider {

    @Override
    public String getProviderId() {
        return "mock";
    }

    @Override
    public String generateResponse(String prompt) {
        String lower = prompt.toLowerCase();
        if (lower.contains("java") || lower.contains("spring")) {
            return "Java & Spring Boot Tip: Always use constructor injection for mandatory dependencies and setter/field injection for optional ones. This ensures your components are easier to unit test!";
        } else if (lower.contains("html") || lower.contains("css") || lower.contains("react")) {
            return "Frontend Tip: Use React functional components with hooks and clean Tailwind/CSS layouts. Keep your components small and focused to improve reusability!";
        } else if (lower.contains("sql") || lower.contains("database")) {
            return "Database Tip: Use indexing on foreign keys and commonly searched columns. Keep transaction scopes short to prevent deadlocks and maintain high concurrency!";
        } else {
            return "Educational Assistant: That is a great question! In a production system, you would analyze the complexity, use divide-and-conquer strategy, and write clean, modular code to solve it. Let me know if you need more details!";
        }
    }
}
