package spring.eshwar.dto.interview;

import spring.eshwar.entity.InterviewQuestion;

public class LiveQuestionResponse {

    private Long questionId;
    private Long interviewId;
    private int questionNumber;
    private String questionText;
    private String category;
    private String difficulty;
    private Integer timeLimitSeconds;
    private String status;

    public LiveQuestionResponse() {
    }

    public LiveQuestionResponse(Long questionId, Long interviewId, int questionNumber,
                                String questionText, String category, String difficulty,
                                Integer timeLimitSeconds, String status) {
        this.questionId = questionId;
        this.interviewId = interviewId;
        this.questionNumber = questionNumber;
        this.questionText = questionText;
        this.category = category;
        this.difficulty = difficulty;
        this.timeLimitSeconds = timeLimitSeconds;
        this.status = status;
    }

    public static LiveQuestionResponse fromEntity(InterviewQuestion q) {
        if (q == null) {
            return null;
        }
        Long interviewId = (q.getInterview() != null) ? q.getInterview().getId() : null;
        return new LiveQuestionResponse(
                q.getId(),
                interviewId,
                q.getQuestionOrder(),
                q.getQuestionText(),
                q.getCategory(),
                q.getDifficulty(),
                q.getTimeLimitSeconds(),
                q.getStatus() != null ? q.getStatus().name() : null
        );
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public Long getInterviewId() {
        return interviewId;
    }

    public void setInterviewId(Long interviewId) {
        this.interviewId = interviewId;
    }

    public int getQuestionNumber() {
        return questionNumber;
    }

    public void setQuestionNumber(int questionNumber) {
        this.questionNumber = questionNumber;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public Integer getTimeLimitSeconds() {
        return timeLimitSeconds;
    }

    public void setTimeLimitSeconds(Integer timeLimitSeconds) {
        this.timeLimitSeconds = timeLimitSeconds;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
