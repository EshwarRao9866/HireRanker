package spring.eshwar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "interview_answers")
public class InterviewAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Associated question is required")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", referencedColumnName = "id", nullable = false, unique = true)
    private InterviewQuestion question;

    @Column(name = "answer_text", columnDefinition = "TEXT")
    private String answerText;

    @Column(name = "response_time_seconds")
    private Double responseTimeSeconds;

    @Column(name = "answer_duration_seconds")
    private Double answerDurationSeconds;

    @Column(name = "audio_recording_url", length = 500)
    private String audioRecordingUrl;

    @Column(name = "technical_score")
    private Double technicalScore;

    @Column(name = "clarity_score")
    private Double clarityScore;

    @Column(name = "completeness_score")
    private Double completenessScore;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "overall_score")
    private Double overallScore;

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @CreationTimestamp
    @Column(name = "answered_at", nullable = false, updatable = false)
    private LocalDateTime answeredAt;

    public InterviewAnswer() {
    }

    public InterviewAnswer(InterviewQuestion question, String answerText, Double responseTimeSeconds,
                           Double answerDurationSeconds, String audioRecordingUrl) {
        this.question = question;
        this.answerText = answerText;
        this.responseTimeSeconds = responseTimeSeconds;
        this.answerDurationSeconds = answerDurationSeconds;
        this.audioRecordingUrl = audioRecordingUrl;
    }

    @PrePersist
    protected void onCreate() {
        if (this.answeredAt == null) {
            this.answeredAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public InterviewQuestion getQuestion() {
        return question;
    }

    public void setQuestion(InterviewQuestion question) {
        this.question = question;
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

    public Double getTechnicalScore() {
        return technicalScore;
    }

    public void setTechnicalScore(Double technicalScore) {
        this.technicalScore = technicalScore;
    }

    public Double getClarityScore() {
        return clarityScore;
    }

    public void setClarityScore(Double clarityScore) {
        this.clarityScore = clarityScore;
    }

    public Double getCompletenessScore() {
        return completenessScore;
    }

    public void setCompletenessScore(Double completenessScore) {
        this.completenessScore = completenessScore;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public Double getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(Double overallScore) {
        this.overallScore = overallScore;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public LocalDateTime getAnsweredAt() {
        return answeredAt;
    }

    public void setAnsweredAt(LocalDateTime answeredAt) {
        this.answeredAt = answeredAt;
    }
}
