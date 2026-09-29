package spring.eshwar.service.auth;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import spring.eshwar.dto.auth.ForgotPasswordRequest;
import spring.eshwar.dto.auth.ResetPasswordRequest;
import spring.eshwar.entity.PasswordResetToken;
import spring.eshwar.entity.Role;
import spring.eshwar.entity.User;
import spring.eshwar.repository.PasswordResetTokenRepository;
import spring.eshwar.repository.UserRepository;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PasswordResetIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @Order(1)
    @DisplayName("Forgot password for candidate sends reset link and token can reset password")
    void testCandidateForgotPasswordAndResetFlow() throws Exception {
        String email = "candidate.reset.test@example.com";
        User candidateUser = userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User("Alice Candidate", email, passwordEncoder.encode("OldPassword123!"), Role.CANDIDATE);
            return userRepository.save(u);
        });

        // 1. Request forgot password
        ForgotPasswordRequest forgotReq = new ForgotPasswordRequest(email);
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("If an account exists for this email, a password reset link has been sent."));

        // 2. Verify token was created in DB for user
        PasswordResetToken resetToken = tokenRepository.findTopByUserOrderByCreatedAtDesc(candidateUser).orElseThrow();
        assertThat(resetToken.isUsed()).isFalse();
        assertThat(resetToken.getExpiresAt()).isAfter(LocalDateTime.now());

        // 3. Emulate user with raw token.
        // We know our token hash matches sha256(rawToken). Let's craft a known rawToken & hash for direct verification
        String testRawToken = "test-raw-candidate-token-123456789";
        resetToken.setTokenHash(hashToken(testRawToken));
        tokenRepository.save(resetToken);

        // 4. Perform password reset with new password
        ResetPasswordRequest resetReq = new ResetPasswordRequest(testRawToken, "NewPassword123!");
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password reset successful. You can now log in."));

        // 5. Verify database password was updated and role was preserved
        User updatedUser = userRepository.findById(candidateUser.getId()).orElseThrow();
        assertThat(updatedUser.getRole()).isEqualTo(Role.CANDIDATE);
        assertThat(passwordEncoder.matches("NewPassword123!", updatedUser.getPassword())).isTrue();
        assertThat(passwordEncoder.matches("OldPassword123!", updatedUser.getPassword())).isFalse();

        // 6. Verify token is marked as used and cannot be reused
        PasswordResetToken usedToken = tokenRepository.findById(resetToken.getId()).orElseThrow();
        assertThat(usedToken.isUsed()).isTrue();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("This password reset link is invalid or has expired."));
    }

    @Test
    @Order(2)
    @DisplayName("Forgot password for Admin preserves ADMIN role after password reset")
    void testAdminForgotPasswordAndResetFlow() throws Exception {
        String adminEmail = "admin.reset.test@example.com";
        User adminUser = userRepository.findByEmail(adminEmail).orElseGet(() -> {
            User u = new User("HR Admin", adminEmail, passwordEncoder.encode("OldAdmin123!"), Role.ADMIN);
            return userRepository.save(u);
        });

        // 1. Request forgot password
        ForgotPasswordRequest forgotReq = new ForgotPasswordRequest(adminEmail);
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        PasswordResetToken resetToken = tokenRepository.findTopByUserOrderByCreatedAtDesc(adminUser).orElseThrow();
        String testAdminRawToken = "test-raw-admin-token-987654321";
        resetToken.setTokenHash(hashToken(testAdminRawToken));
        tokenRepository.save(resetToken);

        // 2. Perform reset
        ResetPasswordRequest resetReq = new ResetPasswordRequest(testAdminRawToken, "NewAdminPassword123!");
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        User updatedAdmin = userRepository.findById(adminUser.getId()).orElseThrow();
        assertThat(updatedAdmin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(passwordEncoder.matches("NewAdminPassword123!", updatedAdmin.getPassword())).isTrue();
    }

    @Test
    @Order(3)
    @DisplayName("Forgot password for unknown email returns same generic message (anti-enumeration)")
    void testUnknownEmailReturnsSameGenericMessage() throws Exception {
        ForgotPasswordRequest forgotReq = new ForgotPasswordRequest("unknown.nonexistent.user@example.com");
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("If an account exists for this email, a password reset link has been sent."));
    }

    @Test
    @Order(4)
    @DisplayName("Reset password with expired or invalid token fails with 400 Bad Request")
    void testExpiredOrInvalidTokenFails() throws Exception {
        ResetPasswordRequest invalidReq = new ResetPasswordRequest("completely-non-existent-token", "NewPass123!");
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("This password reset link is invalid or has expired."));
    }

    private String hashToken(String rawToken) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
