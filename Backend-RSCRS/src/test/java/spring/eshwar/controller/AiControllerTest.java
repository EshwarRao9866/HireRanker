package spring.eshwar.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import spring.eshwar.dto.ai.AiPromptRequest;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/ai/test - generates AI response successfully")
    void testAiTestEndpointSuccess() throws Exception {
        AiPromptRequest request = new AiPromptRequest("Generate one Java Spring Boot interview question.");

        mockMvc.perform(post("/api/ai/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").isNotEmpty())
                .andExpect(jsonPath("$.response", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/ai/test - blank message returns 400 Validation Error")
    void testAiTestEndpointBlankMessageValidation() throws Exception {
        AiPromptRequest request = new AiPromptRequest("   ");

        mockMvc.perform(post("/api/ai/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    @DisplayName("POST /api/ai/test - empty JSON object returns 400 Validation Error")
    void testAiTestEndpointEmptyBodyValidation() throws Exception {
        mockMvc.perform(post("/api/ai/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
