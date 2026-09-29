package spring.eshwar.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import spring.eshwar.dto.LoginRequest;
import spring.eshwar.dto.RegisterRequest;
import spring.eshwar.dto.application.ApplicationRequest;
import spring.eshwar.dto.candidate.CandidateRequest;
import spring.eshwar.dto.evaluation.EvaluationCriteriaRequest;
import spring.eshwar.dto.interview.InterviewRequest;
import spring.eshwar.dto.job.JobRequest;
import spring.eshwar.dto.message.MessageRequest;
import spring.eshwar.dto.resume.ResumeRequest;
import spring.eshwar.dto.screening.ScreeningRequest;
import spring.eshwar.entity.InterviewStatus;
import spring.eshwar.entity.JobStatus;
import spring.eshwar.entity.Role;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RestControllersIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String candidateToken;
    private Long adminUserId;
    private Long candidateUserId;
    private Long candidateId;
    private Long jobId;
    private Long resumeId;
    private Long applicationId;
    private Long screeningId;
    private Long criteriaId;
    private Long interviewId;
    private Long messageId;

    private String adminEmail;
    private String candidateEmail;

    @BeforeAll
    void setupTokens() throws Exception {
        // 1. Register and login Admin
        adminEmail = "admin.rest." + System.currentTimeMillis() + "@hireranker.com";
        RegisterRequest adminReg = new RegisterRequest();
        adminReg.setName("Admin User");
        adminReg.setEmail(adminEmail);
        adminReg.setPassword("AdminPass123!");
        adminReg.setRole(Role.ADMIN);

        MvcResult adminRegRes = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminReg)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode adminJson = objectMapper.readTree(adminRegRes.getResponse().getContentAsString());
        adminToken = adminJson.get("token").asText();
        adminUserId = adminJson.get("id").asLong();

        // 2. Register and login Candidate
        candidateEmail = "cand.rest." + System.currentTimeMillis() + "@hireranker.com";
        RegisterRequest candReg = new RegisterRequest();
        candReg.setName("Candidate User");
        candReg.setEmail(candidateEmail);
        candReg.setPassword("CandPass123!");
        candReg.setRole(Role.CANDIDATE);

        MvcResult candRegRes = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(candReg)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode candJson = objectMapper.readTree(candRegRes.getResponse().getContentAsString());
        candidateToken = candJson.get("token").asText();
        candidateUserId = candJson.get("id").asLong();

        // Get candidate entity ID from profile
        MvcResult profRes = mockMvc.perform(get("/api/candidates/profile/" + candidateUserId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode profJson = objectMapper.readTree(profRes.getResponse().getContentAsString());
        candidateId = profJson.get("id").asLong();
    }

    @Test
    @Order(1)
    @DisplayName("AuthController: Login with valid credentials returns JWT")
    void testAuthLogin() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail(adminEmail);
        request.setPassword("AdminPass123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @Order(2)
    @DisplayName("JobController: Admin creates job (201), public reads (200), admin updates (200)")
    void testJobEndpoints() throws Exception {
        JobRequest jobReq = new JobRequest();
        jobReq.setTitle("Senior Full Stack Engineer");
        jobReq.setCompany("HireRanker Tech");
        jobReq.setDescription("Building scalable AI hiring platform");
        jobReq.setRequiredSkills("Java, Spring Boot, Angular, MySQL");
        jobReq.setExperienceRequired("4+ years");
        jobReq.setLocation("Remote");
        jobReq.setSalaryRange("$120k - $150k");
        jobReq.setEmploymentType("Full-time");
        jobReq.setStatus(JobStatus.ACTIVE);

        MvcResult createRes = mockMvc.perform(post("/api/jobs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jobReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Senior Full Stack Engineer"))
                .andReturn();

        JsonNode json = objectMapper.readTree(createRes.getResponse().getContentAsString());
        jobId = json.get("id").asLong();
        assertThat(jobId).isNotNull();

        // Public GET /api/jobs
        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Public GET /api/jobs/{id}
        mockMvc.perform(get("/api/jobs/" + jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(jobId));

        // Admin PUT /api/jobs/{id}
        jobReq.setTitle("Lead Full Stack Engineer");
        mockMvc.perform(put("/api/jobs/" + jobId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jobReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Lead Full Stack Engineer"));
    }

    @Test
    @Order(3)
    @DisplayName("CandidateController: GET and PUT candidate by ID")
    void testCandidateEndpoints() throws Exception {
        // GET /api/candidates/{id}
        mockMvc.perform(get("/api/candidates/" + candidateId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(candidateId));

        // PUT /api/candidates/{id}
        CandidateRequest updateReq = new CandidateRequest();
        updateReq.setUserId(candidateUserId);
        updateReq.setFullName("Jane Candidate Updated");
        updateReq.setSkills("Java, Angular, Spring Boot, Docker");
        updateReq.setLocation("San Francisco, CA");
        updateReq.setExperience("5 years");

        mockMvc.perform(put("/api/candidates/" + candidateId)
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Jane Candidate Updated"))
                .andExpect(jsonPath("$.location").value("San Francisco, CA"));
    }

    @Test
    @Order(4)
    @DisplayName("ResumeController: POST metadata, GET by ID and candidate")
    void testResumeEndpoints() throws Exception {
        ResumeRequest resumeReq = new ResumeRequest();
        resumeReq.setCandidateId(candidateId);
        resumeReq.setFileName("resume_jane.pdf");
        resumeReq.setFilePath("/uploads/resumes/resume_jane.pdf");
        resumeReq.setFileType("application/pdf");
        resumeReq.setFileSize(2048576L);
        resumeReq.setExtractedText("Experienced software engineer with Java and Angular expertise.");

        MvcResult createRes = mockMvc.perform(post("/api/resumes")
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resumeReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("resume_jane.pdf"))
                .andReturn();

        JsonNode json = objectMapper.readTree(createRes.getResponse().getContentAsString());
        resumeId = json.get("id").asLong();

        // GET /api/resumes/{id}
        mockMvc.perform(get("/api/resumes/" + resumeId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(resumeId));

        // GET /api/resumes/candidate/{candidateId}
        mockMvc.perform(get("/api/resumes/candidate/" + candidateId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(5)
    @DisplayName("ApplicationController: Candidate applies to job, reads application")
    void testApplicationEndpoints() throws Exception {
        ApplicationRequest appReq = new ApplicationRequest();
        appReq.setCandidateId(candidateId);
        appReq.setJobId(jobId);
        appReq.setResumeId(resumeId);

        MvcResult applyRes = mockMvc.perform(post("/api/applications")
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(appReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.candidateId").value(candidateId))
                .andExpect(jsonPath("$.jobId").value(jobId))
                .andReturn();

        JsonNode json = objectMapper.readTree(applyRes.getResponse().getContentAsString());
        applicationId = json.get("id").asLong();

        // GET /api/applications/{id}
        mockMvc.perform(get("/api/applications/" + applicationId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(applicationId));

        // GET /api/applications/candidate/{candidateId}
        mockMvc.perform(get("/api/applications/candidate/" + candidateId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // GET /api/applications/job/{jobId}
        mockMvc.perform(get("/api/applications/job/" + jobId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(6)
    @DisplayName("ScreeningController: Save screening, get by applicationId and ID")
    void testScreeningEndpoints() throws Exception {
        ScreeningRequest screeningReq = new ScreeningRequest();
        screeningReq.setApplicationId(applicationId);
        screeningReq.setOverallScore(88.5);
        screeningReq.setSkillsScore(90.0);
        screeningReq.setExperienceScore(85.0);
        screeningReq.setEducationScore(90.0);
        screeningReq.setMatchingSkills("Java, Spring Boot, Angular");
        screeningReq.setMissingSkills("Kubernetes");
        screeningReq.setRecommendation("RECOMMENDED");

        MvcResult res = mockMvc.perform(post("/api/screening")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(screeningReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.overallScore").value(88.5))
                .andReturn();

        JsonNode json = objectMapper.readTree(res.getResponse().getContentAsString());
        screeningId = json.get("id").asLong();

        // GET /api/applications/{applicationId}/screening
        mockMvc.perform(get("/api/applications/" + applicationId + "/screening")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallScore").value(88.5));

        // GET /api/screening/{id}
        mockMvc.perform(get("/api/screening/" + screeningId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(screeningId));
    }

    @Test
    @Order(7)
    @DisplayName("EvaluationCriteriaController: POST, GET by job, PUT criteria")
    void testEvaluationCriteriaEndpoints() throws Exception {
        EvaluationCriteriaRequest critReq = new EvaluationCriteriaRequest();
        critReq.setJobId(jobId);
        critReq.setRequiredSkills("Java, Spring Boot, Angular");
        critReq.setMinimumExperience(3.0);
        critReq.setEducationRequirements("Bachelor's in Computer Science");
        critReq.setSkillWeight(40.0);
        critReq.setExperienceWeight(30.0);
        critReq.setEducationWeight(30.0);

        MvcResult res = mockMvc.perform(post("/api/evaluation-criteria")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(critReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.skillWeight").value(40.0))
                .andReturn();

        JsonNode json = objectMapper.readTree(res.getResponse().getContentAsString());
        criteriaId = json.get("id").asLong();

        // GET /api/evaluation-criteria/job/{jobId}
        mockMvc.perform(get("/api/evaluation-criteria/job/" + jobId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(jobId));

        // PUT /api/evaluation-criteria/{id}
        critReq.setSkillWeight(50.0);
        critReq.setExperienceWeight(25.0);
        critReq.setEducationWeight(25.0);

        mockMvc.perform(put("/api/evaluation-criteria/" + criteriaId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(critReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skillWeight").value(50.0));
    }

    @Test
    @Order(8)
    @DisplayName("InterviewController: Schedule, get by application and ID, update interview")
    void testInterviewEndpoints() throws Exception {
        // Shortlist application before scheduling interview
        mockMvc.perform(put("/api/applications/" + applicationId + "/shortlist")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        InterviewRequest intReq = new InterviewRequest();
        intReq.setApplicationId(applicationId);
        intReq.setScheduledDateTime(LocalDateTime.now().plusDays(2));
        intReq.setInterviewType("ONLINE");
        intReq.setMeetingLink("https://meet.hireranker.com/tech-round-1");
        intReq.setNotes("Focus on system design and Spring Boot concurrency");
        intReq.setStatus(InterviewStatus.SCHEDULED);

        MvcResult res = mockMvc.perform(post("/api/interviews")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(intReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.interviewType").value("ONLINE"))
                .andReturn();

        JsonNode json = objectMapper.readTree(res.getResponse().getContentAsString());
        interviewId = json.get("id").asLong();

        // GET /api/interviews/{id}
        mockMvc.perform(get("/api/interviews/" + interviewId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(interviewId));

        // GET /api/interviews/application/{applicationId}
        mockMvc.perform(get("/api/interviews/application/" + applicationId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(9)
    @DisplayName("MessageController: Send message, get conversation, mark read")
    void testMessageEndpoints() throws Exception {
        MessageRequest msgReq = new MessageRequest();
        msgReq.setSenderId(adminUserId);
        msgReq.setReceiverId(candidateUserId);
        msgReq.setMessage("Congratulations! Your interview has been scheduled.");

        MvcResult res = mockMvc.perform(post("/api/messages")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msgReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Congratulations! Your interview has been scheduled."))
                .andReturn();

        JsonNode json = objectMapper.readTree(res.getResponse().getContentAsString());
        messageId = json.get("id").asLong();

        // GET /api/messages/conversation/{user1Id}/{user2Id}
        mockMvc.perform(get("/api/messages/conversation/" + adminUserId + "/" + candidateUserId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // PUT /api/messages/{id}/read
        mockMvc.perform(put("/api/messages/" + messageId + "/read")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readStatus").value("READ"));
    }

    @Test
    @Order(10)
    @DisplayName("AdminController: Dashboard stats and users list")
    void testAdminEndpoints() throws Exception {
        // GET /api/admin/dashboard
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").isNumber())
                .andExpect(jsonPath("$.totalJobs").isNumber());

        // GET /api/admin/users
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Unauthorized access check: Candidate attempting to access /api/admin/dashboard returns 403 Forbidden
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(11)
    @DisplayName("Cleanup: Admin and Candidate delete test resources")
    void testDeletions() throws Exception {
        // First delete criteria, interview, screening, and application referencing job and resume
        mockMvc.perform(delete("/api/evaluation-criteria/" + criteriaId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/interviews/" + interviewId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/screening/" + screeningId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/applications/" + applicationId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // DELETE /api/resumes/{id}
        mockMvc.perform(delete("/api/resumes/" + resumeId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isNoContent());

        // DELETE /api/jobs/{id}
        mockMvc.perform(delete("/api/jobs/" + jobId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }
}
