package spring.eshwar.dto.ai;

public class AiPromptResponse {

    private String response;

    public AiPromptResponse() {
    }

    public AiPromptResponse(String response) {
        this.response = response;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}
