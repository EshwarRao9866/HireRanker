package spring.eshwar.dto.chatbot;

import jakarta.validation.constraints.NotBlank;

public class ChatbotRequest {

    private String conversationId;

    @NotBlank(message = "Message cannot be blank")
    private String message;

    private String role;
    private String context;

    public ChatbotRequest() {
    }

    public ChatbotRequest(String message) {
        this.message = message;
    }

    public ChatbotRequest(String conversationId, String message) {
        this.conversationId = conversationId;
        this.message = message;
    }

    public ChatbotRequest(String conversationId, String message, String role) {
        this.conversationId = conversationId;
        this.message = message;
        this.role = role;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }
}
