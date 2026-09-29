package spring.eshwar.dto.chatbot;

import java.time.LocalDateTime;

public class ChatbotResponse {

    private String conversationId;
    private String response;
    private LocalDateTime timestamp;

    public ChatbotResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ChatbotResponse(String conversationId, String response) {
        this.conversationId = conversationId;
        this.response = response;
        this.timestamp = LocalDateTime.now();
    }

    public ChatbotResponse(String conversationId, String response, LocalDateTime timestamp) {
        this.conversationId = conversationId;
        this.response = response;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
