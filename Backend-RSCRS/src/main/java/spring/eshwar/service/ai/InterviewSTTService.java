package spring.eshwar.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import spring.eshwar.dto.interview.InterviewTranscriptionResponse;

/**
 * Dedicated Speech-to-Text (STT) service for HireRanker Live AI Interview.
 * Transcribes candidate spoken responses using low-latency Groq Whisper (whisper-large-v3-turbo).
 */
@Service
public class InterviewSTTService {

    private static final Logger log = LoggerFactory.getLogger(InterviewSTTService.class);

    private final GroqApiClient groqApiClient;

    @Value("${application.ai.interview.stt.provider:${INTERVIEW_STT_PROVIDER:groq}}")
    private String sttProvider;

    @Value("${application.ai.interview.stt.model:${INTERVIEW_STT_MODEL:whisper-large-v3-turbo}}")
    private String sttModel;

    public InterviewSTTService(GroqApiClient groqApiClient) {
        this.groqApiClient = groqApiClient;
    }

    public String getSttProvider() {
        return sttProvider;
    }

    public String getSttModel() {
        return sttModel;
    }

    /**
     * Transcribes an uploaded audio recording chunk or final response file.
     */
    public InterviewTranscriptionResponse transcribeAudio(MultipartFile audioFile, Long interviewId, Long questionId, boolean isFinal) {
        if (audioFile == null || audioFile.isEmpty()) {
            log.warn("[InterviewSTT] Received empty or null audio file for interviewId={}", interviewId);
            return InterviewTranscriptionResponse.failure("Audio file is empty or missing.", sttProvider, sttModel);
        }

        long startMs = System.currentTimeMillis();
        try {
            byte[] bytes = audioFile.getBytes();
            String originalFilename = audioFile.getOriginalFilename();
            String contentType = audioFile.getContentType();

            log.info("[InterviewSTT] Processing audio transcription request: size={} bytes, filename='{}', type='{}', interviewId={}, questionId={}, isFinal={}",
                    bytes.length, originalFilename, contentType, interviewId, questionId, isFinal);

            // Primary: Groq Whisper LPU
            if (groqApiClient.isConfigured()) {
                String transcript = groqApiClient.transcribeAudio(bytes, originalFilename, contentType, sttModel);
                long latency = System.currentTimeMillis() - startMs;

                if (transcript != null) {
                    log.info("[InterviewSTT] Transcription successful in {}ms: '{}'", latency, transcript);
                    return InterviewTranscriptionResponse.success(transcript, isFinal, "groq", sttModel, latency);
                }
            } else {
                log.warn("[InterviewSTT] Groq API client is not configured.");
            }

            long latency = System.currentTimeMillis() - startMs;
            return InterviewTranscriptionResponse.failure("Speech recognition service temporarily unavailable.", sttProvider, sttModel);

        } catch (Exception ex) {
            long latency = System.currentTimeMillis() - startMs;
            log.error("[InterviewSTT] Transcription failed after {}ms: {}", latency, ex.getMessage(), ex);
            return InterviewTranscriptionResponse.failure(ex.getMessage(), sttProvider, sttModel);
        }
    }
}
