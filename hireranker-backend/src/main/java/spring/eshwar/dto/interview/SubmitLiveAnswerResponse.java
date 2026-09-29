package spring.eshwar.dto.interview;

import java.util.Collections;
import java.util.List;

public class SubmitLiveAnswerResponse {

    private Long interviewId;
    private Long questionId;
    private String status;
    private String feedback;
    private Double technicalScore;
    private Double clarityScore;
    private Double completenessScore;
    private Double confidenceScore;
    private Double overallScore;
    private Boolean correct;
    private String correctnessClassification;
    private String explanation;
    private java.util.List<String> missingConcepts;
    private java.util.List<String> strengths;
    private java.util.List<String> improvements;
    private boolean interviewFinished;
    private LiveQuestionResponse nextQuestion;
    private Integer totalQuestionsTarget;

    public SubmitLiveAnswerResponse() {
    }

    public SubmitLiveAnswerResponse(Long interviewId, Long questionId, String status,
                                  String feedback, Double technicalScore, Double clarityScore,
                                  boolean interviewFinished, LiveQuestionResponse nextQuestion) {
        this(interviewId, questionId, status, feedback, technicalScore, clarityScore, true, "CORRECT", null, Collections.emptyList(), interviewFinished, nextQuestion);
    }

    public SubmitLiveAnswerResponse(Long interviewId, Long questionId, String status,
                                  String feedback, Double technicalScore, Double clarityScore,
                                  Boolean correct, String explanation, java.util.List<String> missingConcepts,
                                  boolean interviewFinished, LiveQuestionResponse nextQuestion) {
        this(interviewId, questionId, status, feedback, technicalScore, clarityScore, correct, (correct != null && correct ? "CORRECT" : "INCORRECT"), explanation, missingConcepts, interviewFinished, nextQuestion);
    }

    public SubmitLiveAnswerResponse(Long interviewId, Long questionId, String status,
                                  String feedback, Double technicalScore, Double clarityScore,
                                  Boolean correct, String correctnessClassification, String explanation, java.util.List<String> missingConcepts,
                                  boolean interviewFinished, LiveQuestionResponse nextQuestion) {
        this(interviewId, questionId, status, feedback, technicalScore, clarityScore, clarityScore,
                clarityScore != null ? clarityScore : 0.0,
                technicalScore != null && clarityScore != null
                        ? Math.round(((technicalScore * 0.45) + (clarityScore * 0.25) + (clarityScore * 0.15) + (clarityScore * 0.15)) * 10.0) / 10.0
                        : 0.0,
                correct, correctnessClassification, explanation, missingConcepts, Collections.emptyList(), Collections.emptyList(), interviewFinished, nextQuestion);
    }

    public SubmitLiveAnswerResponse(Long interviewId, Long questionId, String status,
                                  String feedback, Double technicalScore, Double clarityScore,
                                  Double completenessScore, Double confidenceScore, Double overallScore,
                                  Boolean correct, String correctnessClassification, String explanation,
                                  java.util.List<String> missingConcepts, java.util.List<String> strengths,
                                  java.util.List<String> improvements,
                                  boolean interviewFinished, LiveQuestionResponse nextQuestion) {
        this.interviewId = interviewId;
        this.questionId = questionId;
        this.status = status;
        this.feedback = feedback;
        this.technicalScore = technicalScore;
        this.clarityScore = clarityScore;
        this.completenessScore = completenessScore;
        this.confidenceScore = confidenceScore;
        this.overallScore = overallScore;
        this.correct = correct;
        this.correctnessClassification = correctnessClassification;
        this.explanation = explanation;
        this.missingConcepts = missingConcepts != null ? missingConcepts : Collections.emptyList();
        this.strengths = strengths != null ? strengths : Collections.emptyList();
        this.improvements = improvements != null ? improvements : Collections.emptyList();
        this.interviewFinished = interviewFinished;
        this.nextQuestion = nextQuestion;
    }

    public Long getInterviewId() {
        return interviewId;
    }

    public void setInterviewId(Long interviewId) {
        this.interviewId = interviewId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
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

    public boolean isInterviewFinished() {
        return interviewFinished;
    }

    public void setInterviewFinished(boolean interviewFinished) {
        this.interviewFinished = interviewFinished;
    }

    public LiveQuestionResponse getNextQuestion() {
        return nextQuestion;
    }

    public void setNextQuestion(LiveQuestionResponse nextQuestion) {
        this.nextQuestion = nextQuestion;
    }

    public Boolean getCorrect() {
        return correct;
    }

    public void setCorrect(Boolean correct) {
        this.correct = correct;
    }

    public String getCorrectnessClassification() {
        return correctnessClassification;
    }

    public void setCorrectnessClassification(String correctnessClassification) {
        this.correctnessClassification = correctnessClassification;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
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

    public List<String> getMissingConcepts() {
        return missingConcepts;
    }

    public void setMissingConcepts(List<String> missingConcepts) {
        this.missingConcepts = missingConcepts;
    }

    public List<String> getStrengths() {
        return strengths;
    }

    public void setStrengths(List<String> strengths) {
        this.strengths = strengths;
    }

    public List<String> getImprovements() {
        return improvements;
    }

    public void setImprovements(List<String> improvements) {
        this.improvements = improvements;
    }

    public Integer getTotalQuestionsTarget() {
        return totalQuestionsTarget;
    }

    public void setTotalQuestionsTarget(Integer totalQuestionsTarget) {
        this.totalQuestionsTarget = totalQuestionsTarget;
    }
}
