package com.lms.modules.live.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory room management for live classroom sessions.
 * Tracks active rooms and their participants for WebSocket signaling.
 */
@Service
public class RoomService {

    /** Participant info stored per-room */
    public record Participant(
        String sessionWsId,
        String name,
        String role,       // TEACHER, STUDENT, GUEST
        Long userId,       // null for guests
        boolean audioMuted,
        boolean videoMuted,
        long joinedAt
    ) {}

    /** Room state */
    public record Room(
        Long sessionId,
        String teacherWsId,
        Set<String> participantIds,
        Map<String, Participant> participants,
        List<ChatMessage> chatHistory,
        long createdAt
    ) {}

    /** Chat message record */
    public record ChatMessage(
        String senderName,
        String senderRole,
        String message,
        long timestamp
    ) {}

    private final ConcurrentHashMap<Long, Room> activeRooms = new ConcurrentHashMap<>();

    /**
     * Create a new room when a teacher starts a session.
     */
    public Room createRoom(Long sessionId) {
        Room room = new Room(
            sessionId,
            null,
            ConcurrentHashMap.newKeySet(),
            new ConcurrentHashMap<>(),
            Collections.synchronizedList(new ArrayList<>()),
            System.currentTimeMillis()
        );
        activeRooms.put(sessionId, room);
        return room;
    }

    /**
     * Add a participant to a room.
     */
    public Participant joinRoom(Long sessionId, String wsSessionId, String name, String role, Long userId) {
        Room room = activeRooms.get(sessionId);
        if (room == null) {
            throw new RuntimeException("Room not found for session: " + sessionId);
        }

        Participant participant = new Participant(
            wsSessionId, name, role, userId, true, true, System.currentTimeMillis()
        );
        room.participantIds().add(wsSessionId);
        room.participants().put(wsSessionId, participant);

        if ("TEACHER".equals(role)) {
            // Update teacher reference — use reflection or rebuild since records are immutable
            activeRooms.put(sessionId, new Room(
                room.sessionId(), wsSessionId, room.participantIds(),
                room.participants(), room.chatHistory(), room.createdAt()
            ));
        }
        return participant;
    }

    /**
     * Remove a participant from a room.
     */
    public void leaveRoom(Long sessionId, String wsSessionId) {
        Room room = activeRooms.get(sessionId);
        if (room != null) {
            room.participantIds().remove(wsSessionId);
            room.participants().remove(wsSessionId);
        }
    }

    /**
     * Update mute status of a participant's audio or video track.
     */
    public void updateMuteState(Long sessionId, String wsSessionId, String mediaType, boolean muted) {
        Room room = activeRooms.get(sessionId);
        if (room != null) {
            Participant p = room.participants().get(wsSessionId);
            if (p != null) {
                Participant updated = new Participant(
                    p.sessionWsId(),
                    p.name(),
                    p.role(),
                    p.userId(),
                    "audio".equals(mediaType) ? muted : p.audioMuted(),
                    "video".equals(mediaType) ? muted : p.videoMuted(),
                    p.joinedAt()
                );
                room.participants().put(wsSessionId, updated);
            }
        }
    }

    /**
     * Destroy a room when the session ends.
     */
    public void destroyRoom(Long sessionId) {
        activeRooms.remove(sessionId);
    }

    /**
     * Get room by session ID.
     */
    public Room getRoom(Long sessionId) {
        return activeRooms.get(sessionId);
    }

    /**
     * Check if a room exists and is active.
     */
    public boolean isRoomActive(Long sessionId) {
        return activeRooms.containsKey(sessionId);
    }

    /**
     * Get all participants in a room.
     */
    public Collection<Participant> getParticipants(Long sessionId) {
        Room room = activeRooms.get(sessionId);
        if (room == null) return Collections.emptyList();
        return room.participants().values();
    }

    /**
     * Get participant count for a room.
     */
    public int getParticipantCount(Long sessionId) {
        Room room = activeRooms.get(sessionId);
        return room != null ? room.participants().size() : 0;
    }

    /**
     * Add a chat message to room history.
     */
    public void addChatMessage(Long sessionId, String senderName, String senderRole, String message) {
        Room room = activeRooms.get(sessionId);
        if (room != null) {
            room.chatHistory().add(new ChatMessage(senderName, senderRole, message, System.currentTimeMillis()));
            // Keep only last 500 messages per room
            if (room.chatHistory().size() > 500) {
                room.chatHistory().remove(0);
            }
        }
    }

    /**
     * Get chat history for a room.
     */
    public List<ChatMessage> getChatHistory(Long sessionId) {
        Room room = activeRooms.get(sessionId);
        return room != null ? new ArrayList<>(room.chatHistory()) : Collections.emptyList();
    }

    /**
     * Get total number of active rooms (for monitoring).
     */
    public int getActiveRoomCount() {
        return activeRooms.size();
    }
}
