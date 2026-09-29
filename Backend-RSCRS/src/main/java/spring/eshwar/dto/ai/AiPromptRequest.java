package spring.eshwar.dto.ai;

import jakarta.validation.constraints.NotBlank;

public class AiPromptRequest {

    @NotBlank(message = "Message prompt cannot be blank")
    private String message;

    public AiPromptRequest() {
    }

    public AiPromptRequest(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
