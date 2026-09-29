package spring.eshwar.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import spring.eshwar.dto.interview.InterviewTranscriptionResponse;
import spring.eshwar.dto.interview.LiveInterviewResultResponse;
import spring.eshwar.dto.interview.LiveQuestionResponse;
import spring.eshwar.dto.interview.SkipLiveQuestionRequest;
import spring.eshwar.dto.interview.StartInterviewRequest;
import spring.eshwar.dto.interview.StartInterviewResponse;
import spring.eshwar.dto.interview.SubmitLiveAnswerRequest;
import spring.eshwar.dto.interview.SubmitLiveAnswerResponse;
import spring.eshwar.service.LiveInterviewService;
import spring.eshwar.service.ai.InterviewSTTService;

import java.util.List;
import java.util.Map;
import spring.eshwar.entity.InterviewIntegrityEvent;

@RestController
@RequestMapping("/api/interviews")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201", "http://localhost:4202", "http://127.0.0.1:4200", "http://127.0.0.1:4201"}, allowCredentials = "true")
public class LiveInterviewController {

    private final LiveInterviewService liveInterviewService;
    private final InterviewSTTService interviewSTTService;

    public LiveInterviewController(LiveInterviewService liveInterviewService, InterviewSTTService interviewSTTService) {
        this.liveInterviewService = liveInterviewService;
        this.interviewSTTService = interviewSTTService;
    }

    /**
     * Start or resume a live interview session.
     */
    @PostMapping("/start")
    public ResponseEntity<StartInterviewResponse> startInterview(@Valid @RequestBody StartInterviewRequest request) {
        StartInterviewResponse response = liveInterviewService.startInterview(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get the current active question or fetch the next adaptive question.
     */
    @GetMapping("/{interviewId}/next-question")
    public ResponseEntity<LiveQuestionResponse> getNextQuestion(@PathVariable Long interviewId) {
        LiveQuestionResponse response = liveInterviewService.getNextQuestion(interviewId);
        return ResponseEntity.ok(response);
    }

    /**
     * Submit candidate's spoken answer for evaluation and retrieve next question.
     */
    @PostMapping("/{interviewId}/questions/{questionId}/answer")
    public ResponseEntity<SubmitLiveAnswerResponse> submitAnswer(
            @PathVariable Long interviewId,
            @PathVariable Long questionId,
            @Valid @RequestBody SubmitLiveAnswerRequest request) {
        SubmitLiveAnswerResponse response = liveInterviewService.submitAnswer(interviewId, questionId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Skip question when 15-second speech countdown expires or candidate skips.
     */
    @PostMapping("/{interviewId}/questions/{questionId}/skip")
    public ResponseEntity<SubmitLiveAnswerResponse> skipQuestion(
            @PathVariable Long interviewId,
            @PathVariable Long questionId,
            @RequestBody(required = false) SkipLiveQuestionRequest request) {
        SubmitLiveAnswerResponse response = liveInterviewService.skipQuestion(interviewId, questionId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Complete the live interview session and generate final scorecard.
     */
    @PostMapping("/{interviewId}/complete")
    public ResponseEntity<LiveInterviewResultResponse> completeInterview(@PathVariable Long interviewId) {
        LiveInterviewResultResponse response = liveInterviewService.completeInterview(interviewId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve the final scorecard for an interview.
     */
    @GetMapping("/{interviewId}/result")
    public ResponseEntity<LiveInterviewResultResponse> getInterviewResult(@PathVariable Long interviewId) {
        LiveInterviewResultResponse response = liveInterviewService.getInterviewResult(interviewId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve the final scorecard for an application's latest interview.
     */
    @GetMapping("/application/{applicationId}/result")
    public ResponseEntity<LiveInterviewResultResponse> getInterviewResultByApplication(@PathVariable Long applicationId) {
        LiveInterviewResultResponse response = liveInterviewService.getInterviewResultByApplication(applicationId);
        return ResponseEntity.ok(response);
    }

    /**
     * Candidate leaves assessment early; marks session as abandoned / requiring HR review.
     */
    @PostMapping("/{interviewId}/exit")
    public ResponseEntity<Map<String, String>> exitInterview(@PathVariable Long interviewId) {
        liveInterviewService.exitInterview(interviewId);
        return ResponseEntity.ok(Map.of("message", "Assessment exited. Status marked as pending HR review.", "status", "PENDING_HR_REVIEW"));
    }

    /**
     * Admin/HR approves retake of an incomplete/abandoned assessment.
     */
    @PostMapping("/{interviewId}/approve-retake")
    public ResponseEntity<Map<String, String>> approveRetake(@PathVariable Long interviewId) {
        liveInterviewService.approveRetake(interviewId);
        return ResponseEntity.ok(Map.of("message", "Retake approved successfully. Candidate may now rejoin.", "status", "RETAKE_APPROVED"));
    }

    /**
     * Record batch integrity events detected during live interview session.
     */
    @PostMapping("/{interviewId}/integrity-events")
    public ResponseEntity<Map<String, Object>> recordIntegrityEvents(
            @PathVariable Long interviewId,
            @RequestBody spring.eshwar.dto.interview.RecordIntegrityEventsRequest request) {
        var saved = liveInterviewService.recordIntegrityEvents(interviewId, request);
        return ResponseEntity.ok(Map.of("success", true, "recordedCount", saved.size()));
    }

    /**
     * Get all recorded integrity events for HR/Admin review.
     */
    @GetMapping("/{interviewId}/integrity-events")
    public ResponseEntity<List<spring.eshwar.entity.InterviewIntegrityEvent>> getIntegrityEvents(@PathVariable Long interviewId) {
        var events = liveInterviewService.getIntegrityEvents(interviewId);
        return ResponseEntity.ok(events);
    }

    /**
     * Transcribe candidate's spoken audio for live interview using dedicated Groq Whisper.
     */
    @PostMapping(value = "/{interviewId}/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InterviewTranscriptionResponse> transcribeAudio(
            @PathVariable Long interviewId,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "audio", required = false) MultipartFile audio,
            @RequestParam(value = "questionId", required = false) Long questionId,
            @RequestParam(value = "isFinal", defaultValue = "false") boolean isFinal) {
        MultipartFile effectiveFile = (file != null && !file.isEmpty()) ? file : audio;
        InterviewTranscriptionResponse response = interviewSTTService.transcribeAudio(effectiveFile, interviewId, questionId, isFinal);
        return ResponseEntity.ok(response);
    }

    /**
     * General transcription endpoint for pre-check audio verification.
     */
    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InterviewTranscriptionResponse> transcribeGeneralAudio(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "audio", required = false) MultipartFile audio,
            @RequestParam(value = "isFinal", defaultValue = "true") boolean isFinal) {
        MultipartFile effectiveFile = (file != null && !file.isEmpty()) ? file : audio;
        InterviewTranscriptionResponse response = interviewSTTService.transcribeAudio(effectiveFile, null, null, isFinal);
        return ResponseEntity.ok(response);
    }
}
