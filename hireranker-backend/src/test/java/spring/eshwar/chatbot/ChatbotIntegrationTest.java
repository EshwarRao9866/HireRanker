package spring.eshwar.chatbot;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import spring.eshwar.dto.chatbot.ChatbotRequest;
import spring.eshwar.dto.chatbot.ChatbotResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ChatbotIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/chatbot/message - starts new conversation and responds accurately")
    void testSendMessageNewConversation() throws Exception {
        ChatbotRequest request = new ChatbotRequest("How do I upload a resume?");

        MvcResult result = mockMvc.perform(post("/api/chatbot/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId", notNullValue()))
                .andExpect(jsonPath("$.response", notNullValue()))
                .andReturn();

        ChatbotResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ChatbotResponse.class
        );

        assertThat(response.getConversationId()).isNotBlank();
        assertThat(response.getResponse().toLowerCase()).contains("resume");
    }

    @Test
    @DisplayName("Multi-turn context: Follow-up question preserves context")
    void testConversationContextFollowUp() throws Exception {
        // Turn 1: Ask about resume upload
        ChatbotRequest turn1Req = new ChatbotRequest("How do I upload a resume?");

        MvcResult turn1Result = mockMvc.perform(post("/api/chatbot/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(turn1Req)))
                .andExpect(status().isOk())
                .andReturn();

        ChatbotResponse turn1Res = objectMapper.readValue(
                turn1Result.getResponse().getContentAsString(),
                ChatbotResponse.class
        );

        String conversationId = turn1Res.getConversationId();
        assertThat(conversationId).isNotBlank();

        // Turn 2: Follow-up asking "What file format is supported?"
        ChatbotRequest turn2Req = new ChatbotRequest(conversationId, "What file format is supported?");

        MvcResult turn2Result = mockMvc.perform(post("/api/chatbot/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(turn2Req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value(conversationId))
                .andReturn();

        ChatbotResponse turn2Res = objectMapper.readValue(
                turn2Result.getResponse().getContentAsString(),
                ChatbotResponse.class
        );

        assertThat(turn2Res.getResponse().toLowerCase()).contains("pdf");
    }

    @Test
    @DisplayName("POST /api/chatbot/message - troubleshooting resume upload errors")
    void testTroubleshootingQuery() throws Exception {
        ChatbotRequest request = new ChatbotRequest("Why can't I upload my resume?");

        MvcResult result = mockMvc.perform(post("/api/chatbot/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        ChatbotResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ChatbotResponse.class
        );

        assertThat(response.getResponse().toLowerCase()).contains("pdf");
    }

    @Test
    @DisplayName("POST /api/chatbot/message - 15-second interview rule inquiry")
    void testInterview15SecondRuleQuery() throws Exception {
        ChatbotRequest request = new ChatbotRequest("How does the 15 second interview rule work?");

        MvcResult result = mockMvc.perform(post("/api/chatbot/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        ChatbotResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ChatbotResponse.class
        );

        assertThat(response.getResponse().toLowerCase()).contains("15");
    }

    @Test
    @DisplayName("POST /api/chatbot/message - blank message validation error")
    void testBlankMessageValidation() throws Exception {
        ChatbotRequest request = new ChatbotRequest("   ");

        mockMvc.perform(post("/api/chatbot/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    @DisplayName("POST /api/chatbot/reset - resets conversation session")
    void testResetConversation() throws Exception {
        mockMvc.perform(post("/api/chatbot/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("conversationId", "test-session-123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("Verify 5 distinct required questions return unique, relevant, non-identical answers")
    void testFiveDistinctRequiredQuestions() throws Exception {
        String[] questions = new String[]{
                "How do I upload a resume?",
                "How does candidate ranking work?",
                "Give me interview preparation tips.",
                "Why is resume screening failing?",
                "What happens after an AI interview?"
        };

        String[] responses = new String[questions.length];

        for (int i = 0; i < questions.length; i++) {
            ChatbotRequest req = new ChatbotRequest(questions[i]);
            req.setRole("CANDIDATE");

            MvcResult res = mockMvc.perform(post("/api/chatbot/message")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andReturn();

            ChatbotResponse body = objectMapper.readValue(res.getResponse().getContentAsString(), ChatbotResponse.class);
            assertThat(body.getResponse()).isNotBlank();
            responses[i] = body.getResponse();
        }

        // Verify each answer is distinct and not repeating
        for (int i = 0; i < responses.length; i++) {
            for (int j = i + 1; j < responses.length; j++) {
                assertThat(responses[i])
                        .as("Responses for question %d and %d must be distinct", i + 1, j + 1)
                        .isNotEqualTo(responses[j]);
            }
        }

        // Specific topical assertions
        assertThat(responses[0].toLowerCase()).contains("resume"); // upload
        assertThat(responses[1].toLowerCase()).contains("rank"); // ranking
        assertThat(responses[2].toLowerCase()).contains("interview"); // prep tips
        assertThat(responses[3].toLowerCase()).contains("screening"); // screening fail
        assertThat(responses[4].toLowerCase()).contains("scorecard"); // after interview
    }

    @Test
    @DisplayName("Verify role-aware behavior: Candidate vs Admin questions return role-appropriate answers")
    void testRoleAwareQuestions() throws Exception {
        // Candidate asks about ranking
        ChatbotRequest candidateReq = new ChatbotRequest("How does candidate ranking work?");
        candidateReq.setRole("CANDIDATE");
        MvcResult candRes = mockMvc.perform(post("/api/chatbot/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(candidateReq)))
                .andExpect(status().isOk())
                .andReturn();
        ChatbotResponse candBody = objectMapper.readValue(candRes.getResponse().getContentAsString(), ChatbotResponse.class);

        // Admin asks about ranking
        ChatbotRequest adminReq = new ChatbotRequest("How does candidate ranking work?");
        adminReq.setRole("ADMIN");
        MvcResult adminRes = mockMvc.perform(post("/api/chatbot/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminReq)))
                .andExpect(status().isOk())
                .andReturn();
        ChatbotResponse adminBody = objectMapper.readValue(adminRes.getResponse().getContentAsString(), ChatbotResponse.class);

        // Both return valid, distinct role-tailored responses
        assertThat(candBody.getResponse()).isNotBlank();
        assertThat(adminBody.getResponse()).isNotBlank();
        assertThat(candBody.getResponse()).contains("Top Listed");
        assertThat(adminBody.getResponse()).contains("Evaluation Criteria");

        // Admin asks how to create a job
        ChatbotRequest createJobReq = new ChatbotRequest("How do I create a job?");
        createJobReq.setRole("ADMIN");
        MvcResult createJobRes = mockMvc.perform(post("/api/chatbot/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createJobReq)))
                .andExpect(status().isOk())
                .andReturn();
        ChatbotResponse createJobBody = objectMapper.readValue(createJobRes.getResponse().getContentAsString(), ChatbotResponse.class);
        assertThat(createJobBody.getResponse()).contains("Create Job");
    }
}
