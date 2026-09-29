package spring.eshwar.controller;

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
import spring.eshwar.dto.LoginRequest;
import spring.eshwar.dto.RegisterRequest;
import spring.eshwar.dto.application.ApplicationRequest;
import spring.eshwar.dto.candidate.CandidateProfileUpdateRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprehensive integration test executing every single endpoint in the
 * HireRanker Postman collection (HireRanker_Postman_Collection.json)
 * across all 12 modules.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PostmanEndpointsFullSuiteTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String candidateToken;
    private String adminToken;
    private Long candidateUserId;
    private Long adminUserId;
    private Long candidateId;
    private Long jobId;
    private Long resumeId;
    private Long applicationId;
    private Long screeningId;
    private Long criteriaId;
    private Long interviewId;
    private Long messageId;

    private String candidateEmail = "pm.candidate." + System.currentTimeMillis() + "@hireranker.com";
    private String adminEmail = "pm.admin." + System.currentTimeMillis() + "@hireranker.com";

    // =========================================================================
    // 1. Authentication
    // =========================================================================
    @Test
    @Order(1)
    @DisplayName("Folder 1: Authentication - Register Candidate, Register Admin, Login")
    void testFolder01_Authentication() throws Exception {
        // Register Candidate
        RegisterRequest candReg = new RegisterRequest();
        candReg.setName("Jane Doe");
        candReg.setEmail(candidateEmail);
        candReg.setPassword("Password123!");
        candReg.setRole(Role.CANDIDATE);

        MvcResult candRegRes = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(candReg)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("CANDIDATE"))
                .andReturn();

        JsonNode candRegJson = objectMapper.readTree(candRegRes.getResponse().getContentAsString());
        candidateUserId = candRegJson.get("id").asLong();

        // Register Admin
        RegisterRequest adminReg = new RegisterRequest();
        adminReg.setName("Admin Manager");
        adminReg.setEmail(adminEmail);
        adminReg.setPassword("AdminPass123!");
        adminReg.setRole(Role.ADMIN);

        MvcResult adminRegRes = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminReg)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn();

        JsonNode adminRegJson = objectMapper.readTree(adminRegRes.getResponse().getContentAsString());
        adminUserId = adminRegJson.get("id").asLong();

        // Login Candidate
        LoginRequest candLogin = new LoginRequest();
        candLogin.setEmail(candidateEmail);
        candLogin.setPassword("Password123!");

        MvcResult candLoginRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(candLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        JsonNode candLoginJson = objectMapper.readTree(candLoginRes.getResponse().getContentAsString());
        candidateToken = candLoginJson.get("token").asText();

        // Login Admin
        LoginRequest adminLogin = new LoginRequest();
        adminLogin.setEmail(adminEmail);
        adminLogin.setPassword("AdminPass123!");

        MvcResult adminLoginRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        JsonNode adminLoginJson = objectMapper.readTree(adminLoginRes.getResponse().getContentAsString());
        adminToken = adminLoginJson.get("token").asText();
    }

    // =========================================================================
    // 2. Candidates
    // =========================================================================
    @Test
    @Order(2)
    @DisplayName("Folder 2: Candidates - Dashboard, Profile (Me), Update (Me), Get by ID, Update by ID, Get All")
    void testFolder02_Candidates() throws Exception {
        // Get Candidate Profile (Me)
        MvcResult meRes = mockMvc.perform(get("/api/candidates/me")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Jane Doe"))
                .andReturn();

        JsonNode meJson = objectMapper.readTree(meRes.getResponse().getContentAsString());
        candidateId = meJson.get("id").asLong();
        assertThat(candidateId).isNotNull();

        // Get Candidate Dashboard (Me)
        mockMvc.perform(get("/api/candidates/me/dashboard")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateProfile").exists());

        // Update Candidate Profile (Me)
        CandidateProfileUpdateRequest updateMeReq = new CandidateProfileUpdateRequest();
        updateMeReq.setFullName("Jane Doe Updated");
        updateMeReq.setPhone("+1-555-0199");
        updateMeReq.setLocation("San Francisco, CA");
        updateMeReq.setSkills("Java, Spring Boot, Angular, TypeScript, AWS");
        updateMeReq.setExperience("5 years");
        updateMeReq.setEducation("B.S. Computer Science");
        updateMeReq.setGithub("https://github.com/janedoe");
        updateMeReq.setLinkedin("https://linkedin.com/in/janedoe");

        mockMvc.perform(put("/api/candidates/me")
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateMeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Jane Doe Updated"))
                .andExpect(jsonPath("$.location").value("San Francisco, CA"));

        // Get Candidate by ID
        mockMvc.perform(get("/api/candidates/" + candidateId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(candidateId));

        // Update Candidate Profile by ID
        CandidateRequest updateByIdReq = new CandidateRequest();
        updateByIdReq.setUserId(candidateUserId);
        updateByIdReq.setFullName("Jane Doe Final");
        updateByIdReq.setSkills("Java, Spring Boot, Angular");
        updateByIdReq.setLocation("San Francisco, CA");

        mockMvc.perform(put("/api/candidates/" + candidateId)
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateByIdReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Jane Doe Final"));

        // Get All Candidates (Admin)
        mockMvc.perform(get("/api/candidates")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // =========================================================================
    // 3. Jobs
    // =========================================================================
    @Test
    @Order(3)
    @DisplayName("Folder 3: Jobs - Create Job, Get All, Get by ID, Update Job")
    void testFolder03_Jobs() throws Exception {
        // Create Job (Admin)
        JobRequest jobReq = new JobRequest();
        jobReq.setTitle("Senior Java Developer");
        jobReq.setCompany("Acme Corp");
        jobReq.setLocation("Remote");
        jobReq.setDescription("Looking for a Senior Java Developer with Spring Boot and Cloud experience.");
        jobReq.setRequiredSkills("Java, Spring Boot, MySQL, REST, Docker");
        jobReq.setExperienceRequired("3+ years");
        jobReq.setSalaryRange("$110k - $140k");
        jobReq.setEmploymentType("Full-time");
        jobReq.setStatus(JobStatus.ACTIVE);

        MvcResult createJobRes = mockMvc.perform(post("/api/jobs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jobReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Senior Java Developer"))
                .andReturn();

        JsonNode jobJson = objectMapper.readTree(createJobRes.getResponse().getContentAsString());
        jobId = jobJson.get("id").asLong();
        assertThat(jobId).isNotNull();

        // Get All Jobs (Public)
        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Get Job by ID (Public)
        mockMvc.perform(get("/api/jobs/" + jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(jobId));

        // Update Job (Admin)
        jobReq.setTitle("Lead Java Engineer");
        jobReq.setLocation("New York, NY (Hybrid)");
        mockMvc.perform(put("/api/jobs/" + jobId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jobReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Lead Java Engineer"));

        // Get Shortlisted Candidates for Job (Admin) - currently empty
        mockMvc.perform(get("/api/jobs/" + jobId + "/shortlisted")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // =========================================================================
    // 4. Resumes
    // =========================================================================
    @Test
    @Order(4)
    @DisplayName("Folder 4: Resumes - Upload PDF, Extract Text, Register Metadata, Get by ID, Get by Candidate, Get PDF")
    void testFolder04_Resumes() throws Exception {
        // Upload Resume (PDF) multipart
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "jane_doe_resume.pdf",
                "application/pdf",
                "%PDF-1.4\n1 0 obj\n<< /Title (Jane Doe Resume) >>\nendobj\ntrailer\n<<>>\n%%EOF".getBytes()
        );

        MvcResult uploadRes = mockMvc.perform(multipart("/api/resumes/upload")
                        .file(pdfFile)
                        .param("candidateId", candidateId.toString())
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("jane_doe_resume.pdf"))
                .andReturn();

        JsonNode uploadJson = objectMapper.readTree(uploadRes.getResponse().getContentAsString());
        resumeId = uploadJson.get("id").asLong();
        assertThat(resumeId).isNotNull();

        // Extract Resume Text
        mockMvc.perform(post("/api/resumes/" + resumeId + "/extract-text")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk());

        // Get Resume by ID
        mockMvc.perform(get("/api/resumes/" + resumeId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(resumeId));

        // Get Resumes by Candidate ID
        mockMvc.perform(get("/api/resumes/candidate/" + candidateId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Create / Register Resume Metadata (Secondary check)
        ResumeRequest metadataReq = new ResumeRequest();
        metadataReq.setCandidateId(candidateId);
        metadataReq.setFileName("jane_doe_resume_v2.pdf");
        metadataReq.setFilePath("/uploads/resumes/jane_doe_resume_v2.pdf");
        metadataReq.setFileType("application/pdf");
        metadataReq.setFileSize(1048576L);
        metadataReq.setExtractedText("Jane Doe. Software Engineer with 4 years experience in Java, Spring Boot, Angular.");

        mockMvc.perform(post("/api/resumes")
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(metadataReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("jane_doe_resume_v2.pdf"));
    }

    // =========================================================================
    // 5. Applications
    // =========================================================================
    @Test
    @Order(5)
    @DisplayName("Folder 5: Applications - Submit Application, Get by ID, Get by Candidate, Get by Job, Get All, Update Status, Shortlist")
    void testFolder05_Applications() throws Exception {
        // Submit Job Application
        ApplicationRequest appReq = new ApplicationRequest();
        appReq.setCandidateId(candidateId);
        appReq.setJobId(jobId);
        appReq.setResumeId(resumeId);

        MvcResult appRes = mockMvc.perform(post("/api/applications")
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(appReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.candidateId").value(candidateId))
                .andExpect(jsonPath("$.jobId").value(jobId))
                .andReturn();

        JsonNode appJson = objectMapper.readTree(appRes.getResponse().getContentAsString());
        applicationId = appJson.get("id").asLong();
        assertThat(applicationId).isNotNull();

        // Get Application by ID
        mockMvc.perform(get("/api/applications/" + applicationId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(applicationId));

        // Get Applications by Candidate
        mockMvc.perform(get("/api/applications/candidate/" + candidateId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Get Applications by Job (Admin)
        mockMvc.perform(get("/api/applications/job/" + jobId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Get All Applications (Admin)
        mockMvc.perform(get("/api/applications")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Update Application Status (Admin)
        mockMvc.perform(put("/api/applications/" + applicationId + "/status?status=SCREENING")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCREENING"));

        // Step 12 requirement: Screening must exist before shortlisting
        ScreeningRequest preScreen = new ScreeningRequest();
        preScreen.setApplicationId(applicationId);
        preScreen.setOverallScore(88.5);
        preScreen.setSkillsScore(90.0);
        preScreen.setExperienceScore(85.0);
        preScreen.setEducationScore(90.0);
        preScreen.setMatchingSkills("Java, Spring Boot, MySQL");
        preScreen.setMissingSkills("Kubernetes");
        preScreen.setRecommendation("RECOMMENDED");

        mockMvc.perform(post("/api/screening")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(preScreen)))
                .andExpect(status().isCreated());

        // Shortlist Application (Admin)
        mockMvc.perform(put("/api/applications/" + applicationId + "/shortlist")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHORTLISTED"));

        // Verify it now appears under Get Shortlisted Candidates for Job
        mockMvc.perform(get("/api/jobs/" + jobId + "/shortlisted")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // =========================================================================
    // 6. Screening
    // =========================================================================
    @Test
    @Order(6)
    @DisplayName("Folder 6: Screening - Save Screening Result, Get by Application ID, Get by ID")
    void testFolder06_Screening() throws Exception {
        // Save Screening Result (Admin)
        ScreeningRequest screenReq = new ScreeningRequest();
        screenReq.setApplicationId(applicationId);
        screenReq.setOverallScore(88.5);
        screenReq.setSkillsScore(90.0);
        screenReq.setExperienceScore(85.0);
        screenReq.setEducationScore(90.0);
        screenReq.setMatchingSkills("Java, Spring Boot, MySQL");
        screenReq.setMissingSkills("Kubernetes");
        screenReq.setRecommendation("RECOMMENDED");

        MvcResult screenRes = mockMvc.perform(post("/api/screening")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(screenReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.overallScore").value(88.5))
                .andReturn();

        JsonNode screenJson = objectMapper.readTree(screenRes.getResponse().getContentAsString());
        screeningId = screenJson.get("id").asLong();
        assertThat(screeningId).isNotNull();

        // Get Screening by Application ID
        mockMvc.perform(get("/api/applications/" + applicationId + "/screening")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallScore").value(88.5));

        // Get Screening by ID
        mockMvc.perform(get("/api/screening/" + screeningId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(screeningId));
    }

    // =========================================================================
    // 7. Evaluation Criteria
    // =========================================================================
    @Test
    @Order(7)
    @DisplayName("Folder 7: Evaluation Criteria - Create Criteria, Get by ID, Get by Job ID, Update Criteria")
    void testFolder07_EvaluationCriteria() throws Exception {
        // Create Evaluation Criteria (Admin)
        EvaluationCriteriaRequest critReq = new EvaluationCriteriaRequest();
        critReq.setJobId(jobId);
        critReq.setRequiredSkills("Java, Spring Boot, Docker");
        critReq.setMinimumExperience(3.0);
        critReq.setEducationRequirements("Bachelor's Degree in CS");
        critReq.setSkillWeight(50.0);
        critReq.setExperienceWeight(30.0);
        critReq.setEducationWeight(20.0);

        MvcResult critRes = mockMvc.perform(post("/api/evaluation-criteria")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(critReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.skillWeight").value(50.0))
                .andReturn();

        JsonNode critJson = objectMapper.readTree(critRes.getResponse().getContentAsString());
        criteriaId = critJson.get("id").asLong();
        assertThat(criteriaId).isNotNull();

        // Get Evaluation Criteria by ID
        mockMvc.perform(get("/api/evaluation-criteria/" + criteriaId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(criteriaId));

        // Get Evaluation Criteria by Job ID
        mockMvc.perform(get("/api/evaluation-criteria/job/" + jobId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(jobId));

        // Update Evaluation Criteria (Admin)
        critReq.setRequiredSkills("Java, Spring Boot, AWS");
        critReq.setMinimumExperience(4.0);
        critReq.setSkillWeight(40.0);
        critReq.setExperienceWeight(40.0);
        critReq.setEducationWeight(20.0);

        mockMvc.perform(put("/api/evaluation-criteria/" + criteriaId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(critReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skillWeight").value(40.0));
    }

    // =========================================================================
    // 8. Interviews
    // =========================================================================
    @Test
    @Order(8)
    @DisplayName("Folder 8: Interviews - Schedule, Get by ID, Get by Application, Get All, Update, Get by Candidate, Reschedule, Cancel")
    void testFolder08_Interviews() throws Exception {
        // Schedule Interview (Admin)
        InterviewRequest intReq = new InterviewRequest();
        intReq.setApplicationId(applicationId);
        intReq.setScheduledDateTime(LocalDateTime.now().plusDays(3));
        intReq.setInterviewType("LIVE_AI");
        intReq.setMeetingLink("https://meet.google.com/abc-defg-hij");
        intReq.setNotes("Initial AI technical round");
        intReq.setStatus(InterviewStatus.SCHEDULED);

        MvcResult intRes = mockMvc.perform(post("/api/interviews")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(intReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.interviewType").value("LIVE_AI"))
                .andReturn();

        JsonNode intJson = objectMapper.readTree(intRes.getResponse().getContentAsString());
        interviewId = intJson.get("id").asLong();
        assertThat(interviewId).isNotNull();

        // Get Interview by ID
        mockMvc.perform(get("/api/interviews/" + interviewId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(interviewId));

        // Get Interview by Application ID
        mockMvc.perform(get("/api/interviews/application/" + applicationId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Get All Interviews (Admin)
        mockMvc.perform(get("/api/interviews")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Update Interview (Admin)
        intReq.setNotes("Notes updated by admin");
        mockMvc.perform(put("/api/interviews/" + interviewId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(intReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("Notes updated by admin"));

        // Get Interviews by Candidate ID
        mockMvc.perform(get("/api/interviews/candidate/" + candidateId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Reschedule Interview
        InterviewRequest reschedReq = new InterviewRequest();
        reschedReq.setScheduledDateTime(LocalDateTime.now().plusDays(5));
        reschedReq.setMeetingLink("https://meet.hireranker.com/rescheduled-session");
        reschedReq.setNotes("Rescheduled on candidate request");

        mockMvc.perform(put("/api/interviews/" + interviewId + "/reschedule")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reschedReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meetingLink").value("https://meet.hireranker.com/rescheduled-session"));

        // Cancel Interview
        mockMvc.perform(put("/api/interviews/" + interviewId + "/cancel?reason=CandidateRequested")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    // =========================================================================
    // 9. Messages
    // =========================================================================
    @Test
    @Order(9)
    @DisplayName("Folder 9: Messages - Send, Get User Messages, Get by ID, Conversation, Unread Count, Mark Read")
    void testFolder09_Messages() throws Exception {
        // Send Message
        MessageRequest msgReq = new MessageRequest();
        msgReq.setSenderId(candidateUserId);
        msgReq.setReceiverId(adminUserId);
        msgReq.setMessage("Hello, I submitted my application. Looking forward to hearing from you!");

        MvcResult msgRes = mockMvc.perform(post("/api/messages")
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msgReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Hello, I submitted my application. Looking forward to hearing from you!"))
                .andReturn();

        JsonNode msgJson = objectMapper.readTree(msgRes.getResponse().getContentAsString());
        messageId = msgJson.get("id").asLong();
        assertThat(messageId).isNotNull();

        // Get User Messages
        mockMvc.perform(get("/api/messages")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Get Message by ID
        mockMvc.perform(get("/api/messages/" + messageId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(messageId));

        // Get Conversation Between Users
        mockMvc.perform(get("/api/messages/conversation/" + candidateUserId + "/" + adminUserId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Get Unread Count for Admin (receiver)
        mockMvc.perform(get("/api/messages/unread/" + adminUserId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Mark Message as Read (by Admin receiver)
        mockMvc.perform(put("/api/messages/" + messageId + "/read")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readStatus").value("READ"));
    }

    // =========================================================================
    // 10. Admin
    // =========================================================================
    @Test
    @Order(10)
    @DisplayName("Folder 10: Admin - Dashboard Stats, Get All Users, Update Role")
    void testFolder10_Admin() throws Exception {
        // Get Admin Dashboard Stats
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").isNumber())
                .andExpect(jsonPath("$.totalJobs").isNumber());

        // Get All Users (Admin)
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Update User Role (Admin)
        mockMvc.perform(put("/api/admin/users/" + candidateUserId + "/role?role=CANDIDATE")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CANDIDATE"));
    }

    // =========================================================================
    // 11. Candidate Ranking
    // =========================================================================
    @Test
    @Order(11)
    @DisplayName("Folder 11: Candidate Ranking - Get Ranking for Job (Admin)")
    void testFolder11_CandidateRanking() throws Exception {
        mockMvc.perform(get("/api/jobs/" + jobId + "/ranking")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // =========================================================================
    // 12. Users
    // =========================================================================
    @Test
    @Order(12)
    @DisplayName("Folder 12: Users - Current User Profile (Me), Get by ID, Get All Users")
    void testFolder12_Users() throws Exception {
        // Get Current User Profile (Me)
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(candidateEmail));

        // Get User by ID (Admin)
        mockMvc.perform(get("/api/users/" + candidateUserId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(candidateUserId));

        // Get All Users (Admin)
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // =========================================================================
    // 13. Resource Cleanup / Deletion Endpoints
    // =========================================================================
    @Test
    @Order(13)
    @DisplayName("Cleanup & Deletions: Delete Message, Interview, Criteria, Screening, Application, Resume, Job, Candidate, User")
    void testFolder13_Deletions() throws Exception {
        // Delete Message
        mockMvc.perform(delete("/api/messages/" + messageId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isNoContent());

        // Delete Interview (Admin)
        mockMvc.perform(delete("/api/interviews/" + interviewId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Delete Evaluation Criteria (Admin)
        mockMvc.perform(delete("/api/evaluation-criteria/" + criteriaId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Delete Screening Result (Admin)
        mockMvc.perform(delete("/api/screening/" + screeningId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Delete Application (Admin)
        mockMvc.perform(delete("/api/applications/" + applicationId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Delete Resume
        mockMvc.perform(delete("/api/resumes/" + resumeId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isNoContent());

        // Delete Job (Admin)
        mockMvc.perform(delete("/api/jobs/" + jobId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Delete Candidate (Admin)
        mockMvc.perform(delete("/api/candidates/" + candidateId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Delete User (Admin)
        mockMvc.perform(delete("/api/admin/users/" + candidateUserId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }
}
