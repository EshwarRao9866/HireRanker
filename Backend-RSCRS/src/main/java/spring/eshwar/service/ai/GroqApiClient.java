package spring.eshwar.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Low-latency client for Groq OpenAI-compatible API (LPU inference).
 * Provides sub-second completions for live interview questions and answer evaluations.
 */
@Component
public class GroqApiClient {

    private static final Logger log = LoggerFactory.getLogger(GroqApiClient.class);

    private final RestClient aiRestClient;
    private final ObjectMapper objectMapper;

    @Value("${application.ai.groq.api-key:${GROQ_API_KEY:}}")
    private String groqApiKey;

    @Value("${application.ai.interview.groq-fallback-model:${application.ai.groq.model:${INTERVIEW_GROQ_MODEL:llama-3.3-70b-versatile}}}")
    private String groqModel;

    @Value("${application.ai.groq.api-url:https://api.groq.com/openai/v1/chat/completions}")
    private String groqApiUrl;

    @Value("${application.ai.groq.audio-transcription-url:https://api.groq.com/openai/v1/audio/transcriptions}")
    private String groqAudioUrl;

    public GroqApiClient(RestClient aiRestClient, ObjectMapper objectMapper) {
        this.aiRestClient = aiRestClient;
        this.objectMapper = objectMapper;
    }

    public boolean isConfigured() {
        return groqApiKey != null && !groqApiKey.trim().isEmpty();
    }

    public String getGroqModel() {
        return groqModel;
    }

    /**
     * Executes a low-latency chat completion via Groq with cascading model fallbacks.
     */
    public String callChatCompletion(String systemPrompt, String userPrompt, boolean jsonMode) {
        if (!isConfigured()) {
            return null;
        }

        // 1. Primary Groq model (e.g. openai/gpt-oss-20b)
        String result = executeGroqRequest(groqModel, systemPrompt, userPrompt, jsonMode);
        if (result != null && !result.isBlank()) {
            return result;
        }

        // 2. Secondary Groq model: openai/gpt-oss-120b
        if (!"openai/gpt-oss-120b".equalsIgnoreCase(groqModel)) {
            log.info("Attempting secondary Groq model 'openai/gpt-oss-120b'...");
            result = executeGroqRequest("openai/gpt-oss-120b", systemPrompt, userPrompt, jsonMode);
            if (result != null && !result.isBlank()) {
                return result;
            }
        }

        // 3. Tertiary Groq model: groq/compound-mini
        if (!"groq/compound-mini".equalsIgnoreCase(groqModel)) {
            log.info("Attempting tertiary Groq model 'groq/compound-mini'...");
            return executeGroqRequest("groq/compound-mini", systemPrompt, userPrompt, jsonMode);
        }

        return null;
    }

    private String executeGroqRequest(String targetModel, String systemPrompt, String userPrompt, boolean jsonMode) {
        try {
            log.info("Dispatching low-latency prompt to Groq model '{}'...", targetModel);

            List<Map<String, String>> messages = new ArrayList<>();
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                messages.add(Map.of("role", "system", "content", systemPrompt.trim()));
            }
            messages.add(Map.of("role", "user", "content", userPrompt.trim()));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", targetModel);
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.2);
            requestBody.put("max_tokens", 1500);

            if (jsonMode) {
                requestBody.put("response_format", Map.of("type", "json_object"));
            }

            JsonNode response = aiRestClient.post()
                    .uri(groqApiUrl)
                    .header("Authorization", "Bearer " + groqApiKey.trim())
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
                        log.info("Successfully received sub-second response from Groq model '{}'.", targetModel);
                        return content.trim();
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Groq API call to '{}' failed: {}", targetModel, ex.getMessage());
        }
        return null;
    }

    /**
     * Executes speech-to-text audio transcription via Groq Whisper with sub-second latency.
     */
    public String transcribeAudio(byte[] audioBytes, String filename, String mimeType, String preferredModel) {
        if (!isConfigured() || audioBytes == null || audioBytes.length == 0) {
            return null;
        }

        String targetModel = (preferredModel != null && !preferredModel.isBlank()) ? preferredModel : "whisper-large-v3-turbo";
        long startMs = System.currentTimeMillis();

        String result = executeGroqTranscription(audioBytes, filename, targetModel);
        if (result != null && !result.isBlank()) {
            log.info("[InterviewSTT] Groq Whisper model '{}' transcribed {} bytes in {}ms: '{}'",
                    targetModel, audioBytes.length, (System.currentTimeMillis() - startMs), result);
            return result;
        }

        // Secondary fallback to whisper-large-v3 if turbo failed or vice versa
        String fallbackModel = "whisper-large-v3-turbo".equalsIgnoreCase(targetModel) ? "whisper-large-v3" : "whisper-large-v3-turbo";
        log.info("[InterviewSTT] Attempting Groq Whisper fallback model '{}'...", fallbackModel);
        result = executeGroqTranscription(audioBytes, filename, fallbackModel);
        if (result != null && !result.isBlank()) {
            log.info("[InterviewSTT] Groq Whisper fallback model '{}' transcribed {} bytes in {}ms: '{}'",
                    fallbackModel, audioBytes.length, (System.currentTimeMillis() - startMs), result);
            return result;
        }

        return null;
    }

    private String executeGroqTranscription(byte[] audioBytes, String filename, String targetModel) {
        try {
            final String fname = (filename != null && !filename.isBlank()) ? filename : "interview_answer.webm";
            ByteArrayResource fileResource = new ByteArrayResource(audioBytes) {
                @Override
                public String getFilename() {
                    return fname;
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", fileResource);
            body.add("model", targetModel);
            body.add("response_format", "json");
            body.add("language", "en");

            RestClient sttClient = RestClient.builder().build();
            JsonNode response = sttClient.post()
                    .uri(groqAudioUrl)
                    .header("Authorization", "Bearer " + groqApiKey.trim())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            if (response != null && response.has("text")) {
                return response.path("text").asText("").trim();
            }
        } catch (Exception ex) {
            log.warn("[InterviewSTT] Groq Whisper transcription call to '{}' failed: {}", targetModel, ex.getMessage());
        }
        return null;
    }
}
