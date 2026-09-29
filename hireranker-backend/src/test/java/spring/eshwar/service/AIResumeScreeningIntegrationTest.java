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
import spring.eshwar.dto.auth.RegisterRequest;
import spring.eshwar.dto.evaluation.EvaluationCriteriaRequest;
import spring.eshwar.dto.job.JobRequest;
import spring.eshwar.dto.resume.ResumeRequest;
import spring.eshwar.entity.Application;
import spring.eshwar.entity.ApplicationStatus;
import spring.eshwar.entity.Candidate;
import spring.eshwar.entity.Job;
import spring.eshwar.entity.JobStatus;
import spring.eshwar.entity.Role;
import spring.eshwar.entity.ScreeningResult;
import spring.eshwar.repository.ApplicationRepository;
import spring.eshwar.repository.CandidateRepository;
import spring.eshwar.repository.EvaluationCriteriaRepository;
import spring.eshwar.repository.JobRepository;
import spring.eshwar.repository.ResumeRepository;
import spring.eshwar.repository.ScreeningResultRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AIResumeScreeningIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private EvaluationCriteriaRepository evaluationCriteriaRepository;

    @Autowired
    private ScreeningResultRepository screeningResultRepository;

    private String adminToken;
    private String candidateToken;

    private Long candidateId;
    private Long jobId;
    private Long criteriaId;
    private Long resumeId;
    private Long applicationId;
    private Long screeningResultId;

    @BeforeAll
    void setUp() throws Exception {
        // 1. Register Admin
        String adminEmail = "admin.screen." + System.currentTimeMillis() + "@hireranker.com";
        RegisterRequest adminReq = new RegisterRequest();
        adminReq.setName("Admin Screener");
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

        // 2. Register Candidate
        String candEmail = "cand.screen." + System.currentTimeMillis() + "@hireranker.com";
        RegisterRequest candReq = new RegisterRequest();
        candReq.setName("Alex Screener");
        candReq.setEmail(candEmail);
        candReq.setPassword("CandPass123!");
        candReq.setRole(Role.CANDIDATE);

        MvcResult candRes = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(candReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode candJson = objectMapper.readTree(candRes.getResponse().getContentAsString());
        candidateToken = candJson.get("token").asText();
        Candidate c = candidateRepository.findByUserEmail(candEmail).orElseThrow();
        candidateId = c.getId();

        // 3. Admin creates a Job
        JobRequest jobReq = new JobRequest();
        jobReq.setTitle("Senior Java Backend Engineer");
        jobReq.setCompany("TechCorp Innovations");
        jobReq.setLocation("New York, NY");
        jobReq.setDescription("Looking for a skilled Java Backend Engineer with Spring Boot and Cloud experience.");
        jobReq.setRequiredSkills("Java, Spring Boot, MySQL, REST, Docker");
        jobReq.setExperienceRequired("3+ years");
        jobReq.setSalaryRange("$120k - $150k");
        jobReq.setEmploymentType("Full-time");
        jobReq.setStatus(JobStatus.ACTIVE);

        MvcResult jobRes = mockMvc.perform(post("/api/jobs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jobReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode jobJson = objectMapper.readTree(jobRes.getResponse().getContentAsString());
        jobId = jobJson.get("id").asLong();

        // 4. Admin creates Evaluation Criteria for the Job
        EvaluationCriteriaRequest critReq = new EvaluationCriteriaRequest();
        critReq.setJobId(jobId);
        critReq.setRequiredSkills("Java, Spring Boot, MySQL, Docker");
        critReq.setMinimumExperience(3.0);
        critReq.setEducationRequirements("Bachelor's Degree in Computer Science");
        critReq.setSkillWeight(50.0);
        critReq.setExperienceWeight(30.0);
        critReq.setEducationWeight(20.0);

        MvcResult critRes = mockMvc.perform(post("/api/evaluation-criteria")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(critReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode critJson = objectMapper.readTree(critRes.getResponse().getContentAsString());
        criteriaId = critJson.get("id").asLong();

        // 5. Candidate registers a Resume with extracted text
        ResumeRequest resumeReq = new ResumeRequest();
        resumeReq.setCandidateId(candidateId);
        resumeReq.setFileName("alex_morgan_resume.pdf");
        resumeReq.setFilePath("uploads/resumes/alex_resume.pdf");
        resumeReq.setFileType("application/pdf");
        resumeReq.setFileSize(20480L);
        resumeReq.setExtractedText("""
                Alex Screener
                Full Stack & Backend Software Engineer
                
                Skills:
                Java 21, Spring Boot 3, MySQL, RESTful Web Services, Git, Docker, Hibernate
                
                Experience:
                4 years of professional backend software engineering experience building scalable microservices and APIs.
                
                Education:
                Bachelor of Science in Computer Science and Engineering.
                """);

        MvcResult resumeRes = mockMvc.perform(post("/api/resumes")
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resumeReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode resumeJson = objectMapper.readTree(resumeRes.getResponse().getContentAsString());
        resumeId = resumeJson.get("id").asLong();

        // 6. Candidate applies to the Job
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

        JsonNode appJson = objectMapper.readTree(appRes.getResponse().getContentAsString());
        applicationId = appJson.get("id").asLong();
    }

    @Test
    @Order(1)
    @DisplayName("1. Admin successfully triggers AI screening for an application")
    void testAdminTriggersAiScreening() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/applications/" + applicationId + "/screen")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.applicationId").value(applicationId))
                .andExpect(jsonPath("$.overallScore").isNumber())
                .andExpect(jsonPath("$.skillsScore").isNumber())
                .andExpect(jsonPath("$.experienceScore").isNumber())
                .andExpect(jsonPath("$.educationScore").isNumber())
                .andExpect(jsonPath("$.matchingSkills").isString())
                .andExpect(jsonPath("$.recommendation").isNotEmpty())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        screeningResultId = json.get("id").asLong();

        double overall = json.get("overallScore").asDouble();
        double skills = json.get("skillsScore").asDouble();
        double experience = json.get("experienceScore").asDouble();
        double education = json.get("educationScore").asDouble();

        // Scores must be strictly between 0 and 100
        assertThat(overall).isBetween(0.0, 100.0);
        assertThat(skills).isBetween(0.0, 100.0);
        assertThat(experience).isBetween(0.0, 100.0);
        assertThat(education).isBetween(0.0, 100.0);

        // Verify Database Persistence
        ScreeningResult dbResult = screeningResultRepository.findById(screeningResultId).orElseThrow();
        assertThat(dbResult.getApplication().getId()).isEqualTo(applicationId);
        assertThat(dbResult.getOverallScore()).isEqualTo(overall);
        assertThat(dbResult.getMatchingSkills()).containsIgnoringCase("Java");

        // Verify Application status updated from APPLIED to SHORTLISTED or SCREENING
        Application app = applicationRepository.findById(applicationId).orElseThrow();
        assertThat(app.getStatus()).isIn(ApplicationStatus.SHORTLISTED, ApplicationStatus.SCREENING);
    }

    @Test
    @Order(2)
    @DisplayName("2. Subsequent screening returns existing result without re-evaluating")
    void testSubsequentScreening_ReturnsExisting() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/applications/" + applicationId + "/screen")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(screeningResultId))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("id").asLong()).isEqualTo(screeningResultId);
    }

    @Test
    @Order(3)
    @DisplayName("3. Force re-screening with force=true executes evaluation and updates result")
    void testForceReScreening_Success() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/applications/" + applicationId + "/screen")
                        .param("force", "true")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(screeningResultId))
                .andExpect(jsonPath("$.overallScore").isNumber())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("id").asLong()).isEqualTo(screeningResultId);
    }

    @Test
    @Order(4)
    @DisplayName("4. Candidate attempting to trigger screening is rejected with 403 Forbidden")
    void testCandidateTriggerScreening_Forbidden() throws Exception {
        mockMvc.perform(post("/api/applications/" + applicationId + "/screen")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(5)
    @DisplayName("5. Unauthenticated request to screen is rejected with 401 Unauthorized")
    void testUnauthenticatedScreening_Unauthorized() throws Exception {
        mockMvc.perform(post("/api/applications/" + applicationId + "/screen"))
                .andExpect(status().isUnauthorized());
    }

    @AfterAll
    void cleanup() throws Exception {
        // Delete in correct FK order
        if (criteriaId != null) {
            mockMvc.perform(delete("/api/evaluation-criteria/" + criteriaId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNoContent());
        }

        if (screeningResultId != null) {
            mockMvc.perform(delete("/api/screening/" + screeningResultId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNoContent());
        }

        if (applicationId != null) {
            mockMvc.perform(delete("/api/applications/" + applicationId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNoContent());
        }

        if (resumeId != null) {
            mockMvc.perform(delete("/api/resumes/" + resumeId)
                            .header("Authorization", "Bearer " + candidateToken))
                    .andExpect(status().isNoContent());
        }

        if (jobId != null) {
            mockMvc.perform(delete("/api/jobs/" + jobId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNoContent());
        }
    }
}
