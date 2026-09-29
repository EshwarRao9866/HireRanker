package spring.eshwar.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
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
import spring.eshwar.entity.Resume;
import spring.eshwar.entity.Role;
import spring.eshwar.repository.CandidateRepository;
import spring.eshwar.repository.ResumeRepository;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ResumeTextExtractionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private ResumeTextExtractionService textExtractionService;

    private String candidate1Token;
    private Long candidate1Id;

    private String candidate2Token;
    private Long candidate2Id;

    private String candidate3Token;
    private Long candidate3Id;

    private Long resume1Id;
    private String resume1FilePath;

    private byte[] backendResumePdfBytes;
    private byte[] frontendResumePdfBytes;
    private byte[] devopsResumePdfBytes;
    private byte[] blankPdfBytes;
    private byte[] encryptedPdfBytes;
    private byte[] corruptedPdfBytes;

    private final List<Long> createdResumeIds = new ArrayList<>();
    private final List<String> createdDiskFilePaths = new ArrayList<>();

    @BeforeAll
    void setUp() throws Exception {
        // 1. Resume 1: Backend Developer
        backendResumePdfBytes = createPdfWithText(
                "Alex Morgan - Senior Backend Developer",
                "Java 21, Spring Boot 3, Microservices, PostgreSQL, Docker, AWS",
                "5 years designing high-throughput REST APIs and distributed systems",
                "Bachelor of Technology in Computer Science"
        );

        // 2. Resume 2: Frontend Developer
        frontendResumePdfBytes = createPdfWithText(
                "Jordan Lee - Lead Frontend Engineer",
                "Angular 17, TypeScript, RxJS, HTML5, CSS3, Tailwind CSS, Jest",
                "4 years architecting enterprise single page applications",
                "Master of Science in Software Engineering"
        );

        // 3. Resume 3: DevOps & Cloud Engineer
        devopsResumePdfBytes = createPdfWithText(
                "Taylor Swift - Senior DevOps Cloud Architect",
                "Kubernetes, Docker, Terraform, CI/CD, Linux, Python, AWS, Prometheus",
                "6 years managing cloud-native infrastructure and Kubernetes clusters",
                "Bachelor of Science in Information Technology"
        );

        // 4. Blank PDF (page with no text layer)
        try (PDDocument blankDoc = new PDDocument()) {
            blankDoc.addPage(new PDPage(PDRectangle.A4));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            blankDoc.save(baos);
            blankPdfBytes = baos.toByteArray();
        }

        // 5. Encrypted / Password-Protected PDF
        try (PDDocument encDoc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            encDoc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(encDoc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 750);
                cs.showText("Confidential Candidate Resume - Strictly Password Protected");
                cs.endText();
            }
            AccessPermission ap = new AccessPermission();
            StandardProtectionPolicy spp = new StandardProtectionPolicy("ownerPass123!", "userPass123!", ap);
            spp.setEncryptionKeyLength(128);
            encDoc.protect(spp);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            encDoc.save(baos);
            encryptedPdfBytes = baos.toByteArray();
        }

        // 6. Corrupted PDF (valid %PDF- magic byte header, corrupt internal xref/stream bytes)
        corruptedPdfBytes = "%PDF-1.4\nINVALID_OBJECT_CORRUPTED_STREAM_DATA_%%EOF"
                .getBytes(StandardCharsets.UTF_8);

        // Register Candidate 1 (Backend)
        candidate1Token = registerCandidate("Alex Morgan", "cand.backend." + System.currentTimeMillis() + "@hireranker.com");
        candidate1Id = candidateRepository.findByUserEmail(candidateEmailFromToken(candidate1Token)).orElseThrow().getId();

        // Register Candidate 2 (Frontend)
        candidate2Token = registerCandidate("Jordan Lee", "cand.frontend." + System.currentTimeMillis() + "@hireranker.com");
        candidate2Id = candidateRepository.findByUserEmail(candidateEmailFromToken(candidate2Token)).orElseThrow().getId();

        // Register Candidate 3 (DevOps)
        candidate3Token = registerCandidate("Taylor Swift", "cand.devops." + System.currentTimeMillis() + "@hireranker.com");
        candidate3Id = candidateRepository.findByUserEmail(candidateEmailFromToken(candidate3Token)).orElseThrow().getId();
    }

    private String registerCandidate(String name, String email) throws Exception {
        RegisterRequest regReq = new RegisterRequest();
        regReq.setName(name);
        regReq.setEmail(email);
        regReq.setPassword("Password123!");
        regReq.setRole(Role.CANDIDATE);

        MvcResult regRes = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(regRes.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    private String candidateEmailFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JsonNode claims = objectMapper.readTree(payload);
            return claims.get("sub").asText();
        } catch (Exception e) {
            throw new RuntimeException("Could not parse JWT token: " + e.getMessage(), e);
        }
    }

    private byte[] createPdfWithText(String title, String skills, String experience, String education) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
                cs.newLineAtOffset(50, 750);
                cs.showText(title);
                cs.newLineAtOffset(0, -25);
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.showText("Skills: " + skills);
                cs.newLineAtOffset(0, -20);
                cs.showText("Experience: " + experience);
                cs.newLineAtOffset(0, -20);
                cs.showText("Education: " + education);
                cs.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    @Test
    @Order(1)
    @DisplayName("1. Upload Resume 1 (Backend Engineer): Triggers automated text extraction, persists to DB, preserves disk file & candidate ownership")
    void testUploadTriggersTextExtraction_BackendResume() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "Alex_Morgan_Backend_Resume.pdf",
                "application/pdf",
                backendResumePdfBytes
        );

        MvcResult result = mockMvc.perform(multipart("/api/resumes/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + candidate1Token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.candidateId").value(candidate1Id))
                .andExpect(jsonPath("$.extractedText").isNotEmpty())
                .andExpect(jsonPath("$.extractedText").value(containsString("Alex Morgan")))
                .andExpect(jsonPath("$.extractedText").value(containsString("Java 21")))
                .andExpect(jsonPath("$.extractedText").value(containsString("Spring Boot 3")))
                .andExpect(jsonPath("$.extractedText").value(containsString("Microservices")))
                .andExpect(jsonPath("$.extractedText").value(containsString("Bachelor of Technology")))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        resume1Id = json.get("id").asLong();
        resume1FilePath = json.get("filePath").asText();
        createdResumeIds.add(resume1Id);
        createdDiskFilePaths.add(resume1FilePath);

        // Verify Resume in MySQL
        Resume saved = resumeRepository.findById(resume1Id).orElseThrow();
        assertThat(saved.getExtractedText()).contains("Java 21");
        assertThat(saved.getExtractedText()).contains("Spring Boot 3");
        // Verify candidate ownership preserved
        assertThat(saved.getCandidate().getId()).isEqualTo(candidate1Id);

        // Verify physical PDF file preserved on disk
        Path diskPath = Paths.get(resume1FilePath);
        assertThat(Files.exists(diskPath)).isTrue();
        assertThat(Files.size(diskPath)).isEqualTo(backendResumePdfBytes.length);
    }

    @Test
    @Order(2)
    @DisplayName("2. Upload Resume 2 (Frontend Engineer): Extracts Angular, TypeScript, RxJS, Tailwind CSS text accurately")
    void testUploadTriggersTextExtraction_FrontendResume() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "Jordan_Lee_Frontend_Resume.pdf",
                "application/pdf",
                frontendResumePdfBytes
        );

        MvcResult result = mockMvc.perform(multipart("/api/resumes/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + candidate2Token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.candidateId").value(candidate2Id))
                .andExpect(jsonPath("$.extractedText").value(containsString("Jordan Lee")))
                .andExpect(jsonPath("$.extractedText").value(containsString("Angular 17")))
                .andExpect(jsonPath("$.extractedText").value(containsString("TypeScript")))
                .andExpect(jsonPath("$.extractedText").value(containsString("RxJS")))
                .andExpect(jsonPath("$.extractedText").value(containsString("Tailwind CSS")))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        Long id = json.get("id").asLong();
        String path = json.get("filePath").asText();
        createdResumeIds.add(id);
        createdDiskFilePaths.add(path);

        Resume saved = resumeRepository.findById(id).orElseThrow();
        assertThat(saved.getExtractedText()).contains("Angular 17");
        assertThat(saved.getCandidate().getId()).isEqualTo(candidate2Id);
        assertThat(Files.exists(Paths.get(path))).isTrue();
    }

    @Test
    @Order(3)
    @DisplayName("3. Upload Resume 3 (DevOps Engineer): Extracts Kubernetes, Terraform, CI/CD, Prometheus accurately")
    void testUploadTriggersTextExtraction_DevopsResume() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "Taylor_Swift_DevOps_Resume.pdf",
                "application/pdf",
                devopsResumePdfBytes
        );

        MvcResult result = mockMvc.perform(multipart("/api/resumes/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + candidate3Token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.candidateId").value(candidate3Id))
                .andExpect(jsonPath("$.extractedText").value(containsString("Taylor Swift")))
                .andExpect(jsonPath("$.extractedText").value(containsString("Kubernetes")))
                .andExpect(jsonPath("$.extractedText").value(containsString("Terraform")))
                .andExpect(jsonPath("$.extractedText").value(containsString("Prometheus")))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        Long id = json.get("id").asLong();
        String path = json.get("filePath").asText();
        createdResumeIds.add(id);
        createdDiskFilePaths.add(path);

        Resume saved = resumeRepository.findById(id).orElseThrow();
        assertThat(saved.getExtractedText()).contains("Kubernetes");
        assertThat(saved.getCandidate().getId()).isEqualTo(candidate3Id);
        assertThat(Files.exists(Paths.get(path))).isTrue();
    }

    @Test
    @Order(4)
    @DisplayName("4. Handle Blank/Empty PDF: Gracefully saves empty string without application error")
    void testBlankPdfExtraction() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "blank_resume.pdf",
                "application/pdf",
                blankPdfBytes
        );

        MvcResult result = mockMvc.perform(multipart("/api/resumes/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + candidate1Token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.extractedText").value(""))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        Long id = json.get("id").asLong();
        String path = json.get("filePath").asText();
        createdResumeIds.add(id);
        createdDiskFilePaths.add(path);

        Resume saved = resumeRepository.findById(id).orElseThrow();
        assertThat(saved.getExtractedText()).isEqualTo("");
    }

    @Test
    @Order(5)
    @DisplayName("5. Handle Encrypted/Password-Protected PDF: Gracefully flags warning without failing upload")
    void testEncryptedPdfExtraction() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "encrypted_resume.pdf",
                "application/pdf",
                encryptedPdfBytes
        );

        MvcResult result = mockMvc.perform(multipart("/api/resumes/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + candidate1Token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.extractedText").value(containsString("EXTRACTION_WARNING")))
                .andExpect(jsonPath("$.extractedText").value(containsString("encrypted")))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        Long id = json.get("id").asLong();
        String path = json.get("filePath").asText();
        createdResumeIds.add(id);
        createdDiskFilePaths.add(path);

        Resume saved = resumeRepository.findById(id).orElseThrow();
        assertThat(saved.getExtractedText()).contains("EXTRACTION_WARNING");
    }

    @Test
    @Order(6)
    @DisplayName("6. Handle Corrupted PDF: Gracefully flags failure and does NOT expose local server paths")
    void testCorruptedPdfExtraction() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "corrupted_resume.pdf",
                "application/pdf",
                corruptedPdfBytes
        );

        MvcResult result = mockMvc.perform(multipart("/api/resumes/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + candidate1Token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.extractedText").value(containsString("EXTRACTION_FAILED")))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String extracted = json.get("extractedText").asText();
        Long id = json.get("id").asLong();
        String path = json.get("filePath").asText();
        createdResumeIds.add(id);
        createdDiskFilePaths.add(path);

        // Ensure no internal filesystem paths are exposed in the extractedText
        assertThat(extracted).doesNotContain("C:\\");
        assertThat(extracted).doesNotContain("/uploads");
        assertThat(extracted).contains("Corrupted or unreadable PDF format");
    }

    @Test
    @Order(7)
    @DisplayName("7. Manual Text Re-Extraction Endpoint: POST /api/resumes/{id}/extract-text returns 200 OK with extracted text")
    void testManualExtractTextEndpoint() throws Exception {
        // Re-upload backend resume for candidate 1
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "Alex_Morgan_Backend_Resume.pdf",
                "application/pdf",
                backendResumePdfBytes
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/resumes/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + candidate1Token))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        Long resId = json.get("id").asLong();
        createdResumeIds.add(resId);
        createdDiskFilePaths.add(json.get("filePath").asText());

        // Call manual re-extraction
        MvcResult manualResult = mockMvc.perform(post("/api/resumes/" + resId + "/extract-text")
                        .header("Authorization", "Bearer " + candidate1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(resId))
                .andExpect(jsonPath("$.extractedText").value(containsString("Alex Morgan")))
                .andExpect(jsonPath("$.extractedText").value(containsString("Java 21")))
                .andExpect(jsonPath("$.extractedText").value(containsString("PostgreSQL")))
                .andReturn();

        JsonNode manualJson = objectMapper.readTree(manualResult.getResponse().getContentAsString());
        assertThat(manualJson.get("extractedText").asText()).contains("Spring Boot 3");
    }

    @Test
    @Order(8)
    @DisplayName("8. Extraction Failure on Non-existent File: Returns diagnostic message without crashing")
    void testNonExistentFileExtraction() {
        Resume dummy = new Resume();
        dummy.setCandidate(candidateRepository.findById(candidate1Id).orElseThrow());
        dummy.setFileName("ghost.pdf");
        dummy.setFilePath("uploads/resumes/non_existent_path.pdf");
        dummy.setFileType("application/pdf");

        String result = textExtractionService.extractText(dummy);
        assertThat(result).contains("EXTRACTION_FAILED: File not found on server disk");
    }

    @AfterAll
    void cleanup() {
        for (Long id : createdResumeIds) {
            try {
                if (resumeRepository.existsById(id)) {
                    resumeRepository.deleteById(id);
                }
            } catch (Exception ignored) {}
        }
        for (String filePath : createdDiskFilePaths) {
            try {
                if (filePath != null) {
                    File f = new File(filePath);
                    if (f.exists()) {
                        f.delete();
                    }
                }
            } catch (Exception ignored) {}
        }
    }
}
