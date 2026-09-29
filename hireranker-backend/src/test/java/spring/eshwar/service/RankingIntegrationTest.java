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
import spring.eshwar.dto.job.JobRequest;
import spring.eshwar.dto.ranking.RankingResponse;
import spring.eshwar.dto.resume.ResumeRequest;
import spring.eshwar.entity.Application;
import spring.eshwar.entity.ApplicationStatus;
import spring.eshwar.entity.Candidate;
import spring.eshwar.entity.JobStatus;
import spring.eshwar.entity.Role;
import spring.eshwar.entity.ScreeningResult;
import spring.eshwar.repository.ApplicationRepository;
import spring.eshwar.repository.CandidateRepository;
import spring.eshwar.repository.JobRepository;
import spring.eshwar.repository.ResumeRepository;
import spring.eshwar.repository.ScreeningResultRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RankingIntegrationTest {

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
    private ScreeningResultRepository screeningResultRepository;

    @Autowired
    private RankingService rankingService;

    private String adminToken;
    private String candidateToken;

    private Long job1Id;
    private Long jobEmptyId;

    private Long cand1Id;
    private Long cand2Id;
    private Long cand3Id;
    private Long cand4Id;

    private Long app1Id;
    private Long app2Id;
    private Long app3Id;
    private Long app4Id;

    private Long res1Id;
    private Long res2Id;
    private Long res3Id;
    private Long res4Id;

    @BeforeAll
    void setUp() throws Exception {
        // 1. Register Admin
        String adminEmail = "admin.rank." + System.currentTimeMillis() + "@hireranker.com";
        RegisterRequest adminReq = new RegisterRequest();
        adminReq.setName("Admin Ranker");
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

        // 2. Register Candidate 1 (HighScore)
        String cand1Email = "cand1.rank." + System.currentTimeMillis() + "@hireranker.com";
        cand1Id = registerCandidate("Eshwar Rao", cand1Email);

        // 2b. Candidate 1 Token (for authorization test)
        RegisterRequest candLogin = new RegisterRequest();
        candLogin.setEmail(cand1Email);
        candLogin.setPassword("CandPass123!");
        MvcResult candAuthRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(candLogin)))
                .andExpect(status().isOk())
                .andReturn();
        candidateToken = objectMapper.readTree(candAuthRes.getResponse().getContentAsString()).get("token").asText();

        // 3. Register Candidate 2 (MidScore)
        String cand2Email = "cand2.rank." + System.currentTimeMillis() + "@hireranker.com";
        cand2Id = registerCandidate("Krupa Jyothi", cand2Email);

        // 4. Register Candidate 3 (LowScore)
        String cand3Email = "cand3.rank." + System.currentTimeMillis() + "@hireranker.com";
        cand3Id = registerCandidate("Durga Rohith", cand3Email);

        // 5. Register Candidate 4 (Unscreened)
        String cand4Email = "cand4.rank." + System.currentTimeMillis() + "@hireranker.com";
        cand4Id = registerCandidate("Sowmya Maloth", cand4Email);

        // 6. Admin creates Job 1
        JobRequest job1Req = new JobRequest();
        job1Req.setTitle("Lead Java Full Stack Developer");
        job1Req.setCompany("Tech Giants Corp");
        job1Req.setLocation("Hyderabad, India");
        job1Req.setDescription("Building high-throughput microservices and Angular frontends.");
        job1Req.setRequiredSkills("Java 21, Spring Boot 3, Angular 17, Docker, Kubernetes, MySQL");
        job1Req.setExperienceRequired("5+ years");
        job1Req.setSalaryRange("₹25L - ₹35L");
        job1Req.setEmploymentType("Full-time");
        job1Req.setStatus(JobStatus.ACTIVE);

        MvcResult job1Res = mockMvc.perform(post("/api/jobs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(job1Req)))
                .andExpect(status().isCreated())
                .andReturn();
        job1Id = objectMapper.readTree(job1Res.getResponse().getContentAsString()).get("id").asLong();

        // 7. Admin creates Job 2 (Empty Job)
        JobRequest jobEmptyReq = new JobRequest();
        jobEmptyReq.setTitle("Cloud DevOps Architect");
        jobEmptyReq.setCompany("CloudScale Inc");
        jobEmptyReq.setLocation("Remote");
        jobEmptyReq.setDescription("Zero application job for testing empty ranking lists.");
        jobEmptyReq.setRequiredSkills("AWS, Terraform, Kubernetes");
        jobEmptyReq.setExperienceRequired("4+ years");
        jobEmptyReq.setEmploymentType("Full-time");
        jobEmptyReq.setStatus(JobStatus.ACTIVE);

        MvcResult jobEmptyRes = mockMvc.perform(post("/api/jobs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jobEmptyReq)))
                .andExpect(status().isCreated())
                .andReturn();
        jobEmptyId = objectMapper.readTree(jobEmptyRes.getResponse().getContentAsString()).get("id").asLong();

        // 8. Create Resumes & Applications for Candidates 1 to 4 on Job 1
        res1Id = createResume(cand1Id, "eshwar_resume.pdf");
        app1Id = createApplication(cand1Id, job1Id, res1Id);

        res2Id = createResume(cand2Id, "krupa_resume.pdf");
        app2Id = createApplication(cand2Id, job1Id, res2Id);

        res3Id = createResume(cand3Id, "durga_resume.pdf");
        app3Id = createApplication(cand3Id, job1Id, res3Id);

        res4Id = createResume(cand4Id, "sowmya_resume.pdf");
        app4Id = createApplication(cand4Id, job1Id, res4Id);

        // 9. Attach ScreeningResults to Candidates 1, 2, 3 (Leave Candidate 4 unscreened)
        createScreeningResult(app1Id, 95.0, 96.0, 94.0, 95.0, "RECOMMENDED", ApplicationStatus.SHORTLISTED);
        createScreeningResult(app2Id, 88.0, 90.0, 85.0, 89.0, "RECOMMENDED", ApplicationStatus.SHORTLISTED);
        createScreeningResult(app3Id, 72.0, 75.0, 70.0, 71.0, "NOT_RECOMMENDED", ApplicationStatus.REJECTED);
    }

    private Long registerCandidate(String name, String email) throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setName(name);
        req.setEmail(email);
        req.setPassword("CandPass123!");
        req.setRole(Role.CANDIDATE);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        Candidate c = candidateRepository.findByUserEmail(email).orElseThrow();
        return c.getId();
    }

    private Long createResume(Long candidateId, String fileName) throws Exception {
        ResumeRequest req = new ResumeRequest();
        req.setCandidateId(candidateId);
        req.setFileName(fileName);
        req.setFileType("application/pdf");
        req.setFilePath("/uploads/resumes/" + fileName);
        req.setFileSize(204800L);
        req.setExtractedText("Skills: Java, Spring Boot, MySQL, Cloud");

        MvcResult res = mockMvc.perform(post("/api/resumes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();
    }

    private Long createApplication(Long candidateId, Long jobId, Long resumeId) throws Exception {
        ApplicationRequest req = new ApplicationRequest();
        req.setCandidateId(candidateId);
        req.setJobId(jobId);
        req.setResumeId(resumeId);

        MvcResult res = mockMvc.perform(post("/api/applications")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();
    }

    private void createScreeningResult(Long applicationId, Double overall, Double skills,
                                       Double experience, Double education, String recommendation,
                                       ApplicationStatus newStatus) {
        Application app = applicationRepository.findById(applicationId).orElseThrow();
        ScreeningResult sr = new ScreeningResult(
                app, overall, skills, experience, education,
                "Java, Spring Boot, Microservices", "AWS", recommendation
        );
        screeningResultRepository.save(sr);

        app.setStatus(newStatus);
        applicationRepository.save(app);
    }

    @AfterAll
    void tearDown() {
        try {
            Long[] appIds = {app1Id, app2Id, app3Id, app4Id};
            for (Long aId : appIds) {
                if (aId != null) {
                    screeningResultRepository.findByApplicationId(aId)
                            .ifPresent(screeningResultRepository::delete);
                }
            }
            for (Long aId : appIds) {
                if (aId != null && applicationRepository.existsById(aId)) {
                    applicationRepository.deleteById(aId);
                }
            }
            Long[] resIds = {res1Id, res2Id, res3Id, res4Id};
            for (Long rId : resIds) {
                if (rId != null && resumeRepository.existsById(rId)) {
                    resumeRepository.deleteById(rId);
                }
            }
            if (job1Id != null && jobRepository.existsById(job1Id)) {
                jobRepository.deleteById(job1Id);
            }
            if (jobEmptyId != null && jobRepository.existsById(jobEmptyId)) {
                jobRepository.deleteById(jobEmptyId);
            }
        } catch (Exception ignored) {
        }
    }

    @Test
    @Order(1)
    @DisplayName("Admin can retrieve candidate rankings sorted descending with assigned ranks")
    void testCandidateRankingDescendingOrderAndRanks() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/jobs/" + job1Id + "/ranking")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andReturn();

        String content = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(content);

        // Rank 1: Eshwar Rao (95.0)
        JsonNode rank1 = json.get(0);
        assertThat(rank1.get("rank").asInt()).isEqualTo(1);
        assertThat(rank1.get("candidateId").asLong()).isEqualTo(cand1Id);
        assertThat(rank1.get("candidateName").asText()).isEqualTo("Eshwar Rao");
        assertThat(rank1.get("applicationId").asLong()).isEqualTo(app1Id);
        assertThat(rank1.get("overallScore").asDouble()).isEqualTo(95.0);
        assertThat(rank1.get("skillsScore").asDouble()).isEqualTo(96.0);
        assertThat(rank1.get("experienceScore").asDouble()).isEqualTo(94.0);
        assertThat(rank1.get("educationScore").asDouble()).isEqualTo(95.0);
        assertThat(rank1.get("applicationStatus").asText()).isEqualTo("SHORTLISTED");

        // Rank 2: Krupa Jyothi (88.0)
        JsonNode rank2 = json.get(1);
        assertThat(rank2.get("rank").asInt()).isEqualTo(2);
        assertThat(rank2.get("candidateId").asLong()).isEqualTo(cand2Id);
        assertThat(rank2.get("candidateName").asText()).isEqualTo("Krupa Jyothi");
        assertThat(rank2.get("applicationId").asLong()).isEqualTo(app2Id);
        assertThat(rank2.get("overallScore").asDouble()).isEqualTo(88.0);
        assertThat(rank2.get("applicationStatus").asText()).isEqualTo("SHORTLISTED");

        // Rank 3: Durga Rohith (72.0)
        JsonNode rank3 = json.get(2);
        assertThat(rank3.get("rank").asInt()).isEqualTo(3);
        assertThat(rank3.get("candidateId").asLong()).isEqualTo(cand3Id);
        assertThat(rank3.get("candidateName").asText()).isEqualTo("Durga Rohith");
        assertThat(rank3.get("applicationId").asLong()).isEqualTo(app3Id);
        assertThat(rank3.get("overallScore").asDouble()).isEqualTo(72.0);
        assertThat(rank3.get("applicationStatus").asText()).isEqualTo("REJECTED");

        // Rank 4: Sowmya Maloth (Unscreened -> null scores)
        JsonNode rank4 = json.get(3);
        assertThat(rank4.get("rank").asInt()).isEqualTo(4);
        assertThat(rank4.get("candidateId").asLong()).isEqualTo(cand4Id);
        assertThat(rank4.get("candidateName").asText()).isEqualTo("Sowmya Maloth");
        assertThat(rank4.get("applicationId").asLong()).isEqualTo(app4Id);
        assertThat(rank4.get("overallScore").isNull()).isTrue();
        assertThat(rank4.get("skillsScore").isNull()).isTrue();
        assertThat(rank4.get("experienceScore").isNull()).isTrue();
        assertThat(rank4.get("educationScore").isNull()).isTrue();
        assertThat(rank4.get("applicationStatus").asText()).isEqualTo("APPLIED");
    }

    @Test
    @Order(2)
    @DisplayName("Empty applications returns empty list with 200 OK")
    void testRankingEmptyApplications() throws Exception {
        mockMvc.perform(get("/api/jobs/" + jobEmptyId + "/ranking")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @Order(3)
    @DisplayName("Non-existent job returns 404 Not Found")
    void testRankingNonExistentJob() throws Exception {
        mockMvc.perform(get("/api/jobs/999999/ranking")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(4)
    @DisplayName("Candidate user receives 403 Forbidden on ranking endpoint")
    void testCandidateForbiddenOnRanking() throws Exception {
        mockMvc.perform(get("/api/jobs/" + job1Id + "/ranking")
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(5)
    @DisplayName("Unauthenticated request receives 401 Unauthorized or 403 Forbidden")
    void testUnauthenticatedOnRanking() throws Exception {
        mockMvc.perform(get("/api/jobs/" + job1Id + "/ranking")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status).isIn(401, 403);
                });
    }

    @Test
    @Order(6)
    @DisplayName("RankingService direct call produces correctly ordered responses with zero AI invocation")
    void testRankingServiceDirect() {
        List<RankingResponse> responses = rankingService.getRankingForJob(job1Id);
        assertThat(responses).hasSize(4);

        assertThat(responses.get(0).getRank()).isEqualTo(1);
        assertThat(responses.get(0).getOverallScore()).isEqualTo(95.0);

        assertThat(responses.get(1).getRank()).isEqualTo(2);
        assertThat(responses.get(1).getOverallScore()).isEqualTo(88.0);

        assertThat(responses.get(2).getRank()).isEqualTo(3);
        assertThat(responses.get(2).getOverallScore()).isEqualTo(72.0);

        assertThat(responses.get(3).getRank()).isEqualTo(4);
        assertThat(responses.get(3).getOverallScore()).isNull();
    }

    @Test
    @Order(7)
    @DisplayName("Candidates with equal overall scores are ranked consistently using secondary criteria")
    void testEqualScoresHandledConsistently() throws Exception {
        // Create job for tie-breaker testing
        JobRequest tieJobReq = new JobRequest();
        tieJobReq.setTitle("Tie Breaker Test Job");
        tieJobReq.setCompany("Tie Corp");
        tieJobReq.setLocation("Remote");
        tieJobReq.setDescription("Testing deterministic equal score ranking.");
        tieJobReq.setRequiredSkills("Java, Spring Boot");
        tieJobReq.setExperienceRequired("3+ years");
        tieJobReq.setEmploymentType("Full-time");
        tieJobReq.setStatus(JobStatus.ACTIVE);

        MvcResult jobRes = mockMvc.perform(post("/api/jobs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tieJobReq)))
                .andExpect(status().isCreated())
                .andReturn();
        Long tieJobId = objectMapper.readTree(jobRes.getResponse().getContentAsString()).get("id").asLong();

        Long tieApp1Id = null;
        Long tieApp2Id = null;

        try {
            // Apply Cand1 and Cand2 to tieJob
            tieApp1Id = createApplication(cand1Id, tieJobId, res1Id);
            tieApp2Id = createApplication(cand2Id, tieJobId, res2Id);

            // Both have overallScore = 85.0. Cand1 has higher skillsScore (92.0 vs 82.0)
            createScreeningResult(tieApp1Id, 85.0, 92.0, 80.0, 85.0, "RECOMMENDED", ApplicationStatus.SHORTLISTED);
            createScreeningResult(tieApp2Id, 85.0, 82.0, 90.0, 85.0, "RECOMMENDED", ApplicationStatus.SHORTLISTED);

            List<RankingResponse> rankings = rankingService.getRankingForJob(tieJobId);
            assertThat(rankings).hasSize(2);

            // Rank 1: Cand1 (skillsScore 92.0 beats 82.0)
            assertThat(rankings.get(0).getRank()).isEqualTo(1);
            assertThat(rankings.get(0).getCandidateId()).isEqualTo(cand1Id);
            assertThat(rankings.get(0).getOverallScore()).isEqualTo(85.0);
            assertThat(rankings.get(0).getSkillsScore()).isEqualTo(92.0);

            // Rank 2: Cand2
            assertThat(rankings.get(1).getRank()).isEqualTo(2);
            assertThat(rankings.get(1).getCandidateId()).isEqualTo(cand2Id);
            assertThat(rankings.get(1).getOverallScore()).isEqualTo(85.0);
            assertThat(rankings.get(1).getSkillsScore()).isEqualTo(82.0);
        } finally {
            if (tieApp1Id != null) {
                screeningResultRepository.findByApplicationId(tieApp1Id).ifPresent(screeningResultRepository::delete);
                if (applicationRepository.existsById(tieApp1Id)) {
                    applicationRepository.deleteById(tieApp1Id);
                }
            }
            if (tieApp2Id != null) {
                screeningResultRepository.findByApplicationId(tieApp2Id).ifPresent(screeningResultRepository::delete);
                if (applicationRepository.existsById(tieApp2Id)) {
                    applicationRepository.deleteById(tieApp2Id);
                }
            }
            if (tieJobId != null && jobRepository.existsById(tieJobId)) {
                jobRepository.deleteById(tieJobId);
            }
        }
    }
}
