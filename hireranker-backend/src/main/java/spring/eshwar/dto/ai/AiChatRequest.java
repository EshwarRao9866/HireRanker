package spring.eshwar.dto.ai;

import jakarta.validation.constraints.NotBlank;

public class AiChatRequest {

    @NotBlank(message = "Message prompt cannot be blank")
    private String message;

    private String role; // "CANDIDATE" or "ADMIN"

    private String userContext; // Optional context, e.g. candidate name, skills, or job title

    public AiChatRequest() {
    }

    public AiChatRequest(String message, String role, String userContext) {
        this.message = message;
        this.role = role;
        this.userContext = userContext;
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

    public String getUserContext() {
        return userContext;
    }

    public void setUserContext(String userContext) {
        this.userContext = userContext;
    }
}
