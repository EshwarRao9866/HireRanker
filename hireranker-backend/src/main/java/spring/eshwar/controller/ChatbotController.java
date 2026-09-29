package spring.eshwar.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spring.eshwar.dto.chatbot.ChatbotRequest;
import spring.eshwar.dto.chatbot.ChatbotResponse;
import spring.eshwar.service.chatbot.ChatbotService;
import spring.eshwar.service.chatbot.ChatbotSessionManager;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201", "http://localhost:4202", "http://127.0.0.1:4200", "http://127.0.0.1:4201"}, allowCredentials = "true")
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    /**
     * Primary endpoint for conversational HireRanker help & troubleshooting.
     */
    @PostMapping("/message")
    public ResponseEntity<ChatbotResponse> sendMessage(@Valid @RequestBody ChatbotRequest request) {
        ChatbotResponse response = chatbotService.processMessage(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Resets a conversation session so the user can start a fresh dialogue.
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetConversation(@RequestBody(required = false) Map<String, String> body) {
        String conversationId = (body != null) ? body.get("conversationId") : null;
        if (conversationId != null && !conversationId.isBlank()) {
            chatbotService.resetConversation(conversationId.trim());
        }
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Conversation session reset successfully."
        ));
    }

    /**
     * Retrieves recent turn history for an active conversation session.
     */
    @GetMapping("/history/{conversationId}")
    public ResponseEntity<List<ChatbotSessionManager.ChatTurn>> getHistory(@PathVariable String conversationId) {
        List<ChatbotSessionManager.ChatTurn> history = chatbotService.getConversationHistory(conversationId);
        return ResponseEntity.ok(history);
    }
}
