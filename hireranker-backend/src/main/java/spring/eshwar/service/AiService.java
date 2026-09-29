package spring.eshwar.service;

public interface AiService {

    /**
     * Sends a prompt to the AI provider and returns the generated text response.
     *
     * @param prompt the text message or instruction for the AI model
     * @return the generated text content from the AI model
     */
    String generateResponse(String prompt);

    /**
     * Generates a conversational chat response tailored for candidates or recruiters.
     *
     * @param message the user query or chat message
     * @param role the user role ('CANDIDATE' or 'ADMIN')
     * @param userContext optional user or application context
     * @return the generated conversational response
     */
    default String generateChatResponse(String message, String role, String userContext) {
        return generateResponse(message);
    }
}
