package spring.eshwar.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import spring.eshwar.dto.auth.LoginRequest;
import spring.eshwar.dto.auth.RegisterRequest;
import spring.eshwar.dto.message.MessageRequest;
import spring.eshwar.entity.Role;
import spring.eshwar.entity.User;
import spring.eshwar.repository.MessageRepository;
import spring.eshwar.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MessagingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageRepository messageRepository;

    private Long user1Id;
    private String user1Token;

    private Long user2Id;
    private String user2Token;

    private Long user3Id;
    private String user3Token;

    private Long sharedMessageId;
    private final List<Long> createdMessageIds = new ArrayList<>();

    @BeforeAll
    void setUp() throws Exception {
        // Create User 1 (Admin)
        String user1Email = "user1.msg." + System.currentTimeMillis() + "@hireranker.com";
        user1Id = registerAndGetId("Alice Recruiter", user1Email, "AlicePass123!", Role.ADMIN);
        user1Token = loginAndGetToken(user1Email, "AlicePass123!");

        // Create User 2 (Candidate)
        String user2Email = "user2.msg." + System.currentTimeMillis() + "@hireranker.com";
        user2Id = registerAndGetId("Bob Candidate", user2Email, "BobPass123!", Role.CANDIDATE);
        user2Token = loginAndGetToken(user2Email, "BobPass123!");

        // Create User 3 (Candidate 2 / Third party)
        String user3Email = "user3.msg." + System.currentTimeMillis() + "@hireranker.com";
        user3Id = registerAndGetId("Charlie Outsider", user3Email, "CharliePass123!", Role.CANDIDATE);
        user3Token = loginAndGetToken(user3Email, "CharliePass123!");
    }

    @AfterAll
    void tearDown() {
        for (Long msgId : createdMessageIds) {
            try {
                if (messageRepository.existsById(msgId)) {
                    messageRepository.deleteById(msgId);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private Long registerAndGetId(String name, String email, String password, Role role) throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setName(name);
        req.setEmail(email);
        req.setPassword(password);
        req.setRole(role);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        User user = userRepository.findByEmail(email).orElseThrow();
        return user.getId();
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        LoginRequest req = new LoginRequest(email, password);
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    @Order(1)
    @DisplayName("1. User 1 successfully sends a message to User 2 (auto sentAt, UNREAD, DTO)")
    void testSendMessageValid() throws Exception {
        MessageRequest req = new MessageRequest();
        req.setReceiverId(user2Id);
        req.setMessage("Hello Bob, your profile looks impressive! When are you free for a call?");

        MvcResult result = mockMvc.perform(post("/api/messages")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.senderId").value(user1Id))
                .andExpect(jsonPath("$.senderName").value("Alice Recruiter"))
                .andExpect(jsonPath("$.receiverId").value(user2Id))
                .andExpect(jsonPath("$.receiverName").value("Bob Candidate"))
                .andExpect(jsonPath("$.message").value("Hello Bob, your profile looks impressive! When are you free for a call?"))
                .andExpect(jsonPath("$.readStatus").value("UNREAD"))
                .andExpect(jsonPath("$.sentAt").isNotEmpty())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        sharedMessageId = json.get("id").asLong();
        createdMessageIds.add(sharedMessageId);
        assertThat(sharedMessageId).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("2. Sender impersonation prevention: User 2 cannot send message with User 1's senderId")
    void testSenderImpersonationRejected() throws Exception {
        MessageRequest req = new MessageRequest();
        req.setSenderId(user1Id); // Impersonating Alice
        req.setReceiverId(user3Id);
        req.setMessage("This is a forged message from Alice!");

        mockMvc.perform(post("/api/messages")
                        .header("Authorization", "Bearer " + user2Token) // Bob is logged in
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(3)
    @DisplayName("3. Self-messaging rejection: User cannot message themselves")
    void testSelfMessagingRejected() throws Exception {
        MessageRequest req = new MessageRequest();
        req.setReceiverId(user1Id); // Alice sending to Alice
        req.setMessage("Note to self: review resumes tomorrow.");

        mockMvc.perform(post("/api/messages")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(4)
    @DisplayName("4. Non-existent receiver rejection: 404 Not Found")
    void testNonExistentReceiverRejected() throws Exception {
        MessageRequest req = new MessageRequest();
        req.setReceiverId(999999L);
        req.setMessage("Hello to the void!");

        mockMvc.perform(post("/api/messages")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(5)
    @DisplayName("5. Message length validation: Blank or oversized message rejected with 400 Bad Request")
    void testMessageLengthValidation() throws Exception {
        // Blank message
        MessageRequest blankReq = new MessageRequest();
        blankReq.setReceiverId(user2Id);
        blankReq.setMessage("   ");

        mockMvc.perform(post("/api/messages")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankReq)))
                .andExpect(status().isBadRequest());

        // Oversized message (> 2000 chars)
        String longMessage = "A".repeat(2001);
        MessageRequest longReq = new MessageRequest();
        longReq.setReceiverId(user2Id);
        longReq.setMessage(longMessage);

        mockMvc.perform(post("/api/messages")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(longReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(6)
    @DisplayName("6. GET /api/messages returns all messages for authenticated user (isolated from third parties)")
    void testGetMyMessages() throws Exception {
        // Alice views her messages
        mockMvc.perform(get("/api/messages")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.id == " + sharedMessageId + ")]").exists());

        // Bob views his messages
        mockMvc.perform(get("/api/messages")
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.id == " + sharedMessageId + ")]").exists());

        // Charlie views his messages (should NOT see the message between Alice and Bob)
        mockMvc.perform(get("/api/messages")
                        .header("Authorization", "Bearer " + user3Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + sharedMessageId + ")]").doesNotExist());
    }

    @Test
    @Order(7)
    @DisplayName("7. GET /api/messages/{messageId} accessible to sender and receiver")
    void testGetMessageByIdAuthorized() throws Exception {
        // Sender (Alice) can access
        mockMvc.perform(get("/api/messages/" + sharedMessageId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sharedMessageId))
                .andExpect(jsonPath("$.message").value("Hello Bob, your profile looks impressive! When are you free for a call?"));

        // Receiver (Bob) can access
        mockMvc.perform(get("/api/messages/" + sharedMessageId)
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sharedMessageId));
    }

    @Test
    @Order(8)
    @DisplayName("8. GET /api/messages/{messageId} forbidden for unauthorized third party (Charlie)")
    void testGetMessageByIdForbiddenForThirdParty() throws Exception {
        mockMvc.perform(get("/api/messages/" + sharedMessageId)
                        .header("Authorization", "Bearer " + user3Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(9)
    @DisplayName("9. PUT /api/messages/{messageId}/read rejected if caller is not the receiver")
    void testMarkAsReadForbiddenForNonReceiver() throws Exception {
        // Charlie (third party) tries to mark as read -> 403 Forbidden
        mockMvc.perform(put("/api/messages/" + sharedMessageId + "/read")
                        .header("Authorization", "Bearer " + user3Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(10)
    @DisplayName("10. PUT /api/messages/{messageId}/read succeeds when called by receiver (Bob)")
    void testMarkAsReadAuthorized() throws Exception {
        mockMvc.perform(put("/api/messages/" + sharedMessageId + "/read")
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sharedMessageId))
                .andExpect(jsonPath("$.readStatus").value("READ"));
    }

    @Test
    @Order(11)
    @DisplayName("11. Unauthenticated requests to /api/messages endpoints return 401 Unauthorized")
    void testUnauthenticatedAccessRejected() throws Exception {
        mockMvc.perform(get("/api/messages"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverId\": 1, \"message\": \"Hi\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/messages/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/messages/1/read"))
                .andExpect(status().isUnauthorized());
    }
}
