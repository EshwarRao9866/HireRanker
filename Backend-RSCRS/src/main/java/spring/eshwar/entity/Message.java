package spring.eshwar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Sender is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", referencedColumnName = "id", nullable = false)
    private User sender;

    @NotNull(message = "Receiver is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", referencedColumnName = "id", nullable = false)
    private User receiver;

    @NotBlank(message = "Message content cannot be blank")
    @Size(max = 2000, message = "Message length cannot exceed 2000 characters")
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @CreationTimestamp
    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt;

    @NotNull(message = "Read status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "read_status", nullable = false, length = 20)
    private MessageReadStatus readStatus = MessageReadStatus.UNREAD;

    public Message() {
    }

    public Message(User sender, User receiver, String message) {
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        this.readStatus = MessageReadStatus.UNREAD;
    }

    public Message(User sender, User receiver, String message, MessageReadStatus readStatus) {
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        this.readStatus = readStatus != null ? readStatus : MessageReadStatus.UNREAD;
    }

    public Message(Long id, User sender, User receiver, String message, MessageReadStatus readStatus, LocalDateTime sentAt) {
        this.id = id;
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        this.readStatus = readStatus != null ? readStatus : MessageReadStatus.UNREAD;
        this.sentAt = sentAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.sentAt == null) {
            this.sentAt = LocalDateTime.now();
        }
        if (this.readStatus == null) {
            this.readStatus = MessageReadStatus.UNREAD;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public User getReceiver() {
        return receiver;
    }

    public void setReceiver(User receiver) {
        this.receiver = receiver;
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

    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", senderId=" + (sender != null ? sender.getId() : null) +
                ", receiverId=" + (receiver != null ? receiver.getId() : null) +
                ", readStatus=" + readStatus +
                ", sentAt=" + sentAt +
                '}';
    }
}
