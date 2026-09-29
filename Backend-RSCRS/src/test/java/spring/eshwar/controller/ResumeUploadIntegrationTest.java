package spring.eshwar.controller;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import spring.eshwar.dto.auth.RegisterRequest;
import spring.eshwar.entity.Candidate;
import spring.eshwar.entity.Role;
import spring.eshwar.repository.CandidateRepository;
import spring.eshwar.repository.ResumeRepository;
import spring.eshwar.repository.UserRepository;
import spring.eshwar.util.FileStorageUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ResumeUploadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileStorageUtil fileStorageUtil;

    private String candidateToken;
    private String otherCandidateToken;
    private String adminToken;

    private Long candidateId;
    private Long otherCandidateId;
    private Long uploadedResumeId;
    private String uploadedDiskFilePath;

    // Minimal valid PDF binary starting with %PDF- header
    private static final byte[] VALID_PDF_BYTES = ("%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\ntrailer\n<< /Root 1 0 R >>\n%%EOF")
            .getBytes(StandardCharsets.US_ASCII);

    @BeforeAll
    void setUp() throws Exception {
        // 1. Register Candidate 1
        String email1 = "cand.upload1." + System.currentTimeMillis() + "@hireranker.com";
        RegisterRequest req1 = new RegisterRequest();
        req1.setName("Candidate One");
        req1.setEmail(email1);
        req1.setPassword("CandPass123!");
        req1.setRole(Role.CANDIDATE);

        MvcResult res1 = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json1 = objectMapper.readTree(res1.getResponse().getContentAsString());
        candidateToken = json1.get("token").asText();
        Candidate c1 = candidateRepository.findByUserEmail(email1).orElseThrow();
        candidateId = c1.getId();

        // 2. Register Candidate 2
        String email2 = "cand.upload2." + System.currentTimeMillis() + "@hireranker.com";
        RegisterRequest req2 = new RegisterRequest();
        req2.setName("Candidate Two");
        req2.setEmail(email2);
        req2.setPassword("CandPass123!");
        req2.setRole(Role.CANDIDATE);

        MvcResult res2 = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json2 = objectMapper.readTree(res2.getResponse().getContentAsString());
        otherCandidateToken = json2.get("token").asText();
        Candidate c2 = candidateRepository.findByUserEmail(email2).orElseThrow();
        otherCandidateId = c2.getId();

        // 3. Register Admin
        String adminEmail = "admin.upload." + System.currentTimeMillis() + "@hireranker.com";
        RegisterRequest adminReq = new RegisterRequest();
        adminReq.setName("Admin Upload");
        adminReq.setEmail(adminEmail);
        adminReq.setPassword("AdminPass123!");
        adminReq.setRole(Role.ADMIN);

        MvcResult adminRes = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode adminJson = objectMapper.readTree(adminRes.getResponse().getContentAsString());
        adminToken = adminJson.get("token").asText();
    }

    @Test
    @Order(1)
    @DisplayName("1. Upload Valid PDF - Candidate successfully uploads resume")
    void testUploadValidPdf_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "my_software_resume.pdf",
                "application/pdf",
                VALID_PDF_BYTES
        );

        MvcResult result = mockMvc.perform(multipart("/api/resumes/upload")
                        .file(file)
                        .param("candidateId", candidateId.toString())
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.candidateId").value(candidateId))
                .andExpect(jsonPath("$.fileName").value("my_software_resume.pdf"))
                .andExpect(jsonPath("$.originalFileName").value("my_software_resume.pdf"))
                .andExpect(jsonPath("$.generatedFileName").isNotEmpty())
                .andExpect(jsonPath("$.fileType").value("application/pdf"))
                .andExpect(jsonPath("$.filePath").isNotEmpty())
                .andExpect(jsonPath("$.fileSize").value(VALID_PDF_BYTES.length))
                .andExpect(jsonPath("$.uploadedAt").isNotEmpty())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        uploadedResumeId = json.get("id").asLong();
        uploadedDiskFilePath = json.get("filePath").asText();
        String genFileName = json.get("generatedFileName").asText();

        // Verify generated filename is unique and not equal to the original filename
        assertThat(genFileName).isNotEqualTo("my_software_resume.pdf");
        assertThat(genFileName).endsWith(".pdf");

        // Verify the file was stored on disk and is a valid file
        Path storedPath = Paths.get(uploadedDiskFilePath);
        assertThat(Files.exists(storedPath)).isTrue();
        assertThat(Files.size(storedPath)).isEqualTo(VALID_PDF_BYTES.length);
    }

    @Test
    @Order(2)
    @DisplayName("2. Reject Non-PDF Extension - Fails with 400 Bad Request")
    void testUploadNonPdfExtension_Fails() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.docx",
                "application/pdf",
                VALID_PDF_BYTES
        );

        mockMvc.perform(multipart("/api/resumes/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Only PDF files are allowed")));
    }

    @Test
    @Order(3)
    @DisplayName("3. Reject Non-PDF MIME Type - Fails with 400 Bad Request")
    void testUploadInvalidMimeType_Fails() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "text/plain",
                VALID_PDF_BYTES
        );

        mockMvc.perform(multipart("/api/resumes/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Content-Type must be application/pdf")));
    }

    @Test
    @Order(4)
    @DisplayName("4. Reject Corrupted File Missing %PDF- Header - Fails with 400 Bad Request")
    void testUploadFakePdfWithoutMagicBytes_Fails() throws Exception {
        byte[] fakeBytes = "This is just a text file renamed to .pdf".getBytes(StandardCharsets.UTF_8);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fake.pdf",
                "application/pdf",
                fakeBytes
        );

        mockMvc.perform(multipart("/api/resumes/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("file header does not match valid PDF")));
    }

    @Test
    @Order(5)
    @DisplayName("5. Reject Empty File - Fails with 400 Bad Request")
    void testUploadEmptyFile_Fails() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/resumes/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Please select a file to upload."));
    }

    @Test
    @Order(6)
    @DisplayName("6. Prevent Path Traversal in Filename - Fails with 400 Bad Request")
    void testUploadPathTraversal_Fails() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../../secret.pdf",
                "application/pdf",
                VALID_PDF_BYTES
        );

        mockMvc.perform(multipart("/api/resumes/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("path traversal")));
    }

    @Test
    @Order(7)
    @DisplayName("7. Reject Upload for Another Candidate - Fails with 403 Forbidden")
    void testUploadForAnotherCandidate_Fails() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "attempt.pdf",
                "application/pdf",
                VALID_PDF_BYTES
        );

        // Candidate 1 attempting to upload for Candidate 2's ID
        mockMvc.perform(multipart("/api/resumes/upload")
                        .file(file)
                        .param("candidateId", otherCandidateId.toString())
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not authorized to upload a resume for another candidate."));
    }

    @Test
    @Order(8)
    @DisplayName("8. Reject Unauthenticated Upload - Fails with 401 Unauthorized")
    void testUnauthenticatedUpload_Fails() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                VALID_PDF_BYTES
        );

        mockMvc.perform(multipart("/api/resumes/upload")
                        .file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(9)
    @DisplayName("9. Reject Admin Attempt to Upload Candidate Resume - Fails with 403 Forbidden")
    void testAdminUpload_Fails() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "admin_resume.pdf",
                "application/pdf",
                VALID_PDF_BYTES
        );

        // Only CANDIDATE role is allowed
        mockMvc.perform(multipart("/api/resumes/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(10)
    @DisplayName("10. Reject Oversized File - Fails when file exceeds 10 MB limit")
    void testUploadOversizedFile_Fails() throws Exception {
        // Create an oversized payload (10 MB + 1 KB = 10,486,784 bytes) with valid PDF header
        byte[] largeBytes = new byte[10 * 1024 * 1024 + 1024];
        System.arraycopy(VALID_PDF_BYTES, 0, largeBytes, 0, VALID_PDF_BYTES.length);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "too_large.pdf",
                "application/pdf",
                largeBytes
        );

        mockMvc.perform(multipart("/api/resumes/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("File size exceeds the maximum allowed limit")));
    }

    @Test
    @Order(11)
    @DisplayName("11. Handle Missing Candidate - Fails with 404 Not Found when Candidate profile does not exist")
    void testUploadMissingCandidate_Fails() throws Exception {
        // Register a candidate user
        String orphanEmail = "orphan.cand." + System.currentTimeMillis() + "@hireranker.com";
        RegisterRequest orphanReq = new RegisterRequest();
        orphanReq.setName("Orphan Candidate");
        orphanReq.setEmail(orphanEmail);
        orphanReq.setPassword("Password123!");
        orphanReq.setRole(Role.CANDIDATE);

        MvcResult orphanRes = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orphanReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode orphanJson = objectMapper.readTree(orphanRes.getResponse().getContentAsString());
        String orphanToken = orphanJson.get("token").asText();

        // Delete the candidate entity so the candidate profile is missing
        Candidate orphanCandidate = candidateRepository.findByUserEmail(orphanEmail).orElseThrow();
        candidateRepository.delete(orphanCandidate);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "orphan_resume.pdf",
                "application/pdf",
                VALID_PDF_BYTES
        );

        mockMvc.perform(multipart("/api/resumes/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + orphanToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Candidate")));
    }

    @Test
    @Order(12)
    @DisplayName("12. Candidate ID from Request Never Overrides JWT Identity")
    void testUploadWithoutCandidateId_BindsToAuthenticatedCandidate() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "jwt_identity_test.pdf",
                "application/pdf",
                VALID_PDF_BYTES
        );

        // Upload without candidateId param — must resolve automatically from JWT
        MvcResult result = mockMvc.perform(multipart("/api/resumes/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.candidateId").value(candidateId))
                .andExpect(jsonPath("$.fileName").value("jwt_identity_test.pdf"))
                .andExpect(jsonPath("$.originalFileName").value("jwt_identity_test.pdf"))
                .andExpect(jsonPath("$.generatedFileName").isNotEmpty())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        Long resId = json.get("id").asLong();
        String fPath = json.get("filePath").asText();

        // Clean up
        if (fPath != null) {
            new File(fPath).delete();
        }
    }

    @AfterAll
    void cleanup() throws Exception {
        if (uploadedResumeId != null) {
            mockMvc.perform(delete("/api/resumes/" + uploadedResumeId)
                            .header("Authorization", "Bearer " + candidateToken))
                    .andExpect(status().isNoContent());

            if (uploadedDiskFilePath != null) {
                File f = new File(uploadedDiskFilePath);
                if (f.exists()) {
                    f.delete();
                }
            }
        }
    }
}
