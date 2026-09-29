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
@Table(name = "interview_results")
public class InterviewResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Interview is required")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", referencedColumnName = "id", nullable = false, unique = true)
    private Interview interview;

    @Column(name = "technical_score")
    private Double technicalScore;

    @Column(name = "communication_score")
    private Double communicationScore;

    @Column(name = "problem_solving_score")
    private Double problemSolvingScore;

    @Column(name = "answer_relevance_score")
    private Double answerRelevanceScore;

    @Column(name = "completeness_score")
    private Double completenessScore;

    @Column(name = "overall_score")
    private Double overallScore;

    @Column(name = "strengths", columnDefinition = "TEXT")
    private String strengths;

    @Column(name = "weaknesses", columnDefinition = "TEXT")
    private String weaknesses;

    @Column(name = "improvement_topics", columnDefinition = "TEXT")
    private String improvementTopics;

    @Column(name = "recommendation", length = 50)
    private String recommendation;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "total_questions")
    private int totalQuestions;

    @Column(name = "answered_questions")
    private int answeredQuestions;

    @Column(name = "skipped_questions")
    private int skippedQuestions;

    @Column(name = "duration_minutes")
    private Double durationMinutes;

    @Column(name = "integrity_status", length = 30)
    private String integrityStatus; // NORMAL, REVIEW_REQUIRED

    @Column(name = "total_integrity_events")
    private Integer totalIntegrityEvents = 0;

    @Column(name = "multiple_person_events")
    private Integer multiplePersonEvents = 0;

    @Column(name = "tab_switch_events")
    private Integer tabSwitchEvents = 0;

    @Column(name = "attention_away_events")
    private Integer attentionAwayEvents = 0;

    @Column(name = "audio_anomaly_events")
    private Integer audioAnomalyEvents = 0;

    @CreationTimestamp
    @Column(name = "completed_at", nullable = false, updatable = false)
    private LocalDateTime completedAt;

    public InterviewResult() {
    }

    public InterviewResult(Interview interview, Double technicalScore, Double communicationScore,
                           Double problemSolvingScore, Double answerRelevanceScore, Double completenessScore,
                           Double overallScore, String strengths, String weaknesses,
                           String improvementTopics, String recommendation, String summary,
                           int totalQuestions, int answeredQuestions, int skippedQuestions,
                           Double durationMinutes) {
        this.interview = interview;
        this.technicalScore = technicalScore;
        this.communicationScore = communicationScore;
        this.problemSolvingScore = problemSolvingScore;
        this.answerRelevanceScore = answerRelevanceScore;
        this.completenessScore = completenessScore;
        this.overallScore = overallScore;
        this.strengths = strengths;
        this.weaknesses = weaknesses;
        this.improvementTopics = improvementTopics;
        this.recommendation = recommendation;
        this.summary = summary;
        this.totalQuestions = totalQuestions;
        this.answeredQuestions = answeredQuestions;
        this.skippedQuestions = skippedQuestions;
        this.durationMinutes = durationMinutes;
    }

    @PrePersist
    protected void onCreate() {
        if (this.completedAt == null) {
            this.completedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Interview getInterview() {
        return interview;
    }

    public void setInterview(Interview interview) {
        this.interview = interview;
    }

    public Double getTechnicalScore() {
        return technicalScore;
    }

    public void setTechnicalScore(Double technicalScore) {
        this.technicalScore = technicalScore;
    }

    public Double getCommunicationScore() {
        return communicationScore;
    }

    public void setCommunicationScore(Double communicationScore) {
        this.communicationScore = communicationScore;
    }

    public Double getProblemSolvingScore() {
        return problemSolvingScore;
    }

    public void setProblemSolvingScore(Double problemSolvingScore) {
        this.problemSolvingScore = problemSolvingScore;
    }

    public Double getAnswerRelevanceScore() {
        return answerRelevanceScore;
    }

    public void setAnswerRelevanceScore(Double answerRelevanceScore) {
        this.answerRelevanceScore = answerRelevanceScore;
    }

    public Double getCompletenessScore() {
        return completenessScore;
    }

    public void setCompletenessScore(Double completenessScore) {
        this.completenessScore = completenessScore;
    }

    public Double getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(Double overallScore) {
        this.overallScore = overallScore;
    }

    public String getStrengths() {
        return strengths;
    }

    public void setStrengths(String strengths) {
        this.strengths = strengths;
    }

    public String getWeaknesses() {
        return weaknesses;
    }

    public void setWeaknesses(String weaknesses) {
        this.weaknesses = weaknesses;
    }

    public String getImprovementTopics() {
        return improvementTopics;
    }

    public void setImprovementTopics(String improvementTopics) {
        this.improvementTopics = improvementTopics;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public int getAnsweredQuestions() {
        return answeredQuestions;
    }

    public void setAnsweredQuestions(int answeredQuestions) {
        this.answeredQuestions = answeredQuestions;
    }

    public int getSkippedQuestions() {
        return skippedQuestions;
    }

    public void setSkippedQuestions(int skippedQuestions) {
        this.skippedQuestions = skippedQuestions;
    }

    public Double getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Double durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getIntegrityStatus() {
        return integrityStatus;
    }

    public void setIntegrityStatus(String integrityStatus) {
        this.integrityStatus = integrityStatus;
    }

    public Integer getTotalIntegrityEvents() {
        return totalIntegrityEvents;
    }

    public void setTotalIntegrityEvents(Integer totalIntegrityEvents) {
        this.totalIntegrityEvents = totalIntegrityEvents;
    }

    public Integer getMultiplePersonEvents() {
        return multiplePersonEvents;
    }

    public void setMultiplePersonEvents(Integer multiplePersonEvents) {
        this.multiplePersonEvents = multiplePersonEvents;
    }

    public Integer getTabSwitchEvents() {
        return tabSwitchEvents;
    }

    public void setTabSwitchEvents(Integer tabSwitchEvents) {
        this.tabSwitchEvents = tabSwitchEvents;
    }

    public Integer getAttentionAwayEvents() {
        return attentionAwayEvents;
    }

    public void setAttentionAwayEvents(Integer attentionAwayEvents) {
        this.attentionAwayEvents = attentionAwayEvents;
    }

    public Integer getAudioAnomalyEvents() {
        return audioAnomalyEvents;
    }

    public void setAudioAnomalyEvents(Integer audioAnomalyEvents) {
        this.audioAnomalyEvents = audioAnomalyEvents;
    }
}
