package spring.eshwar.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import spring.eshwar.dto.application.ApplicationRequest;
import spring.eshwar.dto.auth.LoginRequest;
import spring.eshwar.dto.auth.RegisterRequest;
import spring.eshwar.dto.candidate.CandidateProfileUpdateRequest;
import spring.eshwar.dto.evaluation.EvaluationCriteriaRequest;
import spring.eshwar.dto.interview.InterviewRequest;
import spring.eshwar.dto.job.JobRequest;
import spring.eshwar.entity.Application;
import spring.eshwar.entity.Role;
import spring.eshwar.entity.ScreeningResult;
import spring.eshwar.repository.ApplicationRepository;
import spring.eshwar.repository.CandidateRepository;
import spring.eshwar.repository.EvaluationCriteriaRepository;
import spring.eshwar.repository.InterviewRepository;
import spring.eshwar.repository.JobRepository;
import spring.eshwar.repository.ResumeRepository;
import spring.eshwar.repository.ScreeningResultRepository;
import spring.eshwar.repository.UserRepository;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CompleteBackendAuditIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private EvaluationCriteriaRepository evaluationCriteriaRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ScreeningResultRepository screeningResultRepository;

    @Autowired
    private InterviewRepository interviewRepository;

    // Tokens and IDs shared across the sequenced test suite
    private String adminToken;
    private String candidateToken;
    private String secondCandidateToken;

    private Long adminUserId;
    private Long candidateUserId;
    private Long candidateId;
    private Long secondCandidateId;

    private Long createdJobId;
    private Long createdCriteriaId;
    private Long candidateResumeId;
    private Long candidateApplicationId;
    private Long secondApplicationId;
    private Long scheduledInterviewId;

    private static final byte[] VALID_PDF_BYTES = ("%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\ntrailer\n<< /Root 1 0 R >>\n%%EOF")
            .getBytes(StandardCharsets.US_ASCII);

    // =========================================================================
    // CANDIDATE WORKFLOW
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("CANDIDATE 1 & 2: Register & Verify Password Never Returned")
    void testCandidateRegistration() throws Exception {
        String email = "candidate.audit." + System.currentTimeMillis() + "@hireranker.com";
        RegisterRequest regReq = new RegisterRequest();
        regReq.setName("Eshwar Audit Candidate");
        regReq.setEmail(email);
        regReq.setPassword("SecureCandidatePass123!");
        regReq.setRole(Role.CANDIDATE);

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.password").doesNotExist()) // Password NEVER returned
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        candidateToken = json.get("token").asText();
        candidateUserId = json.get("id").asLong();

        var candOpt = candidateRepository.findByUserEmail(email);
        assertThat(candOpt).isPresent();
        candidateId = candOpt.get().getId();

        // Register a second candidate for ranking/shortlisting multi-candidate tests
        String email2 = "candidate2.audit." + System.currentTimeMillis() + "@hireranker.com";
        RegisterRequest regReq2 = new RegisterRequest();
        regReq2.setName("Second Candidate");
        regReq2.setEmail(email2);
        regReq2.setPassword("SecureCandidatePass123!");
        regReq2.setRole(Role.CANDIDATE);

        MvcResult result2 = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq2)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json2 = objectMapper.readTree(result2.getResponse().getContentAsString());
        secondCandidateToken = json2.get("token").asText();
        secondCandidateId = candidateRepository.findByUserEmail(email2).orElseThrow().getId();
    }

    @Test
    @Order(2)
    @DisplayName("CANDIDATE 2: Login via /api/auth/login")
    void testCandidateLogin() throws Exception {
        var candidateUser = userRepository.findById(candidateUserId).orElseThrow();
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail(candidateUser.getEmail());
        loginReq.setPassword("SecureCandidatePass123!");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("CANDIDATE"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        candidateToken = json.get("token").asText();
    }

    @Test
    @Order(3)
    @DisplayName("CANDIDATE 3: Complete Profile via PUT /api/candidates/me")
    void testCandidateCompleteProfile() throws Exception {
        CandidateProfileUpdateRequest updateReq = new CandidateProfileUpdateRequest();
        updateReq.setFullName("Eshwar Full Stack Architect");
        updateReq.setPhone("+91 9876543210");
        updateReq.setLocation("Hyderabad, India");
        updateReq.setSkills("Java, Spring Boot, Angular, TypeScript, SQL, Docker");
        updateReq.setExperience("5 years full stack architecture");
        updateReq.setEducation("B.Tech in Computer Science");
        updateReq.setGithub("https://github.com/eshwar");
        updateReq.setLinkedin("https://linkedin.com/in/eshwar");

        mockMvc.perform(put("/api/candidates/me")
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Eshwar Full Stack Architect"))
                .andExpect(jsonPath("$.phone").value("+91 9876543210"))
                .andExpect(jsonPath("$.skills").value(containsString("Java")))
                .andExpect(jsonPath("$.location").value("Hyderabad, India"));
    }

    @Test
    @Order(4)
    @DisplayName("CANDIDATE 4: Upload Resume via POST /api/resumes/upload")
    void testCandidateUploadResume() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "eshwar_senior_resume.pdf",
                "application/pdf",
                VALID_PDF_BYTES
        );

        MvcResult result = mockMvc.perform(multipart("/api/resumes/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("eshwar_senior_resume.pdf"))
                .andExpect(jsonPath("$.fileType").value("application/pdf"))
                .andExpect(jsonPath("$.candidateId").value(candidateId))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        candidateResumeId = json.get("id").asLong();

        // Also upload a resume for second candidate
        MockMultipartFile file2 = new MockMultipartFile(
                "file",
                "second_candidate_resume.pdf",
                "application/pdf",
                VALID_PDF_BYTES
        );
        mockMvc.perform(multipart("/api/resumes/upload")
                        .file(file2)
                        .header("Authorization", "Bearer " + secondCandidateToken))
                .andExpect(status().isCreated())
                .andReturn();
    }

    // =========================================================================
    // ADMIN WORKFLOW
    // =========================================================================

    @Test
    @Order(5)
    @DisplayName("ADMIN 1: Register & Login via /api/auth/login")
    void testAdminRegisterAndLogin() throws Exception {
        String adminEmail = "admin.audit." + System.currentTimeMillis() + "@hireranker.com";
        RegisterRequest adminReg = new RegisterRequest();
        adminReg.setName("System Admin Recruiter");
        adminReg.setEmail(adminEmail);
        adminReg.setPassword("AdminSecurePass123!");
        adminReg.setRole(Role.ADMIN);

        MvcResult regRes = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminReg)))
                .andExpect(status().isCreated())
                .andReturn();

        adminUserId = objectMapper.readTree(regRes.getResponse().getContentAsString()).get("id").asLong();

        // Login as admin
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail(adminEmail);
        loginReq.setPassword("AdminSecurePass123!");

        MvcResult loginRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        adminToken = objectMapper.readTree(loginRes.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    @Order(6)
    @DisplayName("ADMIN 2: Create Job via POST /api/jobs")
    void testAdminCreateJob() throws Exception {
        JobRequest jobReq = new JobRequest();
        jobReq.setTitle("Lead Full Stack Java & Angular Engineer");
        jobReq.setCompany("HireRanker Tech Corp");
        jobReq.setDescription("Architect next-generation automated recruiting and AI screening pipelines.");
        jobReq.setRequiredSkills("Java 21, Spring Boot, Angular 18, TypeScript, PostgreSQL");
        jobReq.setExperienceRequired("4+ Years");
        jobReq.setLocation("Hyderabad / Remote");
        jobReq.setSalaryRange("₹15 - 25 LPA");
        jobReq.setEmploymentType("Full Time");

        MvcResult res = mockMvc.perform(post("/api/jobs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jobReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Lead Full Stack Java & Angular Engineer"))
                .andReturn();

        createdJobId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    @Order(7)
    @DisplayName("ADMIN 3: Add Evaluation Criteria via POST /api/evaluation-criteria")
    void testAdminAddEvaluationCriteria() throws Exception {
        EvaluationCriteriaRequest critReq = new EvaluationCriteriaRequest();
        critReq.setJobId(createdJobId);
        critReq.setRequiredSkills("Java, Spring Boot, Angular, TypeScript");
        critReq.setMinimumExperience(3.0);
        critReq.setEducationRequirements("Bachelor's Degree in Computer Science or equivalent");
        critReq.setSkillWeight(50.0);
        critReq.setExperienceWeight(30.0);
        critReq.setEducationWeight(20.0);

        MvcResult res = mockMvc.perform(post("/api/evaluation-criteria")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(critReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.jobId").value(createdJobId))
                .andExpect(jsonPath("$.skillWeight").value(50.0))
                .andReturn();

        createdCriteriaId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();
    }

    // =========================================================================
    // CANDIDATE APPLY WORKFLOW
    // =========================================================================

    @Test
    @Order(8)
    @DisplayName("CANDIDATE 5: View Jobs via GET /api/jobs")
    void testCandidateViewJobs() throws Exception {
        mockMvc.perform(get("/api/jobs")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)));
    }

    @Test
    @Order(9)
    @DisplayName("CANDIDATE 6: Apply for Job via POST /api/applications")
    void testCandidateApplyForJob() throws Exception {
        ApplicationRequest appReq = new ApplicationRequest();
        appReq.setCandidateId(candidateId);
        appReq.setJobId(createdJobId);
        appReq.setResumeId(candidateResumeId);

        MvcResult res = mockMvc.perform(post("/api/applications")
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(appReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.candidateId").value(candidateId))
                .andExpect(jsonPath("$.jobId").value(createdJobId))
                .andExpect(jsonPath("$.status").value("APPLIED"))
                .andReturn();

        candidateApplicationId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        // Also submit application for candidate 2
        var cand2Resumes = resumeRepository.findAllByCandidateId(secondCandidateId);
        Long cand2ResumeId = cand2Resumes.isEmpty() ? null : cand2Resumes.get(0).getId();

        ApplicationRequest appReq2 = new ApplicationRequest();
        appReq2.setCandidateId(secondCandidateId);
        appReq2.setJobId(createdJobId);
        appReq2.setResumeId(cand2ResumeId);

        MvcResult res2 = mockMvc.perform(post("/api/applications")
                        .header("Authorization", "Bearer " + secondCandidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(appReq2)))
                .andExpect(status().isCreated())
                .andReturn();

        secondApplicationId = objectMapper.readTree(res2.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    @Order(10)
    @DisplayName("VERIFY: Duplicate Application Prevention - 409 Conflict")
    void testPreventDuplicateApplication() throws Exception {
        ApplicationRequest duplicateReq = new ApplicationRequest();
        duplicateReq.setCandidateId(candidateId);
        duplicateReq.setJobId(createdJobId);
        duplicateReq.setResumeId(candidateResumeId);

        mockMvc.perform(post("/api/applications")
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @Order(11)
    @DisplayName("CANDIDATE 7: View Application Status via GET /api/applications/candidate/{id}")
    void testCandidateViewApplicationStatus() throws Exception {
        mockMvc.perform(get("/api/applications/candidate/" + candidateId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].jobId").value(createdJobId));
    }

    // =========================================================================
    // ADMIN SCREENING & RANKING WORKFLOW
    // =========================================================================

    @Test
    @Order(12)
    @DisplayName("ADMIN 4 & 5: View Applications and Resumes")
    void testAdminViewApplicationsAndResumes() throws Exception {
        // View all applications
        mockMvc.perform(get("/api/applications")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(2)));

        // View specific resume
        mockMvc.perform(get("/api/resumes/" + candidateResumeId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(candidateResumeId))
                .andExpect(jsonPath("$.fileName").value("eshwar_senior_resume.pdf"));
    }

    @Test
    @Order(13)
    @DisplayName("ADMIN 6 & 7: Screen Resumes & View Screening Results")
    void testAdminScreenResumes() throws Exception {
        // Pre-populate resume extracted text for screening test
        var resume = resumeRepository.findById(candidateResumeId).orElseThrow();
        resume.setExtractedText("Eshwar Rao - 5 years experience in Java 21, Spring Boot, Angular 18, PostgreSQL, Microservices.");
        resumeRepository.save(resume);

        mockMvc.perform(post("/api/applications/" + candidateApplicationId + "/screen")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value(candidateApplicationId))
                .andExpect(jsonPath("$.overallScore").isNumber())
                .andExpect(jsonPath("$.skillsScore").isNumber());

        // Also test GET screening result via both singular and plural alias
        mockMvc.perform(get("/api/screening/application/" + candidateApplicationId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value(candidateApplicationId));

        mockMvc.perform(get("/api/screenings/application/" + candidateApplicationId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value(candidateApplicationId));
    }

    @Test
    @Order(14)
    @DisplayName("CANDIDATE 8: Candidate View Screening Result where permitted")
    void testCandidateViewScreeningResult() throws Exception {
        mockMvc.perform(get("/api/screening/application/" + candidateApplicationId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value(candidateApplicationId));
    }

    @Test
    @Order(15)
    @DisplayName("ADMIN 8: View Candidate Ranking via GET /api/jobs/{jobId}/ranking")
    void testAdminViewCandidateRanking() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/jobs/" + createdJobId + "/ranking")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].rank").value(1))
                .andReturn();

        JsonNode rankingArray = objectMapper.readTree(res.getResponse().getContentAsString());
        // Screened candidate should have rank 1
        assertThat(rankingArray.get(0).get("applicationId").asLong()).isEqualTo(candidateApplicationId);
        // Unscreened candidate 2 placed deterministically
        assertThat(rankingArray.get(1).get("rank").asInt()).isEqualTo(2);
    }

    @Test
    @Order(16)
    @DisplayName("ADMIN 9: Shortlist Candidate via PUT /api/applications/{id}/shortlist")
    void testAdminShortlistCandidate() throws Exception {
        mockMvc.perform(put("/api/applications/" + candidateApplicationId + "/shortlist")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHORTLISTED"));

        // Verify in shortlisted list
        mockMvc.perform(get("/api/jobs/" + createdJobId + "/shortlisted")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].applicationId").value(candidateApplicationId));

        // Also shortlist second application so there is a shortlisted candidate remaining after candidate 1 transitions to interview
        Application secondApp = applicationRepository.findById(secondApplicationId).orElseThrow();
        ScreeningResult sr2 = new ScreeningResult(
                secondApp, 75.0, 70.0, 80.0, 75.0,
                "Java", "Spring Boot", "CONSIDER"
        );
        screeningResultRepository.save(sr2);

        mockMvc.perform(put("/api/applications/" + secondApplicationId + "/shortlist")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(17)
    @DisplayName("CANDIDATE 9: Candidate View Shortlist Status via Candidate Dashboard")
    void testCandidateViewShortlistStatus() throws Exception {
        mockMvc.perform(get("/api/candidates/me/dashboard")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortlistedApplications").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.candidateProfile.fullName").value("Eshwar Full Stack Architect"));
    }

    @Test
    @Order(18)
    @DisplayName("ADMIN 10: Schedule Interview via POST /api/interviews")
    void testAdminScheduleInterview() throws Exception {
        InterviewRequest intReq = new InterviewRequest();
        intReq.setApplicationId(candidateApplicationId);
        intReq.setScheduledDateTime(LocalDateTime.now().plusDays(2));
        intReq.setInterviewType("ONLINE");
        intReq.setMeetingLink("https://meet.hireranker.com/audit-round-1");
        intReq.setNotes("Comprehensive technical evaluation with engineering panel");

        MvcResult res = mockMvc.perform(post("/api/interviews")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(intReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.applicationId").value(candidateApplicationId))
                .andExpect(jsonPath("$.interviewType").value("ONLINE"))
                .andExpect(jsonPath("$.meetingLink").value("https://meet.hireranker.com/audit-round-1"))
                .andReturn();

        scheduledInterviewId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    @Order(19)
    @DisplayName("CANDIDATE 10: View Interview via GET /api/interviews/candidate/{id}")
    void testCandidateViewInterview() throws Exception {
        mockMvc.perform(get("/api/interviews/candidate/" + candidateId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(scheduledInterviewId))
                .andExpect(jsonPath("$[0].meetingLink").value("https://meet.hireranker.com/audit-round-1"));
    }

    @Test
    @Order(20)
    @DisplayName("ADMIN 11: View Dashboard Statistics via GET /api/admin/dashboard")
    void testAdminViewDashboardStatistics() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCandidates").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.totalApplications").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.totalJobs").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.shortlistedCandidates").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.totalInterviews").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.applicantsOverview").isMap())
                .andExpect(jsonPath("$.applicationStatus").isArray());
    }

    // =========================================================================
    // SECURITY & EDGE CASE VALIDATIONS
    // =========================================================================

    @Test
    @Order(21)
    @DisplayName("SECURITY: Invalid and Expired JWT Handling (401 Unauthorized)")
    void testInvalidJwtHandling() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer invalid.malformed.token"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(22)
    @DisplayName("SECURITY: Candidate Forbidden From Admin Endpoint (403 Forbidden)")
    void testCandidateForbiddenFromAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/jobs/" + createdJobId + "/ranking")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(23)
    @DisplayName("SECURITY: Candidate Cannot Access Another Candidate's Data (403 Forbidden)")
    void testCandidateCannotAccessOtherCandidateData() throws Exception {
        // Candidate 2 attempting to view Candidate 1's profile
        mockMvc.perform(get("/api/candidates/" + candidateId)
                        .header("Authorization", "Bearer " + secondCandidateToken))
                .andExpect(status().isForbidden());

        // Candidate 2 attempting to view Candidate 1's interview
        mockMvc.perform(get("/api/interviews/" + scheduledInterviewId)
                        .header("Authorization", "Bearer " + secondCandidateToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(24)
    @DisplayName("SECURITY: Path Traversal & Invalid File Rejections")
    void testFileSecurityRejections() throws Exception {
        // 1. Path traversal
        MockMultipartFile traversalFile = new MockMultipartFile(
                "file",
                "../../malicious.pdf",
                "application/pdf",
                VALID_PDF_BYTES
        );
        mockMvc.perform(multipart("/api/resumes/upload")
                        .file(traversalFile)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isBadRequest());

        // 2. Non-PDF file
        MockMultipartFile textFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Plain text resume".getBytes(StandardCharsets.UTF_8)
        );
        mockMvc.perform(multipart("/api/resumes/upload")
                        .file(textFile)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(25)
    @DisplayName("EDGE CASE: Interview Scheduling Validation (Past Date Rejected)")
    void testInterviewPastDateRejected() throws Exception {
        InterviewRequest pastReq = new InterviewRequest();
        pastReq.setApplicationId(candidateApplicationId);
        pastReq.setScheduledDateTime(LocalDateTime.now().minusDays(1)); // Past date
        pastReq.setInterviewType("ONLINE");

        mockMvc.perform(post("/api/interviews")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pastReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(26)
    @DisplayName("EDGE CASE: Ranking with Non-Existent Job ID (404 Not Found)")
    void testRankingNonExistentJob() throws Exception {
        mockMvc.perform(get("/api/jobs/999999/ranking")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }
}
