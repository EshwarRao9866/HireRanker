package spring.eshwar.service.chatbot;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatbotSessionManager {

    private static final int MAX_TURNS = 10;
    private static final Duration SESSION_TTL = Duration.ofMinutes(30);

    public record ChatTurn(String role, String message, LocalDateTime timestamp) {}

    public static class ChatSession {
        private final String id;
        private final LocalDateTime createdAt;
        private LocalDateTime lastAccessedAt;
        private final List<ChatTurn> turns;

        public ChatSession(String id) {
            this.id = id;
            this.createdAt = LocalDateTime.now();
            this.lastAccessedAt = LocalDateTime.now();
            this.turns = new ArrayList<>();
        }

        public synchronized void addTurn(String role, String message) {
            this.lastAccessedAt = LocalDateTime.now();
            this.turns.add(new ChatTurn(role, message, LocalDateTime.now()));
            if (this.turns.size() > MAX_TURNS) {
                this.turns.remove(0);
            }
        }

        public synchronized List<ChatTurn> getTurns() {
            return Collections.unmodifiableList(new ArrayList<>(turns));
        }

        public boolean isExpired() {
            return Duration.between(lastAccessedAt, LocalDateTime.now()).compareTo(SESSION_TTL) > 0;
        }

        public String getId() {
            return id;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public LocalDateTime getLastAccessedAt() {
            return lastAccessedAt;
        }
    }

    private final Map<String, ChatSession> sessions = new ConcurrentHashMap<>();

    /**
     * Resolves an existing active session or initializes a new session.
     */
    public ChatSession getOrCreateSession(String conversationId) {
        cleanupExpiredSessions();

        if (conversationId == null || conversationId.isBlank() || !sessions.containsKey(conversationId)) {
            String newId = (conversationId != null && !conversationId.isBlank()) ? conversationId.trim() : UUID.randomUUID().toString();
            ChatSession session = new ChatSession(newId);
            sessions.put(newId, session);
            return session;
        }

        ChatSession existing = sessions.get(conversationId);
        if (existing.isExpired()) {
            sessions.remove(conversationId);
            ChatSession freshSession = new ChatSession(conversationId);
            sessions.put(conversationId, freshSession);
            return freshSession;
        }

        return existing;
    }

    public void addTurn(String conversationId, String role, String message) {
        ChatSession session = getOrCreateSession(conversationId);
        session.addTurn(role, message);
    }

    public List<ChatTurn> getTurns(String conversationId) {
        if (conversationId == null || !sessions.containsKey(conversationId)) {
            return Collections.emptyList();
        }
        return sessions.get(conversationId).getTurns();
    }

    public void resetSession(String conversationId) {
        if (conversationId != null) {
            sessions.remove(conversationId);
        }
    }

    private void cleanupExpiredSessions() {
        sessions.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}
