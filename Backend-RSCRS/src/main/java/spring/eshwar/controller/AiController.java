package spring.eshwar.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spring.eshwar.dto.ai.AiChatRequest;
import spring.eshwar.dto.ai.AiPromptRequest;
import spring.eshwar.dto.ai.AiPromptResponse;
import spring.eshwar.dto.chatbot.ChatbotRequest;
import spring.eshwar.dto.chatbot.ChatbotResponse;
import spring.eshwar.service.AiService;
import spring.eshwar.service.chatbot.ChatbotService;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201", "http://localhost:4202", "http://127.0.0.1:4200", "http://127.0.0.1:4201"}, allowCredentials = "true")
public class AiController {

    private final AiService aiService;
    private final ChatbotService chatbotService;

    public AiController(AiService aiService, ChatbotService chatbotService) {
        this.aiService = aiService;
        this.chatbotService = chatbotService;
    }

    @PostMapping("/test")
    public ResponseEntity<AiPromptResponse> testAi(@Valid @RequestBody AiPromptRequest request) {
        String result = aiService.generateResponse(request.getMessage());
        return ResponseEntity.ok(new AiPromptResponse(result));
    }

    @PostMapping("/chat")
    public ResponseEntity<AiPromptResponse> chatWithAi(@Valid @RequestBody AiChatRequest request) {
        ChatbotRequest chatbotReq = new ChatbotRequest(null, request.getMessage(), request.getRole());
        chatbotReq.setContext(request.getUserContext());
        ChatbotResponse chatbotRes = chatbotService.processMessage(chatbotReq);
        return ResponseEntity.ok(new AiPromptResponse(chatbotRes.getResponse()));
    }
}
