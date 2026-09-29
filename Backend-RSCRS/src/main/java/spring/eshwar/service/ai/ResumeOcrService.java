package spring.eshwar.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fallback OCR Service for scanned or image-based resume PDFs.
 * Triggered ONLY when PDFBox extraction yields empty or unusable text (< 50 characters).
 * 
 * Uses PDFBox PDFRenderer to render PDF pages into images, then transcribes
 * resume text via low-latency Vision AI.
 */
@Service
public class ResumeOcrService {

    private static final Logger log = LoggerFactory.getLogger(ResumeOcrService.class);

    private final RestClient aiRestClient;
    private final ObjectMapper objectMapper;

    @Value("${application.ai.openrouter.api-key:${OPENROUTER_API_KEY:}}")
    private String openRouterApiKey;

    @Value("${application.ai.openrouter.api-url:${application.ai.openrouter.base-url:${OPENROUTER_API_URL:https://openrouter.ai/api/v1}}}")
    private String openRouterBaseUrl;

    @Value("${application.ai.ocr.model:${OCR_AI_MODEL:meta-llama/llama-3.2-11b-vision-instruct:free}}")
    private String ocrModel;

    @Value("${app.ocr.max-pages:3}")
    private int maxPagesToOcr;

    @Value("${app.ocr.enabled:true}")
    private boolean ocrEnabled;

    public ResumeOcrService(RestClient aiRestClient, ObjectMapper objectMapper) {
        this.aiRestClient = aiRestClient;
        this.objectMapper = objectMapper;
    }

    public boolean isOcrEnabled() {
        return ocrEnabled;
    }

    /**
     * Extracts text from a scanned/image PDF file by rendering pages to images and performing OCR.
     *
     * @param pdfFile the physical PDF file
     * @return extracted text from the scanned pages
     */
    public String extractTextFromScannedPdf(File pdfFile) {
        if (!ocrEnabled) {
            log.info("OCR fallback is disabled by configuration (app.ocr.enabled=false).");
            return "";
        }

        if (pdfFile == null || !pdfFile.exists() || !pdfFile.canRead()) {
            log.warn("Cannot perform OCR: PDF file is null or unreadable.");
            return "";
        }

        log.info("Starting OCR fallback extraction for scanned PDF: {}", pdfFile.getName());

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            if (document.isEncrypted()) {
                log.warn("Cannot perform OCR on encrypted PDF: {}", pdfFile.getName());
                return "";
            }

            int totalPages = document.getNumberOfPages();
            int pagesToProcess = Math.min(totalPages, Math.max(1, maxPagesToOcr));
            PDFRenderer renderer = new PDFRenderer(document);

            StringBuilder combinedText = new StringBuilder();

            for (int pageIndex = 0; pageIndex < pagesToProcess; pageIndex++) {
                log.info("Rendering page {}/{} of {} for OCR...", pageIndex + 1, pagesToProcess, pdfFile.getName());
                BufferedImage image = renderer.renderImageWithDPI(pageIndex, 180, ImageType.RGB);

                String pageText = transcribeImage(image, pageIndex + 1);
                if (pageText != null && !pageText.isBlank()) {
                    combinedText.append("\n--- PAGE ").append(pageIndex + 1).append(" ---\n");
                    combinedText.append(pageText.trim()).append("\n");
                }
            }

            String result = combinedText.toString().trim();
            log.info("OCR completed for {}. Extracted {} characters across {} page(s).",
                    pdfFile.getName(), result.length(), pagesToProcess);
            return result;

        } catch (Exception ex) {
            log.error("OCR fallback processing failed for {}: {}", pdfFile.getName(), ex.getMessage(), ex);
            return "";
        }
    }

    private String transcribeImage(BufferedImage image, int pageNumber) {
        if (openRouterApiKey == null || openRouterApiKey.trim().isEmpty()) {
            log.warn("OpenRouter API key is not configured for OCR Vision inference.");
            return "";
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpeg", baos);
            byte[] imageBytes = baos.toByteArray();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            String promptText = "You are a professional Document OCR engine. " +
                    "Transcribe all readable text from this scanned resume page verbatim. " +
                    "Extract candidate contact details, skills, summary, work history, projects, and education. " +
                    "Do NOT add conversational commentary, explanations, or markdown formatting.";

            List<Map<String, Object>> contentList = new ArrayList<>();
            contentList.add(Map.of("type", "text", "text", promptText));
            contentList.add(Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", "data:image/jpeg;base64," + base64Image)
            ));

            Map<String, Object> userMessage = Map.of(
                    "role", "user",
                    "content", contentList
            );

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", ocrModel);
            requestBody.put("messages", List.of(userMessage));
            requestBody.put("temperature", 0.1);
            requestBody.put("max_tokens", 2000);

            String endpoint = buildChatCompletionsUrl();

            JsonNode response = aiRestClient.post()
                    .uri(endpoint)
                    .header("Authorization", "Bearer " + openRouterApiKey.trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);

            if (response != null && response.has("choices")) {
                JsonNode choices = response.get("choices");
                if (choices.isArray() && !choices.isEmpty()) {
                    JsonNode messageNode = choices.get(0).path("message");
                    String content = messageNode.path("content").asText("");
                    return content != null ? content.trim() : "";
                }
            } else if (response != null && response.has("error")) {
                log.warn("Vision OCR API error: {}", response.path("error").path("message").asText());
            }

        } catch (Exception ex) {
            log.warn("Vision OCR call for page {} failed: {}", pageNumber, ex.getMessage());
        }

        return "";
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
