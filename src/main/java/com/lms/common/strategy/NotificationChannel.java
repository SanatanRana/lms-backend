package com.lms.common.strategy;

/**
 * SYSTEM DESIGN: Strategy Pattern for Notification Channels
 * ──────────────────────────────────────────────────────────
 * To add a new notification channel (e.g., WhatsApp, Push):
 *   1. Create a new class implementing this interface.
 *   2. Annotate it with @Component.
 *   3. Done – NotificationService iterates all channels automatically.
 */
public interface NotificationChannel {

    /** Channel identifier (e.g., "email", "sms", "database") */
    String getChannelId();

    /** Send a notification through this channel */
    void send(Long userId, String title, String message);

    /** Whether this channel is enabled (can be toggled via config) */
    default boolean isEnabled() { return true; }
}
