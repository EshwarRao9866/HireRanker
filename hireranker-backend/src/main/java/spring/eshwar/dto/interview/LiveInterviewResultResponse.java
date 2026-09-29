package spring.eshwar.dto.interview;

import spring.eshwar.entity.InterviewResult;

import java.time.LocalDateTime;

public class LiveInterviewResultResponse {

    private Long resultId;
    private Long interviewId;
    private Long applicationId;
    private String candidateName;
    private String jobTitle;
    private Double technicalScore;
    private Double communicationScore;
    private Double problemSolvingScore;
    private Double answerRelevanceScore;
    private Double completenessScore;
    private Double overallScore;
    private String strengths;
    private String weaknesses;
    private String improvementTopics;
    private String recommendation;
    private String summary;
    private int totalQuestions;
    private int answeredQuestions;
    private int skippedQuestions;
    private Double durationMinutes;
    private LocalDateTime completedAt;

    private String integrityStatus; // NORMAL, REVIEW_REQUIRED
    private Integer totalIntegrityEvents = 0;
    private Integer multiplePersonEvents = 0;
    private Integer tabSwitchEvents = 0;
    private Integer attentionAwayEvents = 0;
    private Integer audioAnomalyEvents = 0;

    public LiveInterviewResultResponse() {
    }

    public LiveInterviewResultResponse(Long resultId, Long interviewId, Long applicationId,
                                      String candidateName, String jobTitle, Double technicalScore,
                                      Double communicationScore, Double problemSolvingScore,
                                      Double answerRelevanceScore, Double completenessScore,
                                      Double overallScore, String strengths, String weaknesses,
                                      String improvementTopics, String recommendation, String summary,
                                      int totalQuestions, int answeredQuestions, int skippedQuestions,
                                      Double durationMinutes, LocalDateTime completedAt) {
        this.resultId = resultId;
        this.interviewId = interviewId;
        this.applicationId = applicationId;
        this.candidateName = candidateName;
        this.jobTitle = jobTitle;
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
        this.completedAt = completedAt;
    }

    public static LiveInterviewResultResponse fromEntity(InterviewResult r) {
        if (r == null) {
            return null;
        }
        Long interviewId = (r.getInterview() != null) ? r.getInterview().getId() : null;
        Long applicationId = null;
        String candidateName = null;
        String jobTitle = null;

        try {
            if (r.getInterview() != null && r.getInterview().getApplication() != null) {
                var app = r.getInterview().getApplication();
                applicationId = app.getId();
                if (app.getCandidate() != null) {
                    candidateName = app.getCandidate().getFullName();
                }
                if (app.getJob() != null) {
                    jobTitle = app.getJob().getTitle();
                }
            }
        } catch (Exception ignored) {}

        LiveInterviewResultResponse response = new LiveInterviewResultResponse(
                r.getId(),
                interviewId,
                applicationId,
                candidateName,
                jobTitle,
                r.getTechnicalScore(),
                r.getCommunicationScore(),
                r.getProblemSolvingScore(),
                r.getAnswerRelevanceScore(),
                r.getCompletenessScore(),
                r.getOverallScore(),
                r.getStrengths(),
                r.getWeaknesses(),
                r.getImprovementTopics(),
                r.getRecommendation(),
                r.getSummary(),
                r.getTotalQuestions(),
                r.getAnsweredQuestions(),
                r.getSkippedQuestions(),
                r.getDurationMinutes(),
                r.getCompletedAt()
        );

        response.setIntegrityStatus(r.getIntegrityStatus() != null ? r.getIntegrityStatus() : "NORMAL");
        response.setTotalIntegrityEvents(r.getTotalIntegrityEvents() != null ? r.getTotalIntegrityEvents() : 0);
        response.setMultiplePersonEvents(r.getMultiplePersonEvents() != null ? r.getMultiplePersonEvents() : 0);
        response.setTabSwitchEvents(r.getTabSwitchEvents() != null ? r.getTabSwitchEvents() : 0);
        response.setAttentionAwayEvents(r.getAttentionAwayEvents() != null ? r.getAttentionAwayEvents() : 0);
        response.setAudioAnomalyEvents(r.getAudioAnomalyEvents() != null ? r.getAudioAnomalyEvents() : 0);

        return response;
    }

    public Long getResultId() {
        return resultId;
    }

    public void setResultId(Long resultId) {
        this.resultId = resultId;
    }

    public Long getInterviewId() {
        return interviewId;
    }

    public void setInterviewId(Long interviewId) {
        this.interviewId = interviewId;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
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
