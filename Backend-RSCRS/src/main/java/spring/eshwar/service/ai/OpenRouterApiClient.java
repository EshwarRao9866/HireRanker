package spring.eshwar.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unified OpenAI-compatible client for OpenRouter AI Gateway.
 * Dispatches task-specific inference requests to:
 * - AI Chatbot: qwen/qwen3-8b:free
 * - Resume Screening: qwen/qwen3-30b-a3b:free
 * - Live Voice Interview: openai/gpt-oss-20b:free
 * - Configurable Fallback: openrouter/free
 */
@Component
public class OpenRouterApiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterApiClient.class);

    private final RestClient aiRestClient;
    private final ObjectMapper objectMapper;

    @Value("${application.ai.openrouter.api-key:${OPENROUTER_API_KEY:}}")
    private String openRouterApiKey;

    @Value("${application.ai.openrouter.api-url:${application.ai.openrouter.base-url:${OPENROUTER_API_URL:https://openrouter.ai/api/v1}}}")
    private String openRouterBaseUrl;

    @Value("${application.ai.openrouter.fallback-model:${AI_FALLBACK_MODEL:openrouter/free}}")
    private String fallbackModel;

    @Value("${application.ai.openrouter.site-url:https://hireranker.local}")
    private String siteUrl;

    @Value("${application.ai.openrouter.site-name:HireRanker}")
    private String siteName;

    public OpenRouterApiClient(RestClient aiRestClient, ObjectMapper objectMapper) {
        this.aiRestClient = aiRestClient;
        this.objectMapper = objectMapper;
    }

    public boolean isConfigured() {
        return openRouterApiKey != null && !openRouterApiKey.trim().isEmpty();
    }

    public String getFallbackModel() {
        return fallbackModel;
    }

    /**
     * Executes chat completion with automatic failover to the configured fallback model.
     */
    public String callChatCompletionWithFallback(String primaryModel,
                                                String systemPrompt,
                                                String userPrompt,
                                                boolean jsonMode,
                                                Double temperature,
                                                Integer maxTokens) {
        String targetPrimary = (primaryModel != null && !primaryModel.isBlank()) ? primaryModel.trim() : fallbackModel;

        // 1. Try primary model
        String response = callChatCompletion(targetPrimary, systemPrompt, userPrompt, jsonMode, temperature, maxTokens);
        if (response != null && !response.isBlank()) {
            return response;
        }

        // 2. If primary model fails and is different from fallback, trigger fallback
        if (fallbackModel != null && !fallbackModel.isBlank() && !fallbackModel.equalsIgnoreCase(targetPrimary)) {
            log.warn("Primary OpenRouter model '{}' returned null or failed. Triggering configured fallback '{}'...",
                    targetPrimary, fallbackModel);
            String fallbackResponse = callChatCompletion(fallbackModel.trim(), systemPrompt, userPrompt, jsonMode, temperature, maxTokens);
            if (fallbackResponse != null && !fallbackResponse.isBlank()) {
                log.info("Successfully received completion from OpenRouter fallback model '{}'.", fallbackModel);
                return fallbackResponse;
            }
        }

        return null;
    }

    /**
     * Executes chat completion for a specific model via OpenRouter's OpenAI-compatible endpoint.
     */
    public String callChatCompletion(String model,
                                     String systemPrompt,
                                     String userPrompt,
                                     boolean jsonMode,
                                     Double temperature,
                                     Integer maxTokens) {
        if (!isConfigured()) {
            log.debug("OpenRouter API key is not configured. Skipping remote API call.");
            return null;
        }

        if (userPrompt == null || userPrompt.isBlank()) {
            log.warn("Cannot call OpenRouter with an empty prompt.");
            return null;
        }

        String effectiveModel = (model != null && !model.isBlank()) ? model.trim() : fallbackModel;
        String endpoint = buildChatCompletionsUrl();

        try {
            log.info("Dispatching prompt to OpenRouter model '{}'...", effectiveModel);

            List<Map<String, String>> messages = new ArrayList<>();
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                messages.add(Map.of("role", "system", "content", systemPrompt.trim()));
            }
            messages.add(Map.of("role", "user", "content", userPrompt.trim()));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", effectiveModel);
            requestBody.put("messages", messages);
            requestBody.put("temperature", (temperature != null) ? temperature : 0.2);

            if (maxTokens != null && maxTokens > 0) {
                requestBody.put("max_tokens", maxTokens);
            }

            if (jsonMode) {
                requestBody.put("response_format", Map.of("type", "json_object"));
            }

            JsonNode response = aiRestClient.post()
                    .uri(endpoint)
                    .header("Authorization", "Bearer " + openRouterApiKey.trim())
                    .header("HTTP-Referer", siteUrl)
                    .header("X-Title", siteName)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);

            if (response != null && response.has("choices")) {
                JsonNode choices = response.get("choices");
                if (choices.isArray() && !choices.isEmpty()) {
                    JsonNode messageNode = choices.get(0).path("message");
                    String content = messageNode.path("content").asText("");
                    if (!content.isBlank()) {
                        log.info("Successfully received completion from OpenRouter model '{}'.", effectiveModel);
                        return content.trim();
                    }
                }
            } else if (response != null && response.has("error")) {
                String errorMsg = response.path("error").path("message").asText("Unknown OpenRouter error");
                log.warn("OpenRouter API returned error for model '{}': {}", effectiveModel, errorMsg);
            }
        } catch (Exception ex) {
            log.warn("OpenRouter API call to model '{}' failed: {}", effectiveModel, ex.getMessage());
        }

        return null;
    }

    private String buildChatCompletionsUrl() {
        String base = (openRouterBaseUrl != null && !openRouterBaseUrl.isBlank())
                ? openRouterBaseUrl.trim()
                : "https://openrouter.ai/api/v1";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.endsWith("/chat/completions")) {
            return base;
        }
        return base + "/chat/completions";
    }
}
