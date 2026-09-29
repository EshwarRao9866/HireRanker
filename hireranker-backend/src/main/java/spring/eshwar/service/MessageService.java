package spring.eshwar.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.eshwar.entity.Message;
import spring.eshwar.entity.MessageReadStatus;
import spring.eshwar.entity.User;
import spring.eshwar.exception.BadRequestException;
import spring.eshwar.exception.ResourceNotFoundException;
import spring.eshwar.repository.MessageRepository;
import spring.eshwar.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public MessageService(MessageRepository messageRepository, UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Message getMessageById(Long id) {
        return messageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Message", "id", id));
    }

    @Transactional(readOnly = true)
    public List<Message> getAllMessagesForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return messageRepository.findBySenderOrReceiverOrderBySentAtDesc(user, user);
    }

    @Transactional(readOnly = true)
    public List<Message> getConversation(Long user1Id, Long user2Id) {
        return messageRepository.findConversationBetweenUserIds(user1Id, user2Id);
    }

    @Transactional(readOnly = true)
    public List<Message> getMessagesSentByUser(Long senderId) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", senderId));
        return messageRepository.findBySenderOrderBySentAtDesc(sender);
    }

    @Transactional(readOnly = true)
    public List<Message> getMessagesReceivedByUser(Long receiverId) {
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", receiverId));
        return messageRepository.findByReceiverOrderBySentAtDesc(receiver);
    }

    @Transactional(readOnly = true)
    public List<Message> getUnreadMessages(Long receiverId) {
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", receiverId));
        return messageRepository.findByReceiverAndReadStatusOrderBySentAtDesc(receiver, MessageReadStatus.UNREAD);
    }

    @Transactional
    public Message sendMessage(Long senderId, Long receiverId, String content) {
        if (senderId == null) {
            throw new BadRequestException("Sender ID is required.");
        }
        if (receiverId == null) {
            throw new BadRequestException("Receiver ID is required.");
        }
        if (senderId.equals(receiverId)) {
            throw new BadRequestException("Cannot send a message to yourself.");
        }
        if (content == null || content.isBlank()) {
            throw new BadRequestException("Message content cannot be blank.");
        }
        if (content.trim().length() > 2000) {
            throw new BadRequestException("Message length cannot exceed 2000 characters.");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", senderId));

        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", receiverId));

        Message message = new Message(sender, receiver, content.trim(), MessageReadStatus.UNREAD);
        message.setSentAt(LocalDateTime.now());
        return messageRepository.save(message);
    }

    @Transactional
    public Message markAsRead(Long messageId) {
        Message message = getMessageById(messageId);
        message.setReadStatus(MessageReadStatus.READ);
        return messageRepository.save(message);
    }

    @Transactional
    public void markConversationAsRead(Long receiverId, Long senderId) {
        List<Message> conversation = messageRepository.findConversationBetweenUserIds(receiverId, senderId);
        for (Message msg : conversation) {
            if (msg.getReceiver().getId().equals(receiverId) && msg.getReadStatus() == MessageReadStatus.UNREAD) {
                msg.setReadStatus(MessageReadStatus.READ);
                messageRepository.save(msg);
            }
        }
    }

    @Transactional
    public void deleteMessage(Long id) {
        if (!messageRepository.existsById(id)) {
            throw new ResourceNotFoundException("Message", "id", id);
        }
        messageRepository.deleteById(id);
    }
}
