package com.lms.modules.live.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lms.modules.live.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.lms.modules.live.repository.LiveSessionRepository;
import com.lms.modules.course.repository.EnrollmentRepository;
import com.lms.modules.live.entity.LiveSessionEntity;
import org.springframework.context.event.EventListener;
import com.lms.common.event.DomainEvents.LiveSessionEndedEvent;

/**
 * Core WebSocket handler for live classroom signaling.
 * 
 * Handles WebRTC negotiation (SDP offers/answers, ICE candidates),
 * room management, real-time chat, and participant state synchronization.
 * 
 * Message Protocol (JSON):
 * {
 * "type":
 * "JOIN_ROOM|LEAVE_ROOM|OFFER|ANSWER|ICE_CANDIDATE|CHAT_MESSAGE|MUTE_TOGGLE|PARTICIPANT_LIST",
 * "roomId": "session-id",
 * "payload": { ... }
 * }
 */
@Component
public class SignalingWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RoomService roomService;

    @Autowired
    private LiveSessionRepository liveSessionRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    // Map WebSocket session ID → room ID (sessionId) for cleanup on disconnect
    private final ConcurrentHashMap<String, Long> sessionToRoom = new ConcurrentHashMap<>();
    // Map WebSocket session ID → WebSocketSession for message routing
    private final ConcurrentHashMap<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        activeSessions.put(session.getId(), session);
        System.out.println("[SignalingWS] Connection established: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode json = objectMapper.readTree(message.getPayload());
        String type = json.has("type") ? json.get("type").asText() : "";
        Long roomId = json.has("roomId") ? json.get("roomId").asLong() : null;
        JsonNode payload = json.get("payload");

        switch (type) {
            case "JOIN_ROOM" -> handleJoinRoom(session, roomId, payload);
            case "LEAVE_ROOM" -> handleLeaveRoom(session, roomId);
            case "OFFER" -> handleOffer(session, roomId, payload);
            case "ANSWER" -> handleAnswer(session, roomId, payload);
            case "ICE_CANDIDATE" -> handleIceCandidate(session, roomId, payload);
            case "CHAT_MESSAGE" -> handleChatMessage(session, roomId, payload);
            case "MUTE_TOGGLE" -> handleMuteToggle(session, roomId, payload);
            case "SCREEN_SHARE" -> handleScreenShare(session, roomId, payload);
            case "FORCE_MUTE" -> handleForceMute(session, roomId, payload);
            case "TOGGLE_CHAT_ACCESS" -> handleToggleChatAccess(session, roomId, payload);
            default -> sendError(session, "Unknown message type: " + type);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String wsId = session.getId();
        Long roomId = sessionToRoom.remove(wsId);
        activeSessions.remove(wsId);

        if (roomId != null) {
            roomService.leaveRoom(roomId, wsId);
            // Notify remaining participants
            broadcastToRoom(roomId, createMessage("PARTICIPANT_LEFT", roomId, Map.of(
                    "sessionId", wsId,
                    "participants", getParticipantList(roomId))), wsId);
        }
        System.out.println("[SignalingWS] Connection closed: " + wsId);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        System.err.println("[SignalingWS] Transport error for " + session.getId() + ": " + exception.getMessage());
        try {
            afterConnectionClosed(session, CloseStatus.SERVER_ERROR);
        } catch (Exception e) {
            System.err.println("[SignalingWS] Error during cleanup: " + e.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Message Handlers
    // ──────────────────────────────────────────────────────────────

    private void handleJoinRoom(WebSocketSession session, Long roomId, JsonNode payload) {
        if (roomId == null || payload == null) {
            sendError(session, "roomId and payload required for JOIN_ROOM");
            return;
        }

        String name = payload.has("name") ? payload.get("name").asText() : "Anonymous";
        String role = payload.has("role") ? payload.get("role").asText() : "GUEST";
        Long userId = payload.has("userId") && !payload.get("userId").isNull()
                ? payload.get("userId").asLong()
                : null;

        // Verify session exists and check authorization policies
        LiveSessionEntity liveSession = liveSessionRepository.findById(roomId).orElse(null);
        if (liveSession == null) {
            sendError(session, "Access denied: Live class not found.");
            try {
                session.close(CloseStatus.BAD_DATA);
            } catch (Exception e) {
            }
            return;
        }

        if ("GUEST".equals(role)) {
            if (!liveSession.getGuestAccessEnabled()) {
                sendError(session, "Access denied: Guest access is disabled for this classroom.");
                try {
                    session.close(CloseStatus.POLICY_VIOLATION);
                } catch (Exception e) {
                }
                return;
            }
        } else if ("STUDENT".equals(role)) {
            if (userId != null) {
                boolean isEnrolled = enrollmentRepository.existsByStudentIdAndCourseId(userId,
                        liveSession.getCourse().getId());
                if (!isEnrolled) {
                    sendError(session, "Access denied: You are not enrolled in this course.");
                    try {
                        session.close(CloseStatus.POLICY_VIOLATION);
                    } catch (Exception e) {
                    }
                    return;
                }
            } else {
                sendError(session, "Access denied: User identification failed.");
                try {
                    session.close(CloseStatus.POLICY_VIOLATION);
                } catch (Exception e) {
                }
                return;
            }
        }

        boolean audioMuted = payload.has("audioMuted") ? payload.get("audioMuted").asBoolean() : true;
        boolean videoMuted = payload.has("videoMuted") ? payload.get("videoMuted").asBoolean() : true;

        // Create room if it doesn't exist (teacher starting)
        if (!roomService.isRoomActive(roomId)) {
            roomService.createRoom(roomId);
        }

        RoomService.Participant participant = roomService.joinRoom(
                roomId, session.getId(), name, role, userId, audioMuted, videoMuted);
        sessionToRoom.put(session.getId(), roomId);

        // Send current room state to the joining participant
        sendMessage(session, createMessage("ROOM_JOINED", roomId, Map.of(
                "yourSessionId", session.getId(),
                "participants", getParticipantList(roomId),
                "chatHistory", roomService.getChatHistory(roomId))));

        // Notify all other participants about the new participant
        broadcastToRoom(roomId, createMessage("PARTICIPANT_JOINED", roomId, Map.of(
                "sessionId", session.getId(),
                "name", name,
                "role", role,
                "participants", getParticipantList(roomId))), session.getId());
    }

    private void handleLeaveRoom(WebSocketSession session, Long roomId) {
        if (roomId == null)
            return;
        String wsId = session.getId();
        roomService.leaveRoom(roomId, wsId);
        sessionToRoom.remove(wsId);

        broadcastToRoom(roomId, createMessage("PARTICIPANT_LEFT", roomId, Map.of(
                "sessionId", wsId,
                "participants", getParticipantList(roomId))), wsId);
    }

    private void handleOffer(WebSocketSession session, Long roomId, JsonNode payload) {
        if (roomId == null || payload == null)
            return;
        String targetId = payload.has("targetId") ? payload.get("targetId").asText() : null;

        if (targetId != null) {
            // Send offer to specific peer
            WebSocketSession target = activeSessions.get(targetId);
            if (target != null && target.isOpen()) {
                sendMessage(target, createMessage("OFFER", roomId, Map.of(
                        "sdp", payload.get("sdp").asText(),
                        "senderId", session.getId())));
            }
        } else {
            // Broadcast offer to all in room (teacher broadcasts to all students)
            broadcastToRoom(roomId, createMessage("OFFER", roomId, Map.of(
                    "sdp", payload.get("sdp").asText(),
                    "senderId", session.getId())), session.getId());
        }
    }

    private void handleAnswer(WebSocketSession session, Long roomId, JsonNode payload) {
        if (roomId == null || payload == null)
            return;
        String targetId = payload.has("targetId") ? payload.get("targetId").asText() : null;

        if (targetId != null) {
            WebSocketSession target = activeSessions.get(targetId);
            if (target != null && target.isOpen()) {
                sendMessage(target, createMessage("ANSWER", roomId, Map.of(
                        "sdp", payload.get("sdp").asText(),
                        "senderId", session.getId())));
            }
        }
    }

    private void handleIceCandidate(WebSocketSession session, Long roomId, JsonNode payload) {
        if (roomId == null || payload == null)
            return;
        String targetId = payload.has("targetId") ? payload.get("targetId").asText() : null;

        ObjectNode candidatePayload = objectMapper.createObjectNode();
        candidatePayload.put("senderId", session.getId());
        if (payload.has("candidate"))
            candidatePayload.set("candidate", payload.get("candidate"));
        if (payload.has("sdpMid"))
            candidatePayload.put("sdpMid", payload.get("sdpMid").asText());
        if (payload.has("sdpMLineIndex"))
            candidatePayload.put("sdpMLineIndex", payload.get("sdpMLineIndex").asInt());

        if (targetId != null) {
            WebSocketSession target = activeSessions.get(targetId);
            if (target != null && target.isOpen()) {
                sendRawMessage(target, createRawMessage("ICE_CANDIDATE", roomId, candidatePayload));
            }
        } else {
            broadcastRawToRoom(roomId, createRawMessage("ICE_CANDIDATE", roomId, candidatePayload), session.getId());
        }
    }

    private void handleChatMessage(WebSocketSession session, Long roomId, JsonNode payload) {
        if (roomId == null || payload == null)
            return;
        String message = payload.has("message") ? payload.get("message").asText() : "";
        if (message.isBlank())
            return;
        RoomService.Participant sender = roomService.getParticipant(roomId, session.getId());
        if (sender == null) return;
        
        if (sender.chatDisabled()) {
            sendError(session, "Your chat access has been disabled by the instructor.");
            return;
        }

        roomService.addChatMessage(roomId, sender.name(), sender.role(), message);

        // Broadcast chat to all in room (including sender for confirmation)
        broadcastToRoom(roomId, createMessage("CHAT_MESSAGE", roomId, Map.of(
            "senderName", sender.name(),
            "senderRole", sender.role(),
            "message", message,
            "timestamp", System.currentTimeMillis(),
            "senderId", session.getId()
        )), null);
    }

    private void handleMuteToggle(WebSocketSession session, Long roomId, JsonNode payload) {
        if (roomId == null || payload == null)
            return;
        String mediaType = payload.has("mediaType") ? payload.get("mediaType").asText() : "";
        boolean muted = payload.has("muted") && payload.get("muted").asBoolean();

        // Update active in-memory room state
        roomService.updateMuteState(roomId, session.getId(), mediaType, muted);

        // Broadcast to all to update UI
        broadcastToRoom(roomId, createMessage("MUTE_TOGGLE", roomId, Map.of(
                "sessionId", session.getId(),
                "mediaType", mediaType,
                "muted", muted)), null);
    }

    private void handleForceMute(WebSocketSession session, Long roomId, JsonNode payload) {
        if (roomId == null || payload == null)
            return;
        RoomService.Participant sender = roomService.getParticipant(roomId, session.getId());
        if (sender == null || !"TEACHER".equals(sender.role()))
            return; // Only teachers can force mute

        String targetId = payload.has("targetId") ? payload.get("targetId").asText() : null;
        String mediaType = payload.has("mediaType") ? payload.get("mediaType").asText() : null;
        if (targetId == null || mediaType == null)
            return;

        WebSocketSession target = activeSessions.get(targetId);
        if (target != null && target.isOpen()) {
            sendMessage(target, createMessage("FORCE_MUTE", roomId, Map.of(
                    "mediaType", mediaType)));
        }
    }

    private void handleToggleChatAccess(WebSocketSession session, Long roomId, JsonNode payload) {
        if (roomId == null || payload == null)
            return;
        RoomService.Participant sender = roomService.getParticipant(roomId, session.getId());
        if (sender == null || !"TEACHER".equals(sender.role()))
            return; // Only teachers can toggle chat

        String targetId = payload.has("targetId") ? payload.get("targetId").asText() : null;
        boolean disabled = payload.has("disabled") && payload.get("disabled").asBoolean();
        if (targetId == null)
            return;

        roomService.setChatDisabled(roomId, targetId, disabled);

        broadcastToRoom(roomId, createMessage("CHAT_ACCESS_CHANGED", roomId, Map.of(
                "targetId", targetId,
                "disabled", disabled)), null);
    }

    private void handleScreenShare(WebSocketSession session, Long roomId, JsonNode payload) {
        if (roomId == null || payload == null)
            return;
        boolean sharing = payload.has("sharing") && payload.get("sharing").asBoolean();

        broadcastToRoom(roomId, createMessage("SCREEN_SHARE", roomId, Map.of(
                "sessionId", session.getId(),
                "sharing", sharing)), null);
    }

    // ──────────────────────────────────────────────────────────────
    // Utility Methods
    // ──────────────────────────────────────────────────────────────

    private List<Map<String, Object>> getParticipantList(Long roomId) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (RoomService.Participant p : roomService.getParticipants(roomId)) {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("sessionId", p.sessionWsId());
            info.put("name", p.name());
            info.put("role", p.role());
            info.put("audioMuted", p.audioMuted());
            info.put("videoMuted", p.videoMuted());
            info.put("chatDisabled", p.chatDisabled());
            list.add(info);
        }
        return list;
    }

    private String createMessage(String type, Long roomId, Map<String, Object> payload) {
        try {
            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("type", type);
            msg.put("roomId", roomId);
            msg.put("payload", payload);
            return objectMapper.writeValueAsString(msg);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String createRawMessage(String type, Long roomId, JsonNode payload) {
        try {
            ObjectNode msg = objectMapper.createObjectNode();
            msg.put("type", type);
            msg.put("roomId", roomId);
            msg.set("payload", payload);
            return objectMapper.writeValueAsString(msg);
        } catch (Exception e) {
            return "{}";
        }
    }

    private void sendMessage(WebSocketSession session, String message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        } catch (IOException e) {
            System.err.println("[SignalingWS] Error sending message: " + e.getMessage());
        }
    }

    private void sendRawMessage(WebSocketSession session, String message) {
        sendMessage(session, message);
    }

    private void sendError(WebSocketSession session, String error) {
        sendMessage(session, createMessage("ERROR", null, Map.of("message", error)));
    }

    /**
     * Broadcast a message to all participants in a room, optionally excluding one.
     */
    private void broadcastToRoom(Long roomId, String message, String excludeWsId) {
        RoomService.Room room = roomService.getRoom(roomId);
        if (room == null)
            return;

        for (String wsId : room.participantIds()) {
            if (excludeWsId != null && excludeWsId.equals(wsId))
                continue;
            WebSocketSession target = activeSessions.get(wsId);
            if (target != null && target.isOpen()) {
                sendMessage(target, message);
            }
        }
    }

    private void broadcastRawToRoom(Long roomId, String message, String excludeWsId) {
        broadcastToRoom(roomId, message, excludeWsId);
    }

    @EventListener
    public void handleLiveSessionEnded(LiveSessionEndedEvent event) {
        Long roomId = event.sessionId();
        System.out.println("[SignalingWS] Handling LiveSessionEndedEvent for room: " + roomId);

        // 1. Broadcast "ROOM_ENDED" message to all participants in this room
        String endMessage = createMessage("ROOM_ENDED", roomId, Map.of(
                "message", "The live class has been ended by the instructor."));
        broadcastToRoom(roomId, endMessage, null);

        // 2. Close all WebSocket sessions of participants in this room
        RoomService.Room room = roomService.getRoom(roomId);
        if (room != null) {
            List<String> wsIds = new ArrayList<>(room.participantIds());
            for (String wsId : wsIds) {
                WebSocketSession session = activeSessions.get(wsId);
                if (session != null && session.isOpen()) {
                    try {
                        session.close(new CloseStatus(4001, "Room ended"));
                    } catch (IOException e) {
                        System.err.println("[SignalingWS] Error closing session " + wsId + ": " + e.getMessage());
                    }
                }
            }
        }
    }
}
