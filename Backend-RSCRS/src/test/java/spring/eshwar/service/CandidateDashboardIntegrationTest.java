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
import spring.eshwar.dto.interview.InterviewRequest;
import spring.eshwar.dto.job.JobRequest;
import spring.eshwar.dto.resume.ResumeRequest;
import spring.eshwar.entity.Application;
import spring.eshwar.entity.ApplicationStatus;
import spring.eshwar.entity.Candidate;
import spring.eshwar.entity.InterviewStatus;
import spring.eshwar.entity.JobStatus;
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
public class CandidateDashboardIntegrationTest {

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

    @Autowired
    private InterviewRepository interviewRepository;

    private String adminToken;

    private Long cand1Id;
    private String cand1Token;

    private Long cand2Id;
    private String cand2Token;

    private Long jobId;
    private Long resume1Id;
    private Long app1Id;
    private Long screening1Id;
    private Long interview1Id;

    @BeforeAll
    void setUp() throws Exception {
        // 1. Register Admin
        String adminEmail = "admin.cdash." + System.currentTimeMillis() + "@hireranker.com";
        registerUser("Admin Recruiter", adminEmail, "AdminPass123!", Role.ADMIN);
        adminToken = loginUser(adminEmail, "AdminPass123!");

        // 2. Register Candidate 1 (Active candidate with applications and interviews)
        String cand1Email = "cand1.cdash." + System.currentTimeMillis() + "@hireranker.com";
        registerUser("Eshwar Rao", cand1Email, "CandPass123!", Role.CANDIDATE);
        cand1Token = loginUser(cand1Email, "CandPass123!");
        Candidate cand1 = candidateRepository.findByUserEmail(cand1Email).orElseThrow();
        cand1Id = cand1.getId();
        cand1.setSkills("Java, Spring Boot, SQL, Angular, Docker");
        candidateRepository.save(cand1);

        // 3. Register Candidate 2 (Fresh candidate with 0 applications)
        String cand2Email = "cand2.cdash." + System.currentTimeMillis() + "@hireranker.com";
        registerUser("Krupa Jyothi", cand2Email, "CandPass123!", Role.CANDIDATE);
        cand2Token = loginUser(cand2Email, "CandPass123!");
        Candidate cand2 = candidateRepository.findByUserEmail(cand2Email).orElseThrow();
        cand2Id = cand2.getId();

        // 4. Create Active Job
        JobRequest jobReq = new JobRequest();
        jobReq.setTitle("Senior Java Full Stack Engineer");
        jobReq.setCompany("FutureTech Global");
        jobReq.setLocation("Hyderabad, India");
        jobReq.setDescription("Building high-throughput microservices.");
        jobReq.setRequiredSkills("Java, Spring Boot, SQL, Angular");
        jobReq.setExperienceRequired("4+ years");
        jobReq.setStatus(JobStatus.ACTIVE);

        MvcResult jobRes = mockMvc.perform(post("/api/jobs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jobReq)))
                .andExpect(status().isCreated())
                .andReturn();
        jobId = objectMapper.readTree(jobRes.getResponse().getContentAsString()).get("id").asLong();

        // 5. Candidate 1 uploads Resume
        ResumeRequest resReq = new ResumeRequest();
        resReq.setCandidateId(cand1Id);
        resReq.setFileName("Eshwar_Rao_Resume.pdf");
        resReq.setFileType("application/pdf");
        resReq.setFilePath("/uploads/resumes/Eshwar_Rao_Resume.pdf");
        resReq.setFileSize(154200L);
        resReq.setExtractedText("Java Full Stack developer with 5 years experience in Spring Boot and Angular.");

        MvcResult resResult = mockMvc.perform(post("/api/resumes")
                        .header("Authorization", "Bearer " + cand1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resReq)))
                .andExpect(status().isCreated())
                .andReturn();
        resume1Id = objectMapper.readTree(resResult.getResponse().getContentAsString()).get("id").asLong();

        // 6. Candidate 1 applies for Job
        ApplicationRequest appReq = new ApplicationRequest();
        appReq.setCandidateId(cand1Id);
        appReq.setJobId(jobId);
        appReq.setResumeId(resume1Id);

        MvcResult appResult = mockMvc.perform(post("/api/applications")
                        .header("Authorization", "Bearer " + cand1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(appReq)))
                .andExpect(status().isCreated())
                .andReturn();
        app1Id = objectMapper.readTree(appResult.getResponse().getContentAsString()).get("id").asLong();

        // 7. Update Application to SHORTLISTED and attach ScreeningResult
        Application app = applicationRepository.findById(app1Id).orElseThrow();
        app.setStatus(ApplicationStatus.SHORTLISTED);
        applicationRepository.save(app);

        ScreeningResult sr = new ScreeningResult();
        sr.setApplication(app);
        sr.setOverallScore(92.0);
        sr.setSkillsScore(94.0);
        sr.setExperienceScore(90.0);
        sr.setEducationScore(92.0);
        sr.setMatchingSkills("Java, Spring Boot, SQL, Angular");
        sr.setMissingSkills("Docker");
        sr.setRecommendation("RECOMMENDED");
        sr.setScreenedAt(LocalDateTime.now());
        screening1Id = screeningResultRepository.save(sr).getId();

        // 8. Schedule Upcoming Interview for Candidate 1
        InterviewRequest intReq = new InterviewRequest();
        intReq.setApplicationId(app1Id);
        intReq.setScheduledDateTime(LocalDateTime.now().plusDays(2));
        intReq.setInterviewType("ONLINE");
        intReq.setMeetingLink("https://meet.hireranker.com/interview-cand1");
        intReq.setNotes("Technical Architecture Round");
        intReq.setStatus(InterviewStatus.SCHEDULED);

        MvcResult intResult = mockMvc.perform(post("/api/interviews")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(intReq)))
                .andExpect(status().isCreated())
                .andReturn();
        interview1Id = objectMapper.readTree(intResult.getResponse().getContentAsString()).get("id").asLong();
    }

    @AfterAll
    void tearDown() {
        try {
            if (interview1Id != null && interviewRepository.existsById(interview1Id)) {
                interviewRepository.deleteById(interview1Id);
            }
            if (screening1Id != null && screeningResultRepository.existsById(screening1Id)) {
                screeningResultRepository.deleteById(screening1Id);
            }
            if (app1Id != null && applicationRepository.existsById(app1Id)) {
                applicationRepository.deleteById(app1Id);
            }
            if (resume1Id != null && resumeRepository.existsById(resume1Id)) {
                resumeRepository.deleteById(resume1Id);
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
    @DisplayName("1. Authenticated candidate accesses GET /api/candidates/me/dashboard successfully")
    void testCandidateCanAccessOwnDashboard() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/candidates/me/dashboard")
                        .header("Authorization", "Bearer " + cand1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateProfile.id").value(cand1Id))
                .andExpect(jsonPath("$.candidateProfile.fullName").value("Eshwar Rao"))
                .andExpect(jsonPath("$.totalApplications").value(1))
                .andExpect(jsonPath("$.totalResumes").value(1))
                .andExpect(jsonPath("$.screenedApplications").value(1))
                .andExpect(jsonPath("$.upcomingInterviews").isArray())
                .andExpect(jsonPath("$.recentApplications").isArray())
                .andExpect(jsonPath("$.applicationStatuses").isMap())
                .andExpect(jsonPath("$.recommendedJobs").isArray())
                .andReturn();

        JsonNode json = objectMapper.readTree(res.getResponse().getContentAsString());
        assertThat(json.get("upcomingInterviews").size()).isEqualTo(1);
        assertThat(json.get("upcomingInterviews").get(0).get("meetingLink").asText())
                .isEqualTo("https://meet.hireranker.com/interview-cand1");

        assertThat(json.get("recentApplications").size()).isEqualTo(1);
        JsonNode recentApp = json.get("recentApplications").get(0);
        assertThat(recentApp.get("jobTitle").asText()).isEqualTo("Senior Java Full Stack Engineer");
        assertThat(recentApp.get("matchScore").asDouble()).isEqualTo(92.0);

        assertThat(json.get("recommendedJobs").size()).isGreaterThanOrEqualTo(1);
        JsonNode recJob = json.get("recommendedJobs").get(0);
        assertThat(recJob.get("matchScore").asDouble()).isGreaterThan(70.0);
    }

    @Test
    @Order(2)
    @DisplayName("2. Privacy and data isolation: Candidate 2 cannot see Candidate 1's data")
    void testDataIsolationBetweenCandidates() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/candidates/me/dashboard")
                        .header("Authorization", "Bearer " + cand2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateProfile.id").value(cand2Id))
                .andExpect(jsonPath("$.candidateProfile.fullName").value("Krupa Jyothi"))
                .andExpect(jsonPath("$.totalApplications").value(0))
                .andExpect(jsonPath("$.totalResumes").value(0))
                .andExpect(jsonPath("$.screenedApplications").value(0))
                .andExpect(jsonPath("$.upcomingInterviews").isEmpty())
                .andExpect(jsonPath("$.recentApplications").isEmpty())
                .andReturn();

        JsonNode json = objectMapper.readTree(res.getResponse().getContentAsString());
        assertThat(json.get("upcomingInterviews").size()).isEqualTo(0);
        assertThat(json.get("recentApplications").size()).isEqualTo(0);
    }

    @Test
    @Order(3)
    @DisplayName("3. Non-candidate (ADMIN) receives 403 Forbidden")
    void testAdminAccessForbidden() throws Exception {
        mockMvc.perform(get("/api/candidates/me/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(4)
    @DisplayName("4. Unauthenticated request receives 401 Unauthorized")
    void testUnauthenticatedAccessDenied() throws Exception {
        mockMvc.perform(get("/api/candidates/me/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(5)
    @DisplayName("5. Request parameter candidateId is completely ignored; identity relies purely on JWT")
    void testCandidateIdParamIgnored() throws Exception {
        mockMvc.perform(get("/api/candidates/me/dashboard?candidateId=99999")
                        .header("Authorization", "Bearer " + cand1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateProfile.id").value(cand1Id))
                .andExpect(jsonPath("$.candidateProfile.fullName").value("Eshwar Rao"));
    }

    @Test
    @Order(6)
    @DisplayName("6. Both availableJobs and recommendedJobs are available in the JSON response")
    void testAvailableJobsFieldPresent() throws Exception {
        mockMvc.perform(get("/api/candidates/me/dashboard")
                        .header("Authorization", "Bearer " + cand1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendedJobs").isArray())
                .andExpect(jsonPath("$.availableJobs").isArray());
    }
}

