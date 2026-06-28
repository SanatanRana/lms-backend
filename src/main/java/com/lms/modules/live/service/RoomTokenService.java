package com.lms.modules.live.service;

import org.springframework.stereotype.Service;
import java.util.UUID;

/**
 * Generates and validates secure room tokens for shareable live class join links.
 * Token format: UUID — e.g., "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
 * Join URL: /live/join/{roomToken}
 */
@Service
public class RoomTokenService {

    /**
     * Generate a unique, URL-safe room token.
     */
    public String generateToken() {
        return UUID.randomUUID().toString();
    }
}
