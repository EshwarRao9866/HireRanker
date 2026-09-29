package spring.eshwar.dto.interview;

import jakarta.validation.constraints.NotNull;

public class StartInterviewRequest {

    private Long applicationId;
    private Long interviewId;

    public StartInterviewRequest() {
    }

    public StartInterviewRequest(Long applicationId) {
        this.applicationId = applicationId;
    }

    public StartInterviewRequest(Long applicationId, Long interviewId) {
        this.applicationId = applicationId;
        this.interviewId = interviewId;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public Long getInterviewId() {
        return interviewId;
    }

    public void setInterviewId(Long interviewId) {
        this.interviewId = interviewId;
    }
}
