package spring.eshwar.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Task-specific AI service for HireRanker Chatbot utilizing OpenRouter qwen/qwen3-8b:free
 * (with automatic failover to openrouter/free).
 * Optimized for low latency, grounded documentation recall, and concise 1-3 sentence answers.
 */
@Service
public class ChatbotAIService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotAIService.class);

    private final OpenRouterApiClient openRouterApiClient;
    private final GroqApiClient groqApiClient;

    @Value("${application.ai.chatbot.model:${application.ai.openrouter.chatbot-model:${CHATBOT_AI_MODEL:openai/gpt-oss-20b}}}")
    private String chatbotModel;

    @Value("${application.ai.openrouter.fallback-model:${AI_FALLBACK_MODEL:openrouter/free}}")
    private String fallbackModel;

    public ChatbotAIService(OpenRouterApiClient openRouterApiClient, GroqApiClient groqApiClient) {
        this.openRouterApiClient = openRouterApiClient;
        this.groqApiClient = groqApiClient;
    }

    public String getChatbotModel() {
        return chatbotModel;
    }

    /**
     * Generates a conversational response using OpenRouter (with automatic failover).
     * Optimized for low latency, grounded documentation recall, and concise 1-3 sentence answers
     * with zero reasoning/scratchpad leakage.
     */
    public String generateChatbotReply(String groundedPrompt) {
        String systemPrompt = "You are HireRanker AI Recruiter Assistant. Provide direct, factual, and concise answers in 1 to 3 short sentences. NEVER output reasoning, thinking process, scratchpad, analysis steps, or markdown titles. Answer immediately and directly.";

        // 1. Primary: OpenRouter (with automatic fallback)
        if (openRouterApiClient.isConfigured()) {
            try {
                String openRouterReply = openRouterApiClient.callChatCompletionWithFallback(
                        chatbotModel,
                        systemPrompt,
                        groundedPrompt,
                        false,
                        0.2,
                        450
                );
                if (openRouterReply != null && !openRouterReply.isBlank()) {
                    return cleanModelResponse(openRouterReply);
                }
            } catch (Exception ex) {
                log.warn("OpenRouter chatbot execution failed: {}. Checking secondary fallbacks.", ex.getMessage());
            }
        }

        // 2. Secondary Fallback: Groq (if configured)
        if (groqApiClient.isConfigured()) {
            try {
                String groqReply = groqApiClient.callChatCompletion(
                        systemPrompt,
                        groundedPrompt,
                        false
                );
                if (groqReply != null && !groqReply.isBlank()) {
                    return cleanModelResponse(groqReply);
                }
            } catch (Exception ex) {
                log.warn("Groq secondary fallback failed: {}", ex.getMessage());
            }
        }

        return null;
    }

    /**
     * Sanitizes AI model responses to guarantee no thinking process, scratchpad,
     * or markdown reasoning leaks through to the recruiter/admin.
     */
    public String cleanModelResponse(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String text = raw.trim();

        // 1. Extract from JSON object if the model wrapped it in {"answer": "..."}
        if (text.startsWith("{") && text.endsWith("}")) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(text);
                if (root.has("answer")) {
                    text = root.path("answer").asText("").trim();
                } else if (root.has("response")) {
                    text = root.path("response").asText("").trim();
                } else if (root.has("message")) {
                    text = root.path("message").asText("").trim();
                }
            } catch (Exception ignored) {
                // fall through to text cleaning
            }
        }

        // 2. Strip <think>...</think>, <reasoning>...</reasoning>, and <thinking>...</thinking> tags
        text = text.replaceAll("(?is)<think>.*?</think>", "").trim();
        text = text.replaceAll("(?is)<reasoning>.*?</reasoning>", "").trim();
        text = text.replaceAll("(?is)<thinking>.*?</thinking>", "").trim();

        // 3. Strip "Here's a thinking process: ... Answer: " or similar scratchpads
        if (text.toLowerCase().contains("here's a thinking process") || text.toLowerCase().contains("thinking process:") || text.toLowerCase().contains("let's analyze")) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?is)(?:\\n|\\r\\n)?\\s*(?:\\*\\*)?(?:answer|response|final answer|final response|conclusion):\\s*(?:\\*\\*)?\\s*(.*)$").matcher(text);
            if (m.find()) {
                text = m.group(1).trim();
            } else {
                // Filter out reasoning paragraphs (e.g. "1. Analyze...", "2. Identify...", etc.)
                String[] paragraphs = text.split("\\n\\s*\\n+");
                StringBuilder cleanParagraphs = new StringBuilder();
                for (String p : paragraphs) {
                    String pt = p.trim();
                    if (!pt.toLowerCase().startsWith("here's a thinking process")
                            && !pt.toLowerCase().startsWith("thinking process:")
                            && !pt.toLowerCase().startsWith("let's analyze")
                            && !pt.matches("(?i)^\\d+\\.\\s+(?:analyze|identify|formulate|consider|determine|evaluate|note|review|draft|outline).*")) {
                        if (cleanParagraphs.length() > 0) cleanParagraphs.append(" ");
                        cleanParagraphs.append(pt);
                    }
                }
                text = cleanParagraphs.toString().trim();
            }
        }

        // 4. Strip leftover prefix labels and markdown headers
        text = text.replaceFirst("^(?i)(?:\\*\\*)?(?:answer|response|final answer|final response|direct answer):\\s*(?:\\*\\*)?\\s*", "").trim();
        text = text.replaceFirst("^(?i)###\\s*(?:Answer|Response|Direct Answer)[^\\n]*\\n+", "").trim();

        // 5. If the remaining text is still internal reasoning or empty, return empty string so fallback is used
        if (text.matches("(?is)^(?:\\d+\\.\\s+(?:analyze|identify)|here's a thinking|analysis:).*")) {
            return "";
        }

        // 6. Strip code fences if wrapped in ``` ... ```
        if (text.startsWith("```") && text.endsWith("```")) {
            text = text.replaceAll("^```(?:json|markdown)?\\s*", "").replaceAll("\\s*```$", "").trim();
        }

        return text.trim();
    }
}
