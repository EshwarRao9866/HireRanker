package spring.eshwar.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import spring.eshwar.dto.message.MessageRequest;
import spring.eshwar.dto.message.MessageResponse;
import spring.eshwar.entity.Message;
import spring.eshwar.entity.User;
import spring.eshwar.exception.UnauthorizedException;
import spring.eshwar.repository.UserRepository;
import spring.eshwar.service.MessageService;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201", "http://localhost:4202", "http://127.0.0.1:4200", "http://127.0.0.1:4201"}, allowCredentials = "true")
public class MessageController {

    private final MessageService messageService;
    private final UserRepository userRepository;

    public MessageController(MessageService messageService, UserRepository userRepository) {
        this.messageService = messageService;
        this.userRepository = userRepository;
    }

    /**
     * Sends a new message.
     * Sender must be the logged-in user. Impersonation is rejected.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> sendMessage(@Valid @RequestBody MessageRequest request,
                                                       Authentication authentication) {
        User currentUser = getCurrentUser(authentication);

        // Requirement: Sender must be the logged-in user. Do not allow users to impersonate another sender.
        if (request.getSenderId() != null && !request.getSenderId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Cannot send message on behalf of another user.");
        }

        Long senderId = currentUser.getId();
        Message message = messageService.sendMessage(
                senderId,
                request.getReceiverId(),
                request.getMessage()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(MessageResponse.fromEntity(message));
    }

    /**
     * Retrieves messages.
     * Defaults to all messages involving the logged-in user.
     * Specific sender/receiver filters require authorization.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @RequestParam(required = false) Long user1,
            @RequestParam(required = false) Long user2,
            @RequestParam(required = false) Long senderId,
            @RequestParam(required = false) Long receiverId,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        boolean admin = isAdmin(authentication);

        List<Message> messages;
        if (user1 != null && user2 != null) {
            if (!admin && !currentUser.getId().equals(user1) && !currentUser.getId().equals(user2)) {
                throw new AccessDeniedException("You are not authorized to view messages between other users.");
            }
            messages = messageService.getConversation(user1, user2);
        } else if (senderId != null) {
            if (!admin && !currentUser.getId().equals(senderId)) {
                throw new AccessDeniedException("You are not authorized to view messages sent by another user.");
            }
            messages = messageService.getMessagesSentByUser(senderId);
        } else if (receiverId != null) {
            if (!admin && !currentUser.getId().equals(receiverId)) {
                throw new AccessDeniedException("You are not authorized to view messages received by another user.");
            }
            messages = messageService.getMessagesReceivedByUser(receiverId);
        } else {
            // Default: return all messages involving the logged-in user
            messages = messageService.getAllMessagesForUser(currentUser.getId());
        }

        List<MessageResponse> response = messages.stream()
                .map(MessageResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single message by ID.
     * Users can only access their own messages.
     */
    @GetMapping("/{messageId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> getMessageById(@PathVariable Long messageId,
                                                          Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Message message = messageService.getMessageById(messageId);

        // Requirement: Users can only access their own messages.
        if (!isAdmin(authentication)
                && !message.getSender().getId().equals(currentUser.getId())
                && !message.getReceiver().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You are not authorized to access this message.");
        }

        return ResponseEntity.ok(MessageResponse.fromEntity(message));
    }

    /**
     * Retrieves conversation between two users.
     */
    @GetMapping("/conversation/{user1Id}/{user2Id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MessageResponse>> getConversation(@PathVariable Long user1Id,
                                                                 @PathVariable Long user2Id,
                                                                 Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        if (!isAdmin(authentication)
                && !currentUser.getId().equals(user1Id)
                && !currentUser.getId().equals(user2Id)) {
            throw new AccessDeniedException("You are not authorized to view this conversation.");
        }

        List<MessageResponse> response = messageService.getConversation(user1Id, user2Id).stream()
                .map(MessageResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves unread messages for a receiver.
     */
    @GetMapping("/unread/{receiverId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MessageResponse>> getUnreadMessages(@PathVariable Long receiverId,
                                                                   Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        if (!isAdmin(authentication) && !currentUser.getId().equals(receiverId)) {
            throw new AccessDeniedException("You are not authorized to view unread messages for another user.");
        }

        List<MessageResponse> response = messageService.getUnreadMessages(receiverId).stream()
                .map(MessageResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Marks a message as READ.
     * Only the receiver (or admin) can mark a message as read.
     */
    @PutMapping("/{messageId}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> markAsRead(@PathVariable Long messageId,
                                                      Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Message message = messageService.getMessageById(messageId);

        // Requirement: Users can only access their own messages. Only receiver can mark as read.
        if (!isAdmin(authentication) && !message.getReceiver().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Only the receiver of the message can mark it as read.");
        }

        Message updated = messageService.markAsRead(messageId);
        return ResponseEntity.ok(MessageResponse.fromEntity(updated));
    }

    /**
     * Deletes a message.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long id,
                                              Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Message message = messageService.getMessageById(id);

        if (!isAdmin(authentication)
                && !message.getSender().getId().equals(currentUser.getId())
                && !message.getReceiver().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You are not authorized to delete this message.");
        }

        messageService.deleteMessage(id);
        return ResponseEntity.noContent().build();
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Authentication is required.");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found."));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
