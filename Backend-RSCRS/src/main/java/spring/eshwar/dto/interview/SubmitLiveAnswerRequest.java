package spring.eshwar.dto.interview;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public class SubmitLiveAnswerRequest {

    @NotBlank(message = "Answer transcript text cannot be blank")
    private String answerText;

    @PositiveOrZero(message = "Response time must be zero or positive")
    private Double responseTimeSeconds;

    @PositiveOrZero(message = "Answer duration must be zero or positive")
    private Double answerDurationSeconds;

    private String audioRecordingUrl;
    private Boolean assisted;

    public SubmitLiveAnswerRequest() {
    }

    public SubmitLiveAnswerRequest(String answerText, Double responseTimeSeconds,
                                   Double answerDurationSeconds, String audioRecordingUrl) {
        this.answerText = answerText;
        this.responseTimeSeconds = responseTimeSeconds;
        this.answerDurationSeconds = answerDurationSeconds;
        this.audioRecordingUrl = audioRecordingUrl;
        this.assisted = false;
    }

    public SubmitLiveAnswerRequest(String answerText, Double responseTimeSeconds,
                                   Double answerDurationSeconds, String audioRecordingUrl, Boolean assisted) {
        this.answerText = answerText;
        this.responseTimeSeconds = responseTimeSeconds;
        this.answerDurationSeconds = answerDurationSeconds;
        this.audioRecordingUrl = audioRecordingUrl;
        this.assisted = assisted;
    }

    public Boolean getAssisted() {
        return assisted;
    }

    public void setAssisted(Boolean assisted) {
        this.assisted = assisted;
    }

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public Double getResponseTimeSeconds() {
        return responseTimeSeconds;
    }

    public void setResponseTimeSeconds(Double responseTimeSeconds) {
        this.responseTimeSeconds = responseTimeSeconds;
    }

    public Double getAnswerDurationSeconds() {
        return answerDurationSeconds;
    }

    public void setAnswerDurationSeconds(Double answerDurationSeconds) {
        this.answerDurationSeconds = answerDurationSeconds;
    }

    public String getAudioRecordingUrl() {
        return audioRecordingUrl;
    }

    public void setAudioRecordingUrl(String audioRecordingUrl) {
        this.audioRecordingUrl = audioRecordingUrl;
    }
}
