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
import spring.eshwar.dto.candidate.CandidateProfileUpdateRequest;
import spring.eshwar.dto.candidate.CandidateRequest;
import spring.eshwar.entity.Candidate;
import spring.eshwar.entity.Role;
import spring.eshwar.entity.User;
import spring.eshwar.repository.CandidateRepository;
import spring.eshwar.repository.UserRepository;

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
public class UserProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    private String candidate1Token;
    private Long candidate1UserId;
    private Long candidate1Id;

    private String candidate2Token;
    private Long candidate2UserId;
    private Long candidate2Id;

    private String adminToken;
    private Long adminUserId;

    @BeforeAll
    void setupTestData() throws Exception {
        userRepository.findByEmail("alice.prof@test.com").ifPresent(u -> {
            candidateRepository.findByUserId(u.getId()).ifPresent(c -> candidateRepository.deleteById(c.getId()));
            userRepository.deleteById(u.getId());
        });
        userRepository.findByEmail("bob.prof@test.com").ifPresent(u -> {
            candidateRepository.findByUserId(u.getId()).ifPresent(c -> candidateRepository.deleteById(c.getId()));
            userRepository.deleteById(u.getId());
        });
        userRepository.findByEmail("admin.prof@test.com").ifPresent(u -> userRepository.deleteById(u.getId()));

        // Register Candidate 1
        RegisterRequest cand1Req = new RegisterRequest();
        cand1Req.setName("Alice Candidate");
        cand1Req.setEmail("alice.prof@test.com");
        cand1Req.setPassword("Password123!");
        cand1Req.setRole(Role.CANDIDATE);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cand1Req)))
                .andExpect(status().isCreated());

        LoginRequest login1 = new LoginRequest();
        login1.setEmail("alice.prof@test.com");
        login1.setPassword("Password123!");

        MvcResult login1Res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login1)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode login1Json = objectMapper.readTree(login1Res.getResponse().getContentAsString());
        candidate1Token = login1Json.get("token").asText();
        candidate1UserId = login1Json.get("id").asLong();

        Candidate cand1 = candidateRepository.findByUserId(candidate1UserId).orElseThrow();
        cand1.setPhone("+1-555-0101");
        cand1.setLocation("Austin, TX");
        cand1.setSkills("Java, Spring Boot");
        cand1.setExperience("3 years");
        cand1.setEducation("B.S. Computer Science");
        cand1.setGithub("https://github.com/alice");
        cand1.setLinkedin("https://linkedin.com/in/alice");
        candidateRepository.save(cand1);
        candidate1Id = cand1.getId();

        // Register Candidate 2
        RegisterRequest cand2Req = new RegisterRequest();
        cand2Req.setName("Bob Candidate");
        cand2Req.setEmail("bob.prof@test.com");
        cand2Req.setPassword("Password123!");
        cand2Req.setRole(Role.CANDIDATE);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cand2Req)))
                .andExpect(status().isCreated());

        LoginRequest login2 = new LoginRequest();
        login2.setEmail("bob.prof@test.com");
        login2.setPassword("Password123!");

        MvcResult login2Res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login2)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode login2Json = objectMapper.readTree(login2Res.getResponse().getContentAsString());
        candidate2Token = login2Json.get("token").asText();
        candidate2UserId = login2Json.get("id").asLong();
        Candidate cand2 = candidateRepository.findByUserId(candidate2UserId).orElseThrow();
        candidate2Id = cand2.getId();

        // Register Admin
        RegisterRequest adminReq = new RegisterRequest();
        adminReq.setName("Admin Officer");
        adminReq.setEmail("admin.prof@test.com");
        adminReq.setPassword("AdminPass123!");
        adminReq.setRole(Role.ADMIN);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminReq)))
                .andExpect(status().isCreated());

        LoginRequest adminLogin = new LoginRequest();
        adminLogin.setEmail("admin.prof@test.com");
        adminLogin.setPassword("AdminPass123!");

        MvcResult adminLoginRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode adminJson = objectMapper.readTree(adminLoginRes.getResponse().getContentAsString());
        adminToken = adminJson.get("token").asText();
        adminUserId = adminJson.get("id").asLong();
    }

    @AfterAll
    void cleanUp() {
        if (candidate1Id != null) candidateRepository.deleteById(candidate1Id);
        if (candidate2Id != null) candidateRepository.deleteById(candidate2Id);
        if (candidate1UserId != null) userRepository.deleteById(candidate1UserId);
        if (candidate2UserId != null) userRepository.deleteById(candidate2UserId);
        if (adminUserId != null) userRepository.deleteById(adminUserId);
    }

    @Test
    @Order(1)
    @DisplayName("1. Authenticated candidate retrieves own profile via GET /api/candidates/me")
    void testCandidateCanGetOwnCandidateProfile() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/candidates/me")
                        .header("Authorization", "Bearer " + candidate1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(candidate1Id))
                .andExpect(jsonPath("$.userId").value(candidate1UserId))
                .andExpect(jsonPath("$.email").value("alice.prof@test.com"))
                .andExpect(jsonPath("$.fullName").value("Alice Candidate"))
                .andExpect(jsonPath("$.phone").value("+1-555-0101"))
                .andExpect(jsonPath("$.location").value("Austin, TX"))
                .andExpect(jsonPath("$.skills").value("Java, Spring Boot"))
                .andExpect(jsonPath("$.experience").value("3 years"))
                .andExpect(jsonPath("$.education").value("B.S. Computer Science"))
                .andExpect(jsonPath("$.github").value("https://github.com/alice"))
                .andExpect(jsonPath("$.linkedin").value("https://linkedin.com/in/alice"))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.has("password")).isFalse();
    }

    @Test
    @Order(2)
    @DisplayName("2. Candidate updates profile via PUT /api/candidates/me")
    void testCandidateCanUpdateOwnCandidateProfile() throws Exception {
        CandidateProfileUpdateRequest updateReq = new CandidateProfileUpdateRequest(
                "Alice Candidate Updated",
                "+1-555-9999",
                "San Francisco, CA",
                "Java 21, Spring Boot 3, Angular 17, Docker",
                "5 years",
                "M.S. Software Systems",
                "https://github.com/alice-updated",
                "https://linkedin.com/in/alice-updated"
        );

        MvcResult result = mockMvc.perform(put("/api/candidates/me")
                        .header("Authorization", "Bearer " + candidate1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Alice Candidate Updated"))
                .andExpect(jsonPath("$.phone").value("+1-555-9999"))
                .andExpect(jsonPath("$.location").value("San Francisco, CA"))
                .andExpect(jsonPath("$.skills").value("Java 21, Spring Boot 3, Angular 17, Docker"))
                .andExpect(jsonPath("$.experience").value("5 years"))
                .andExpect(jsonPath("$.education").value("M.S. Software Systems"))
                .andExpect(jsonPath("$.github").value("https://github.com/alice-updated"))
                .andExpect(jsonPath("$.linkedin").value("https://linkedin.com/in/alice-updated"))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.has("password")).isFalse();

        // Verify User entity name was also synchronized
        User user = userRepository.findById(candidate1UserId).orElseThrow();
        assertThat(user.getName()).isEqualTo("Alice Candidate Updated");
    }

    @Test
    @Order(3)
    @DisplayName("3. Attempting to modify role through candidate profile API is completely ignored")
    void testRoleCannotBeModifiedViaCandidateProfile() throws Exception {
        String maliciousPayload = "{"
                + "\"fullName\": \"Alice Still Candidate\","
                + "\"role\": \"ADMIN\","
                + "\"password\": \"HackedPass!\""
                + "}";

        mockMvc.perform(put("/api/candidates/me")
                        .header("Authorization", "Bearer " + candidate1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(maliciousPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Alice Still Candidate"));

        // Verify database user role remained CANDIDATE
        User user = userRepository.findById(candidate1UserId).orElseThrow();
        assertThat(user.getRole()).isEqualTo(Role.CANDIDATE);
    }

    @Test
    @Order(4)
    @DisplayName("4. Candidate retrieves their own user profile via GET /api/users/me (no password returned)")
    void testCandidateCanGetOwnUserProfile() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + candidate1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(candidate1UserId))
                .andExpect(jsonPath("$.email").value("alice.prof@test.com"))
                .andExpect(jsonPath("$.role").value("CANDIDATE"))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.has("password")).isFalse();
    }

    @Test
    @Order(5)
    @DisplayName("5. Admin retrieves their own user profile via GET /api/users/me (no password returned)")
    void testAdminCanGetOwnUserProfile() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(adminUserId))
                .andExpect(jsonPath("$.email").value("admin.prof@test.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.has("password")).isFalse();
    }

    @Test
    @Order(6)
    @DisplayName("6. Admin is forbidden from calling candidate-only GET /api/candidates/me")
    void testAdminCannotAccessCandidateProfileEndpoint() throws Exception {
        mockMvc.perform(get("/api/candidates/me")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(7)
    @DisplayName("7. Candidate cannot access or modify another candidate's profile by ID")
    void testCandidateCannotAccessOtherCandidateById() throws Exception {
        // Candidate 1 attempts to view Candidate 2 by ID -> 403
        mockMvc.perform(get("/api/candidates/" + candidate2Id)
                        .header("Authorization", "Bearer " + candidate1Token))
                .andExpect(status().isForbidden());

        // Candidate 1 attempts to update Candidate 2 by ID -> 403
        CandidateRequest updateReq = new CandidateRequest();
        updateReq.setUserId(candidate2UserId);
        updateReq.setFullName("Malicious Modification");
        mockMvc.perform(put("/api/candidates/" + candidate2Id)
                        .header("Authorization", "Bearer " + candidate1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(8)
    @DisplayName("8. Candidate cannot inspect other users' info or list all users via /api/users")
    void testCandidateCannotAccessOtherUsersOrListUsers() throws Exception {
        // Candidate 1 attempts to query Candidate 2's user info by ID -> 403
        mockMvc.perform(get("/api/users/" + candidate2UserId)
                        .header("Authorization", "Bearer " + candidate1Token))
                .andExpect(status().isForbidden());

        // Candidate 1 attempts to list all users -> 403
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + candidate1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(9)
    @DisplayName("9. Admin can access user info by ID and list all users via /api/users")
    void testAdminCanAccessUserByIdAndListAllUsers() throws Exception {
        // Admin gets candidate 1 user info by ID
        MvcResult res1 = mockMvc.perform(get("/api/users/" + candidate1UserId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(candidate1UserId))
                .andExpect(jsonPath("$.email").value("alice.prof@test.com"))
                .andReturn();
        assertThat(objectMapper.readTree(res1.getResponse().getContentAsString()).has("password")).isFalse();

        // Admin lists all users
        MvcResult res2 = mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode usersArray = objectMapper.readTree(res2.getResponse().getContentAsString());
        assertThat(usersArray.isArray()).isTrue();
        assertThat(usersArray.size()).isGreaterThanOrEqualTo(3);
    }

    @Test
    @Order(10)
    @DisplayName("10. Validation errors on candidate profile update return 400 Bad Request")
    void testValidationFailuresOnCandidateProfileUpdate() throws Exception {
        // Blank fullName
        CandidateProfileUpdateRequest blankNameReq = new CandidateProfileUpdateRequest();
        blankNameReq.setFullName("   ");
        mockMvc.perform(put("/api/candidates/me")
                        .header("Authorization", "Bearer " + candidate1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankNameReq)))
                .andExpect(status().isBadRequest());

        // Invalid phone number format
        CandidateProfileUpdateRequest invalidPhoneReq = new CandidateProfileUpdateRequest();
        invalidPhoneReq.setFullName("Valid Name");
        invalidPhoneReq.setPhone("invalid-letters-in-phone");
        mockMvc.perform(put("/api/candidates/me")
                        .header("Authorization", "Bearer " + candidate1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPhoneReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(11)
    @DisplayName("11. Unauthenticated requests are rejected with 401 Unauthorized")
    void testUnauthenticatedAccessRejected() throws Exception {
        mockMvc.perform(get("/api/candidates/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/candidates/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }
}
