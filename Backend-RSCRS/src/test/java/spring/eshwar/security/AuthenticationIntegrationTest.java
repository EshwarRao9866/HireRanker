package spring.eshwar.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import spring.eshwar.dto.LoginRequest;
import spring.eshwar.dto.RegisterRequest;
import spring.eshwar.entity.Role;
import spring.eshwar.repository.CandidateRepository;
import spring.eshwar.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    private static String candidateToken;
    private static String adminToken;

    @Test
    @Order(1)
    @DisplayName("1. Valid registration - successfully creates user, profile, and returns JWT")
    void test01_ValidRegistration() throws Exception {
        RegisterRequest candidateReq = new RegisterRequest();
        candidateReq.setName("John Candidate");
        candidateReq.setEmail("john.candidate@example.com");
        candidateReq.setPassword("password123");
        candidateReq.setRole(Role.CANDIDATE);

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(candidateReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.email").value("john.candidate@example.com"))
                .andExpect(jsonPath("$.role").value("CANDIDATE"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        candidateToken = root.get("token").asText();
        assertThat(candidateToken).isNotBlank();

        assertThat(userRepository.existsByEmail("john.candidate@example.com")).isTrue();
        assertThat(candidateRepository.findByUserEmail("john.candidate@example.com")).isPresent();

        // Also register admin for role verification
        RegisterRequest adminReq = new RegisterRequest();
        adminReq.setName("System Admin");
        adminReq.setEmail("admin@example.com");
        adminReq.setPassword("adminpass123");
        adminReq.setRole(Role.ADMIN);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @Order(2)
    @DisplayName("2. Duplicate registration - rejects duplicate email with 400 Bad Request")
    void test02_DuplicateRegistration() throws Exception {
        RegisterRequest duplicateReq = new RegisterRequest();
        duplicateReq.setName("John Clone");
        duplicateReq.setEmail("john.candidate@example.com");
        duplicateReq.setPassword("anotherpass123");
        duplicateReq.setRole(Role.CANDIDATE);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("An account with email john.candidate@example.com already exists."));
    }

    @Test
    @Order(3)
    @DisplayName("3. Valid login - authenticates credentials and returns JWT")
    void test03_ValidLogin() throws Exception {
        // Candidate login
        LoginRequest candidateLogin = new LoginRequest();
        candidateLogin.setEmail("john.candidate@example.com");
        candidateLogin.setPassword("password123");

        MvcResult candResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(candidateLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("CANDIDATE"))
                .andReturn();

        JsonNode candRoot = objectMapper.readTree(candResult.getResponse().getContentAsString());
        candidateToken = candRoot.get("token").asText();
        assertThat(candidateToken).isNotBlank();

        // Admin login
        LoginRequest adminLogin = new LoginRequest();
        adminLogin.setEmail("admin@example.com");
        adminLogin.setPassword("adminpass123");

        MvcResult adminResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn();

        JsonNode adminRoot = objectMapper.readTree(adminResult.getResponse().getContentAsString());
        adminToken = adminRoot.get("token").asText();
        assertThat(adminToken).isNotBlank();
    }

    @Test
    @Order(4)
    @DisplayName("4. Wrong password - rejects invalid credentials with 401 Unauthorized")
    void test04_WrongPassword() throws Exception {
        LoginRequest badLogin = new LoginRequest();
        badLogin.setEmail("john.candidate@example.com");
        badLogin.setPassword("completely_wrong_password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    @Test
    @Order(5)
    @DisplayName("5. Missing JWT - unauthenticated access to protected endpoint is rejected with 401 Unauthorized")
    void test05_MissingJwt() throws Exception {
        mockMvc.perform(get("/api/candidates/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(6)
    @DisplayName("6. Invalid JWT - malformed token is rejected with 401 Unauthorized")
    void test06_InvalidJwt() throws Exception {
        mockMvc.perform(get("/api/candidates/me")
                        .header("Authorization", "Bearer invalid.malformed.jwt.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(7)
    @DisplayName("7. Expired JWT - expired token is rejected with 401 Unauthorized")
    void test07_ExpiredJwt() throws Exception {
        UserDetails userDetails = userDetailsService.loadUserByUsername("john.candidate@example.com");
        // Generate an expired token (expiration in the past: -10,000 milliseconds)
        String expiredToken = jwtService.generateToken(userDetails, -10000L);

        mockMvc.perform(get("/api/candidates/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(8)
    @DisplayName("8. ADMIN access - admin token successfully accesses admin-only endpoints")
    void test08_AdminAccess() throws Exception {
        assertThat(adminToken).isNotBlank();

        mockMvc.perform(get("/api/auth/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/candidates")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(9)
    @DisplayName("9. CANDIDATE access - candidate token successfully accesses candidate profile")
    void test09_CandidateAccess() throws Exception {
        assertThat(candidateToken).isNotBlank();

        mockMvc.perform(get("/api/candidates/me")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("John Candidate"));
    }

    @Test
    @Order(10)
    @DisplayName("10. Forbidden role access - candidate token is blocked from admin endpoints with 403 Forbidden")
    void test10_ForbiddenRoleAccess() throws Exception {
        assertThat(candidateToken).isNotBlank();

        // Candidate attempting to access admin-only users list
        mockMvc.perform(get("/api/auth/users")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden());

        // Candidate attempting to access admin-only all candidates list
        mockMvc.perform(get("/api/candidates")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden());
    }
}
