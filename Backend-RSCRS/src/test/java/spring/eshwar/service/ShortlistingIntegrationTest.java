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
public class ShortlistingIntegrationTest {

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

    private String adminToken;
    private String candidateToken;

    private Long jobId;
    private Long cand1Id;
    private Long cand2Id;
    private Long cand3Id;

    private Long app1Id;
    private Long app2Id;
    private Long app3Id;

    private Long res1Id;
    private Long res2Id;
    private Long res3Id;

    @BeforeAll
    void setUp() throws Exception {
        // 1. Admin
        String adminEmail = "admin.shortlist." + System.currentTimeMillis() + "@hireranker.com";
        RegisterRequest adminReq = new RegisterRequest();
        adminReq.setName("Admin Shortlister");
        adminReq.setEmail(adminEmail);
        adminReq.setPassword("AdminPass123!");
        adminReq.setRole(Role.ADMIN);

        MvcResult adminRes = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminReq)))
                .andExpect(status().isCreated())
                .andReturn();
        adminToken = objectMapper.readTree(adminRes.getResponse().getContentAsString()).get("token").asText();

        // 2. Candidate 1 (with screening result)
        String cand1Email = "cand1.shortlist." + System.currentTimeMillis() + "@hireranker.com";
        cand1Id = registerCandidate("Alex Screened", cand1Email);

        // Candidate login token
        RegisterRequest candLogin = new RegisterRequest();
        candLogin.setEmail(cand1Email);
        candLogin.setPassword("CandPass123!");
        MvcResult candAuthRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(candLogin)))
                .andExpect(status().isOk())
                .andReturn();
        candidateToken = objectMapper.readTree(candAuthRes.getResponse().getContentAsString()).get("token").asText();

        // 3. Candidate 2 (no screening result)
        String cand2Email = "cand2.shortlist." + System.currentTimeMillis() + "@hireranker.com";
        cand2Id = registerCandidate("Taylor Unscreened", cand2Email);

        // 4. Candidate 3 (will remain rejected, not shortlisted)
        String cand3Email = "cand3.shortlist." + System.currentTimeMillis() + "@hireranker.com";
        cand3Id = registerCandidate("Jordan Rejected", cand3Email);

        // 5. Job
        JobRequest jobReq = new JobRequest();
        jobReq.setTitle("Staff Software Engineer");
        jobReq.setCompany("FutureTech Global");
        jobReq.setLocation("Bengaluru, India");
        jobReq.setDescription("Building mission critical scalable applications.");
        jobReq.setResponsibilities("Design, develop, and maintain high performance services.");
        jobReq.setRequiredSkills("Java, Spring Boot, Microservices, Cloud");
        jobReq.setExperienceRequired("6+ years");
        jobReq.setStatus(JobStatus.ACTIVE);

        MvcResult jobRes = mockMvc.perform(post("/api/jobs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jobReq)))
                .andExpect(status().isCreated())
                .andReturn();
        jobId = objectMapper.readTree(jobRes.getResponse().getContentAsString()).get("id").asLong();

        // 6. Resumes & Applications
        res1Id = createResume(cand1Id, "alex_resume.pdf");
        app1Id = createApplication(cand1Id, jobId, res1Id);

        res2Id = createResume(cand2Id, "taylor_resume.pdf");
        app2Id = createApplication(cand2Id, jobId, res2Id);

        res3Id = createResume(cand3Id, "jordan_resume.pdf");
        app3Id = createApplication(cand3Id, jobId, res3Id);

        // Attach screening result for Candidate 1
        Application app1 = applicationRepository.findById(app1Id).orElseThrow();
        ScreeningResult sr1 = new ScreeningResult(
                app1, 92.5, 95.0, 90.0, 92.0,
                "Java, Spring Boot, Microservices", "None", "RECOMMENDED"
        );
        screeningResultRepository.save(sr1);
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
        req.setFileSize(102400L);

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

    @AfterAll
    void tearDown() {
        try {
            Long[] appIds = {app1Id, app2Id, app3Id};
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
            Long[] resIds = {res1Id, res2Id, res3Id};
            for (Long rId : resIds) {
                if (rId != null && resumeRepository.existsById(rId)) {
                    resumeRepository.deleteById(rId);
                }
            }
            if (jobId != null && jobRepository.existsById(jobId)) {
                jobRepository.deleteById(jobId);
            }
        } catch (Exception ignored) {
        }
    }

    @Test
    @Order(1)
    @DisplayName("Admin can shortlist an application with completed screening")
    void testShortlistApplicationWithScreeningResult() throws Exception {
        MvcResult res = mockMvc.perform(put("/api/applications/" + app1Id + "/shortlist")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(app1Id))
                .andExpect(jsonPath("$.candidateId").value(cand1Id))
                .andExpect(jsonPath("$.status").value("SHORTLISTED"))
                .andReturn();

        // Verify in database - no duplicate was created
        Application app = applicationRepository.findById(app1Id).orElseThrow();
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.SHORTLISTED);

        long appCount = applicationRepository.findByCandidateId(cand1Id).stream()
                .filter(a -> a.getJob().getId().equals(jobId))
                .count();
        assertThat(appCount).isEqualTo(1);
    }

    @Test
    @Order(2)
    @DisplayName("Shortlisting an unscreened application fails with 400 Bad Request")
    void testShortlistApplicationWithoutScreeningResult() throws Exception {
        // app2Id has NO screening result yet
        mockMvc.perform(put("/api/applications/" + app2Id + "/shortlist")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Screening result must exist before shortlisting the application."));

        // Application status should remain unchanged (APPLIED)
        Application app = applicationRepository.findById(app2Id).orElseThrow();
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
    }

    @Test
    @Order(3)
    @DisplayName("Prevent invalid state transitions: HIRED, INTERVIEW, REJECTED cannot transition to SHORTLISTED")
    void testPreventInvalidStateTransitions() throws Exception {
        // Test REJECTED status
        Application app3 = applicationRepository.findById(app3Id).orElseThrow();
        app3.setStatus(ApplicationStatus.REJECTED);
        applicationRepository.save(app3);

        mockMvc.perform(put("/api/applications/" + app3Id + "/shortlist")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot shortlist an application that has already been REJECTED."));

        // Test INTERVIEW status
        app3.setStatus(ApplicationStatus.INTERVIEW);
        applicationRepository.save(app3);

        mockMvc.perform(put("/api/applications/" + app3Id + "/shortlist")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot shortlist an application that has already advanced to INTERVIEW status."));

        // Test HIRED status
        app3.setStatus(ApplicationStatus.HIRED);
        applicationRepository.save(app3);

        mockMvc.perform(put("/api/applications/" + app3Id + "/shortlist")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot shortlist an application that is already HIRED."));
    }

    @Test
    @Order(4)
    @DisplayName("Shortlisting with invalid application ID returns 404 Not Found")
    void testShortlistInvalidApplicationId() throws Exception {
        mockMvc.perform(put("/api/applications/999999/shortlist")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(5)
    @DisplayName("Screening candidate 2 then shortlisting succeeds")
    void testScreenAndThenShortlist() throws Exception {
        // Create screening result for candidate 2
        Application app2 = applicationRepository.findById(app2Id).orElseThrow();
        ScreeningResult sr2 = new ScreeningResult(
                app2, 88.0, 90.0, 85.0, 89.0,
                "Java, Cloud", "None", "RECOMMENDED"
        );
        screeningResultRepository.save(sr2);

        // Now shortlisting succeeds
        mockMvc.perform(put("/api/applications/" + app2Id + "/shortlist")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(app2Id))
                .andExpect(jsonPath("$.status").value("SHORTLISTED"));

        Application updated = applicationRepository.findById(app2Id).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ApplicationStatus.SHORTLISTED);
    }

    @Test
    @Order(6)
    @DisplayName("Admin can retrieve all shortlisted candidates for a job with candidate, application, resume, and screening scores")
    void testGetShortlistedCandidatesForJob() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/jobs/" + jobId + "/shortlisted")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andReturn();

        String content = res.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(content);

        // Candidate 1
        JsonNode item1 = json.get(0);
        assertThat(item1.get("candidate").get("fullName").asText()).isEqualTo("Alex Screened");
        assertThat(item1.get("application").get("id").asLong()).isEqualTo(app1Id);
        assertThat(item1.get("applicationStatus").asText()).isEqualTo("SHORTLISTED");
        assertThat(item1.get("status").asText()).isEqualTo("SHORTLISTED");
        assertThat(item1.get("resume").get("fileName").asText()).isEqualTo("alex_resume.pdf");
        assertThat(item1.get("screeningScore").asDouble()).isEqualTo(92.5);
        assertThat(item1.get("screeningScores").get("overallScore").asDouble()).isEqualTo(92.5);
        assertThat(item1.get("screeningScores").get("skillsScore").asDouble()).isEqualTo(95.0);

        // Candidate 2
        JsonNode item2 = json.get(1);
        assertThat(item2.get("candidate").get("fullName").asText()).isEqualTo("Taylor Unscreened");
        assertThat(item2.get("application").get("id").asLong()).isEqualTo(app2Id);
        assertThat(item2.get("applicationStatus").asText()).isEqualTo("SHORTLISTED");
        assertThat(item2.get("status").asText()).isEqualTo("SHORTLISTED");
        assertThat(item2.get("resume").get("fileName").asText()).isEqualTo("taylor_resume.pdf");
        assertThat(item2.get("screeningScore").asDouble()).isEqualTo(88.0);
        assertThat(item2.get("screeningScores").get("overallScore").asDouble()).isEqualTo(88.0);
        assertThat(item2.get("screeningScores").get("skillsScore").asDouble()).isEqualTo(90.0);
    }

    @Test
    @Order(7)
    @DisplayName("Non-existent job returns 404 when querying shortlisted candidates")
    void testGetShortlistedInvalidJobId() throws Exception {
        mockMvc.perform(get("/api/jobs/999999/shortlisted")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(8)
    @DisplayName("Candidate user receives 403 Forbidden on shortlisting endpoints")
    void testCandidateForbiddenOnShortlisting() throws Exception {
        mockMvc.perform(put("/api/applications/" + app1Id + "/shortlist")
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/jobs/" + jobId + "/shortlisted")
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(9)
    @DisplayName("Unauthenticated caller receives 401 Unauthorized")
    void testUnauthenticatedOnShortlisting() throws Exception {
        mockMvc.perform(put("/api/applications/" + app1Id + "/shortlist")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/jobs/" + jobId + "/shortlisted")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status).isIn(401, 403);
                });
    }
}
