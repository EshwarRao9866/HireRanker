package spring.eshwar.dto.interview;

public class InterviewTranscriptionResponse {

    private boolean success;
    private String text;
    private boolean isFinal;
    private String provider;
    private String model;
    private long latencyMs;
    private String error;

    public InterviewTranscriptionResponse() {
    }

    public InterviewTranscriptionResponse(boolean success, String text, boolean isFinal, String provider, String model, long latencyMs) {
        this.success = success;
        this.text = text;
        this.isFinal = isFinal;
        this.provider = provider;
        this.model = model;
        this.latencyMs = latencyMs;
    }

    public static InterviewTranscriptionResponse success(String text, boolean isFinal, String provider, String model, long latencyMs) {
        return new InterviewTranscriptionResponse(true, text, isFinal, provider, model, latencyMs);
    }

    public static InterviewTranscriptionResponse failure(String error, String provider, String model) {
        InterviewTranscriptionResponse resp = new InterviewTranscriptionResponse(false, "", true, provider, model, 0);
        resp.setError(error);
        return resp;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public void setFinal(boolean aFinal) {
        isFinal = aFinal;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
