package spring.eshwar.dto.message;

import spring.eshwar.entity.Message;
import spring.eshwar.entity.MessageReadStatus;

import java.time.LocalDateTime;

public class MessageResponse {

    private Long id;
    private Long senderId;
    private String senderName;
    private Long receiverId;
    private String receiverName;
    private String message;
    private LocalDateTime sentAt;
    private MessageReadStatus readStatus;

    public MessageResponse() {
    }

    public MessageResponse(Long id, Long senderId, String senderName, Long receiverId,
                           String receiverName, String message, LocalDateTime sentAt,
                           MessageReadStatus readStatus) {
        this.id = id;
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.receiverName = receiverName;
        this.message = message;
        this.sentAt = sentAt;
        this.readStatus = readStatus;
    }

    public static MessageResponse fromEntity(Message msg) {
        if (msg == null) {
            return null;
        }

        Long senderId = (msg.getSender() != null) ? msg.getSender().getId() : null;
        String senderName = (msg.getSender() != null) ? msg.getSender().getName() : null;
        Long receiverId = (msg.getReceiver() != null) ? msg.getReceiver().getId() : null;
        String receiverName = (msg.getReceiver() != null) ? msg.getReceiver().getName() : null;

        return new MessageResponse(
                msg.getId(),
                senderId,
                senderName,
                receiverId,
                receiverName,
                msg.getMessage(),
                msg.getSentAt(),
                msg.getReadStatus()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public MessageReadStatus getReadStatus() {
        return readStatus;
    }

    public void setReadStatus(MessageReadStatus readStatus) {
        this.readStatus = readStatus;
    }
}
