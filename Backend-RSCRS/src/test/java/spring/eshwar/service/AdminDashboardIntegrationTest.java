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
import spring.eshwar.dto.application.ApplicationRequest;
import spring.eshwar.dto.auth.LoginRequest;
import spring.eshwar.dto.auth.RegisterRequest;
import spring.eshwar.dto.dashboard.AdminDashboardResponse;
import spring.eshwar.dto.job.JobRequest;
import spring.eshwar.dto.resume.ResumeRequest;
import spring.eshwar.entity.Application;
import spring.eshwar.entity.ApplicationStatus;
import spring.eshwar.entity.Candidate;
import spring.eshwar.entity.Job;
import spring.eshwar.entity.JobStatus;
import spring.eshwar.entity.Resume;
import spring.eshwar.entity.Role;
import spring.eshwar.entity.ScreeningResult;
import spring.eshwar.repository.ApplicationRepository;
import spring.eshwar.repository.CandidateRepository;
import spring.eshwar.repository.InterviewRepository;
import spring.eshwar.repository.JobRepository;
import spring.eshwar.repository.ResumeRepository;
import spring.eshwar.repository.ScreeningResultRepository;
import spring.eshwar.repository.UserRepository;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdminDashboardIntegrationTest {

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
    private ResumeRepository resumeRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ScreeningResultRepository screeningResultRepository;

    private String adminToken;
    private String candidateToken;
    private Long candidateId;
    private Long jobId;
    private Long resumeId;
    private Long applicationId;
    private Long screeningResultId;

    @BeforeAll
    void setUp() throws Exception {
        // 1. Register and login Admin
        String adminEmail = "admin.dash." + System.currentTimeMillis() + "@hireranker.com";
        registerUser("Admin Executive", adminEmail, "AdminPass123!", Role.ADMIN);
        adminToken = loginUser(adminEmail, "AdminPass123!");

        // 2. Register and login Candidate
        String candidateEmail = "cand.dash." + System.currentTimeMillis() + "@hireranker.com";
        registerUser("Jane Dashboard", candidateEmail, "CandPass123!", Role.CANDIDATE);
        candidateToken = loginUser(candidateEmail, "CandPass123!");
        Candidate cand = candidateRepository.findByUserEmail(candidateEmail).orElseThrow();
        candidateId = cand.getId();
        cand.setSkills("Java, Spring Boot, SQL, Angular, Docker");
        candidateRepository.save(cand);

        // 3. Create Job
        JobRequest jobReq = new JobRequest();
        jobReq.setTitle("Lead Cloud Architect");
        jobReq.setCompany("Enterprise Systems");
        jobReq.setLocation("Bangalore, India");
        jobReq.setDescription("Architecting scalable cloud microservices.");
        jobReq.setRequiredSkills("Java, Spring Boot, Kubernetes, AWS");
        jobReq.setExperienceRequired("7+ years");
        jobReq.setStatus(JobStatus.ACTIVE);

        MvcResult jobRes = mockMvc.perform(post("/api/jobs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jobReq)))
                .andExpect(status().isCreated())
                .andReturn();
        jobId = objectMapper.readTree(jobRes.getResponse().getContentAsString()).get("id").asLong();

        // 4. Create Resume
        ResumeRequest resumeReq = new ResumeRequest();
        resumeReq.setCandidateId(candidateId);
        resumeReq.setFileName("jane_cloud_resume.pdf");
        resumeReq.setFileType("application/pdf");
        resumeReq.setFilePath("/uploads/resumes/jane_cloud_resume.pdf");
        resumeReq.setFileSize(204800L);
        resumeReq.setExtractedText("Experienced Cloud Architect with 8 years Java, Spring Boot, AWS, Docker");

        MvcResult resumeRes = mockMvc.perform(post("/api/resumes")
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resumeReq)))
                .andExpect(status().isCreated())
                .andReturn();
        resumeId = objectMapper.readTree(resumeRes.getResponse().getContentAsString()).get("id").asLong();

        // 5. Create Application
        ApplicationRequest appReq = new ApplicationRequest();
        appReq.setCandidateId(candidateId);
        appReq.setJobId(jobId);
        appReq.setResumeId(resumeId);

        MvcResult appRes = mockMvc.perform(post("/api/applications")
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(appReq)))
                .andExpect(status().isCreated())
                .andReturn();
        applicationId = objectMapper.readTree(appRes.getResponse().getContentAsString()).get("id").asLong();

        // 6. Update Application to SHORTLISTED
        Application app = applicationRepository.findById(applicationId).orElseThrow();
        app.setStatus(ApplicationStatus.SHORTLISTED);
        applicationRepository.save(app);

        // 7. Create ScreeningResult
        ScreeningResult sr = new ScreeningResult();
        sr.setApplication(app);
        sr.setOverallScore(94.5);
        sr.setSkillsScore(96.0);
        sr.setExperienceScore(95.0);
        sr.setEducationScore(92.0);
        sr.setMatchingSkills("Java, Spring Boot, AWS");
        sr.setMissingSkills("Kubernetes");
        sr.setRecommendation("HIGHLY_RECOMMENDED");
        sr.setScreenedAt(LocalDateTime.now());
        ScreeningResult savedSr = screeningResultRepository.save(sr);
        screeningResultId = savedSr.getId();
    }

    @AfterAll
    void tearDown() {
        try {
            if (screeningResultId != null && screeningResultRepository.existsById(screeningResultId)) {
                screeningResultRepository.deleteById(screeningResultId);
            }
            if (applicationId != null && applicationRepository.existsById(applicationId)) {
                applicationRepository.deleteById(applicationId);
            }
            if (resumeId != null && resumeRepository.existsById(resumeId)) {
                resumeRepository.deleteById(resumeId);
            }
            if (jobId != null && jobRepository.existsById(jobId)) {
                jobRepository.deleteById(jobId);
            }
        } catch (Exception ignored) {
        }
    }

    private void registerUser(String name, String email, String password, Role role) throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setName(name);
        req.setEmail(email);
        req.setPassword(password);
        req.setRole(role);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    private String loginUser(String email, String password) throws Exception {
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
    @DisplayName("1. Admin can access GET /api/admin/dashboard and receive full analytics")
    void testAdminCanAccessDashboard() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCandidates").isNumber())
                .andExpect(jsonPath("$.totalApplications").isNumber())
                .andExpect(jsonPath("$.totalJobs").isNumber())
                .andExpect(jsonPath("$.screenedResumes").isNumber())
                .andExpect(jsonPath("$.shortlistedCandidates").isNumber())
                .andExpect(jsonPath("$.averageMatchScore").isNumber())
                .andExpect(jsonPath("$.totalInterviews").isNumber())
                .andExpect(jsonPath("$.totalUsers").isNumber())
                .andExpect(jsonPath("$.applicantsOverview").isMap())
                .andExpect(jsonPath("$.applicationStatus").isArray())
                .andExpect(jsonPath("$.applicationStatusDistribution").isArray())
                .andExpect(jsonPath("$.topSkills").isArray())
                .andExpect(jsonPath("$.topRankedCandidates").isArray())
                .andReturn();

        JsonNode json = objectMapper.readTree(res.getResponse().getContentAsString());
        assertThat(json.get("totalCandidates").asLong()).isGreaterThanOrEqualTo(1L);
        assertThat(json.get("totalApplications").asLong()).isGreaterThanOrEqualTo(1L);
        assertThat(json.get("totalJobs").asLong()).isGreaterThanOrEqualTo(1L);
        assertThat(json.get("screenedResumes").asLong()).isGreaterThanOrEqualTo(1L);
        assertThat(json.get("shortlistedCandidates").asLong()).isGreaterThanOrEqualTo(1L);
        assertThat(json.get("averageMatchScore").asDouble()).isGreaterThan(0.0);
    }

    @Test
    @Order(2)
    @DisplayName("2. Chart data structures are populated and consistent")
    void testChartDataStructures() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(res.getResponse().getContentAsString());

        // Applicants overview
        JsonNode overview = json.get("applicantsOverview");
        assertThat(overview.get("totalApplicants").asLong()).isGreaterThanOrEqualTo(1L);
        assertThat(overview.get("screenedResumes").asLong()).isGreaterThanOrEqualTo(1L);
        assertThat(overview.get("labels").isArray()).isTrue();
        assertThat(overview.get("labels").size()).isEqualTo(7);
        assertThat(overview.get("applicationsSeries").isArray()).isTrue();
        assertThat(overview.get("screenedSeries").isArray()).isTrue();

        // Application status breakdown
        JsonNode statusList = json.get("applicationStatus");
        assertThat(statusList.size()).isGreaterThanOrEqualTo(5);
        boolean foundShortlisted = false;
        for (JsonNode st : statusList) {
            if ("SHORTLISTED".equals(st.get("status").asText())) {
                foundShortlisted = true;
                assertThat(st.get("count").asLong()).isGreaterThanOrEqualTo(1L);
                assertThat(st.get("percent").asDouble()).isGreaterThan(0.0);
            }
        }
        assertThat(foundShortlisted).isTrue();

        // Top skills
        JsonNode skills = json.get("topSkills");
        assertThat(skills.size()).isGreaterThanOrEqualTo(1);
        boolean hasJava = false;
        for (JsonNode sk : skills) {
            if ("Java".equalsIgnoreCase(sk.get("name").asText())) {
                hasJava = true;
            }
        }
        assertThat(hasJava).isTrue();

        // Top ranked candidates
        JsonNode topRanked = json.get("topRankedCandidates");
        assertThat(topRanked.size()).isGreaterThanOrEqualTo(1);
        JsonNode firstRank = topRanked.get(0);
        assertThat(firstRank.get("rank").asInt()).isEqualTo(1);
        assertThat(firstRank.get("candidateName").asText()).isEqualTo("Jane Dashboard");
        assertThat(firstRank.get("matchScore").asDouble()).isEqualTo(94.5);
    }

    @Test
    @Order(3)
    @DisplayName("3. Non-admin candidate receives 403 Forbidden")
    void testCandidateCannotAccessDashboard() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(4)
    @DisplayName("4. Unauthenticated request receives 401 Unauthorized")
    void testUnauthenticatedAccessDenied() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(5)
    @DisplayName("5. Empty database scenario returns zeroes, empty arrays, and valid labels without crashing")
    void testEmptyDatabaseGracefulHandling() {
        CandidateRepository mockCandRepo = org.mockito.Mockito.mock(CandidateRepository.class);
        ApplicationRepository mockAppRepo = org.mockito.Mockito.mock(ApplicationRepository.class);
        JobRepository mockJobRepo = org.mockito.Mockito.mock(JobRepository.class);
        ResumeRepository mockResumeRepo = org.mockito.Mockito.mock(ResumeRepository.class);
        ScreeningResultRepository mockScreenRepo = org.mockito.Mockito.mock(ScreeningResultRepository.class);
        InterviewRepository mockInterviewRepo = org.mockito.Mockito.mock(InterviewRepository.class);
        UserRepository mockUserRepo = org.mockito.Mockito.mock(UserRepository.class);

        AdminDashboardService emptyService = new AdminDashboardService(
                mockCandRepo, mockAppRepo, mockJobRepo, mockResumeRepo,
                mockScreenRepo, mockInterviewRepo, mockUserRepo
        );

        AdminDashboardResponse emptyData = emptyService.getDashboardData();
        assertThat(emptyData).isNotNull();
        assertThat(emptyData.getTotalCandidates()).isEqualTo(0L);
        assertThat(emptyData.getTotalJobs()).isEqualTo(0L);
        assertThat(emptyData.getTotalApplications()).isEqualTo(0L);
        assertThat(emptyData.getScreenedResumes()).isEqualTo(0L);
        assertThat(emptyData.getShortlistedCandidates()).isEqualTo(0L);
        assertThat(emptyData.getAverageMatchScore()).isEqualTo(0.0);
        assertThat(emptyData.getTotalInterviews()).isEqualTo(0L);
        assertThat(emptyData.getApplicantsOverview()).isNotNull();
        assertThat(emptyData.getApplicantsOverview().getTotalApplicants()).isEqualTo(0L);
        assertThat(emptyData.getApplicantsOverview().getLabels()).hasSize(7);
        assertThat(emptyData.getApplicationStatus()).isNotEmpty();
        assertThat(emptyData.getApplicationStatusDistribution()).isNotEmpty();
        assertThat(emptyData.getTopSkills()).isEmpty();
        assertThat(emptyData.getTopRankedCandidates()).isEmpty();
    }
}
