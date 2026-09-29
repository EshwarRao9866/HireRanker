package spring.eshwar.dto.interview;

public class SkipLiveQuestionRequest {

    private String reason; // e.g. "TIMEOUT_15S", "CANDIDATE_SKIPPED"
    private Double responseTimeSeconds; // e.g. 15.0

    public SkipLiveQuestionRequest() {
    }

    public SkipLiveQuestionRequest(String reason, Double responseTimeSeconds) {
        this.reason = reason;
        this.responseTimeSeconds = responseTimeSeconds;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Double getResponseTimeSeconds() {
        return responseTimeSeconds;
    }

    public void setResponseTimeSeconds(Double responseTimeSeconds) {
        this.responseTimeSeconds = responseTimeSeconds;
    }
}
