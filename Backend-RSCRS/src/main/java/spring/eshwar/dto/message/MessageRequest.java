package spring.eshwar.dto.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class MessageRequest {

    private Long senderId;

    @NotNull(message = "Receiver ID is required")
    private Long receiverId;

    @NotBlank(message = "Message content cannot be blank")
    @Size(min = 1, max = 2000, message = "Message length must be between 1 and 2000 characters")
    private String message;

    public MessageRequest() {
    }

    public MessageRequest(Long receiverId, String message) {
        this.receiverId = receiverId;
        this.message = message;
    }

    public MessageRequest(Long senderId, Long receiverId, String message) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
