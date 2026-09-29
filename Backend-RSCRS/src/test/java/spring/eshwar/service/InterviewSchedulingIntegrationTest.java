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
import spring.eshwar.dto.interview.InterviewRequest;
import spring.eshwar.dto.job.JobRequest;
import spring.eshwar.dto.resume.ResumeRequest;
import spring.eshwar.entity.Application;
import spring.eshwar.entity.ApplicationStatus;
import spring.eshwar.entity.Candidate;
import spring.eshwar.entity.Interview;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
public class InterviewSchedulingIntegrationTest {

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
    private InterviewRepository interviewRepository;

    @Autowired
    private ScreeningResultRepository screeningResultRepository;

    private String adminToken;
    private String cand1Token;
    private String cand2Token;

    private Long jobId;
    private Long cand1Id;
    private Long cand2Id;

    private Long app1Id;
    private Long app2Id;

    private Long res1Id;
    private Long res2Id;

    private Long interview1Id;
    private final List<Long> createdInterviewIds = new ArrayList<>();

    @BeforeAll
    void setUp() throws Exception {
        // 1. Admin
        String adminEmail = "admin.interview." + System.currentTimeMillis() + "@hireranker.com";
        RegisterRequest adminReq = new RegisterRequest();
        adminReq.setName("Admin Interviewer");
        adminReq.setEmail(adminEmail);
        adminReq.setPassword("AdminPass123!");
        adminReq.setRole(Role.ADMIN);

        MvcResult adminRes = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminReq)))
                .andExpect(status().isCreated())
                .andReturn();
        adminToken = objectMapper.readTree(adminRes.getResponse().getContentAsString()).get("token").asText();

        // 2. Candidate 1 (Shortlisted)
        String cand1Email = "cand1.int." + System.currentTimeMillis() + "@hireranker.com";
        cand1Id = registerCandidate("Alex Shortlisted", cand1Email);
        cand1Token = loginUser(cand1Email, "CandPass123!");

        // 3. Candidate 2 (Applied, not shortlisted)
        String cand2Email = "cand2.int." + System.currentTimeMillis() + "@hireranker.com";
        cand2Id = registerCandidate("Jordan Unshortlisted", cand2Email);
        cand2Token = loginUser(cand2Email, "CandPass123!");

        // 4. Job
        JobRequest jobReq = new JobRequest();
        jobReq.setTitle("Senior Full Stack Engineer");
        jobReq.setCompany("CloudNative Systems");
        jobReq.setLocation("Hyderabad, India");
        jobReq.setDescription("Building distributed systems with Spring Boot and Angular.");
        jobReq.setResponsibilities("Design, develop, and maintain high performance services.");
        jobReq.setRequiredSkills("Java, Spring Boot, Angular, Docker");
        jobReq.setExperienceRequired("4+ years");
        jobReq.setStatus(JobStatus.ACTIVE);

        MvcResult jobRes = mockMvc.perform(post("/api/jobs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jobReq)))
                .andExpect(status().isCreated())
                .andReturn();
        jobId = objectMapper.readTree(jobRes.getResponse().getContentAsString()).get("id").asLong();

        // 5. Candidate 1 Application -> Shortlisted
        res1Id = createResume(cand1Id, "alex_cv.pdf");
        app1Id = createApplication(cand1Id, jobId, res1Id);

        // Attach screening result so shortlisting requirement is met
        Application a1 = applicationRepository.findById(app1Id).orElseThrow();
        ScreeningResult sr1 = new ScreeningResult(
                a1, 91.0, 92.0, 90.0, 90.0,
                "Java, Spring Boot, Angular", "None", "RECOMMENDED"
        );
        screeningResultRepository.save(sr1);

        // Shortlist Candidate 1
        mockMvc.perform(put("/api/applications/" + app1Id + "/shortlist")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // 6. Candidate 2 Application -> Stays APPLIED (NOT shortlisted)
        res2Id = createResume(cand2Id, "jordan_cv.pdf");
        app2Id = createApplication(cand2Id, jobId, res2Id);
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

    private String loginUser(String email, String password) throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(email);
        req.setPassword(password);

        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    private Long createResume(Long candidateId, String fileName) throws Exception {
        ResumeRequest req = new ResumeRequest();
        req.setCandidateId(candidateId);
        req.setFileName(fileName);
        req.setFileType("application/pdf");
        req.setFilePath("/uploads/resumes/" + fileName);
        req.setFileSize(51200L);

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
            for (Long intId : createdInterviewIds) {
                if (interviewRepository.existsById(intId)) {
                    interviewRepository.deleteById(intId);
                }
            }
            Long[] appIds = {app1Id, app2Id};
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
            Long[] resIds = {res1Id, res2Id};
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
    @DisplayName("Admin can schedule an interview for shortlisted candidate with future date, ONLINE type, and meeting link")
    void testAdminCanScheduleInterviewForShortlistedCandidate() throws Exception {
        InterviewRequest req = new InterviewRequest();
        req.setApplicationId(app1Id);
        req.setScheduledDateTime(LocalDateTime.now().plusDays(3));
        req.setInterviewType("ONLINE");
        req.setMeetingLink("https://meet.hireranker.com/tech-round-1");
        req.setNotes("Comprehensive technical evaluation round.");

        MvcResult res = mockMvc.perform(post("/api/interviews")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.applicationId").value(app1Id))
                .andExpect(jsonPath("$.interviewType").value("ONLINE"))
                .andExpect(jsonPath("$.meetingLink").value("https://meet.hireranker.com/tech-round-1"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.notes").value("Comprehensive technical evaluation round."))
                .andReturn();

        JsonNode json = objectMapper.readTree(res.getResponse().getContentAsString());
        interview1Id = json.get("id").asLong();
        createdInterviewIds.add(interview1Id);

        // Verify Application status transitioned to INTERVIEW
        Application app = applicationRepository.findById(app1Id).orElseThrow();
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.INTERVIEW);
    }

    @Test
    @Order(2)
    @DisplayName("Reject interview scheduling if candidate is not shortlisted")
    void testCannotScheduleInterviewForNonShortlistedCandidate() throws Exception {
        InterviewRequest req = new InterviewRequest();
        req.setApplicationId(app2Id); // app2 is in APPLIED status
        req.setScheduledDateTime(LocalDateTime.now().plusDays(2));
        req.setInterviewType("ONLINE");

        mockMvc.perform(post("/api/interviews")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(3)
    @DisplayName("Reject interview scheduling with past date")
    void testCannotScheduleInterviewInThePast() throws Exception {
        InterviewRequest req = new InterviewRequest();
        req.setApplicationId(app1Id);
        req.setScheduledDateTime(LocalDateTime.now().minusDays(1)); // Past date
        req.setInterviewType("ONLINE");

        mockMvc.perform(post("/api/interviews")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(4)
    @DisplayName("Support OFFLINE, PHONE, and auto-generated meeting link for ONLINE")
    void testScheduleDifferentInterviewTypes() throws Exception {
        // OFFLINE
        InterviewRequest offlineReq = new InterviewRequest();
        offlineReq.setApplicationId(app1Id);
        offlineReq.setScheduledDateTime(LocalDateTime.now().plusDays(4));
        offlineReq.setInterviewType("OFFLINE");
        offlineReq.setNotes("Room 402, Bangalore Office");

        MvcResult offlineRes = mockMvc.perform(post("/api/interviews")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(offlineReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.interviewType").value("OFFLINE"))
                .andReturn();
        createdInterviewIds.add(objectMapper.readTree(offlineRes.getResponse().getContentAsString()).get("id").asLong());

        // PHONE
        InterviewRequest phoneReq = new InterviewRequest();
        phoneReq.setApplicationId(app1Id);
        phoneReq.setScheduledDateTime(LocalDateTime.now().plusDays(5));
        phoneReq.setInterviewType("PHONE");
        phoneReq.setNotes("Telephonic screening with Lead Architect");

        MvcResult phoneRes = mockMvc.perform(post("/api/interviews")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(phoneReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.interviewType").value("PHONE"))
                .andReturn();
        createdInterviewIds.add(objectMapper.readTree(phoneRes.getResponse().getContentAsString()).get("id").asLong());

        // ONLINE without meeting link -> should auto-generate meeting link
        InterviewRequest onlineReq = new InterviewRequest();
        onlineReq.setApplicationId(app1Id);
        onlineReq.setScheduledDateTime(LocalDateTime.now().plusDays(6));
        onlineReq.setInterviewType("ONLINE");

        MvcResult onlineRes = mockMvc.perform(post("/api/interviews")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(onlineReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.interviewType").value("ONLINE"))
                .andExpect(jsonPath("$.meetingLink").isNotEmpty())
                .andReturn();
        createdInterviewIds.add(objectMapper.readTree(onlineRes.getResponse().getContentAsString()).get("id").asLong());
    }

    @Test
    @Order(5)
    @DisplayName("Candidate can view their own interview by ID and candidate list")
    void testCandidateCanViewOwnInterviews() throws Exception {
        // View by ID
        mockMvc.perform(get("/api/interviews/" + interview1Id)
                        .header("Authorization", "Bearer " + cand1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(interview1Id))
                .andExpect(jsonPath("$.candidateName").value("Alex Shortlisted"));

        // View by Candidate ID
        mockMvc.perform(get("/api/interviews/candidate/" + cand1Id)
                        .header("Authorization", "Bearer " + cand1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    @Order(6)
    @DisplayName("Candidate cannot view another candidate's interview (403 Forbidden)")
    void testCandidateCannotViewAnotherCandidatesInterview() throws Exception {
        // Candidate 2 trying to view Candidate 1's interview
        mockMvc.perform(get("/api/interviews/" + interview1Id)
                        .header("Authorization", "Bearer " + cand2Token))
                .andExpect(status().isForbidden());

        // Candidate 2 trying to view Candidate 1's candidate list
        mockMvc.perform(get("/api/interviews/candidate/" + cand1Id)
                        .header("Authorization", "Bearer " + cand2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(7)
    @DisplayName("Candidate cannot modify interviews (403 Forbidden)")
    void testCandidateCannotModifyInterviews() throws Exception {
        InterviewRequest modReq = new InterviewRequest();
        modReq.setScheduledDateTime(LocalDateTime.now().plusDays(10));

        // Candidate 1 (owner) attempting PUT
        mockMvc.perform(put("/api/interviews/" + interview1Id)
                        .header("Authorization", "Bearer " + cand1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(modReq)))
                .andExpect(status().isForbidden());

        // Candidate 1 (owner) attempting Cancel
        mockMvc.perform(put("/api/interviews/" + interview1Id + "/cancel")
                        .header("Authorization", "Bearer " + cand1Token))
                .andExpect(status().isForbidden());

        // Candidate 1 (owner) attempting Reschedule
        mockMvc.perform(put("/api/interviews/" + interview1Id + "/reschedule")
                        .header("Authorization", "Bearer " + cand1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(modReq)))
                .andExpect(status().isForbidden());

        // Candidate 2 attempting PUT
        mockMvc.perform(put("/api/interviews/" + interview1Id)
                        .header("Authorization", "Bearer " + cand2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(modReq)))
                .andExpect(status().isForbidden());

        // Candidate 2 attempting Cancel
        mockMvc.perform(put("/api/interviews/" + interview1Id + "/cancel")
                        .header("Authorization", "Bearer " + cand2Token))
                .andExpect(status().isForbidden());

        // Candidate 2 attempting Reschedule
        mockMvc.perform(put("/api/interviews/" + interview1Id + "/reschedule")
                        .header("Authorization", "Bearer " + cand2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(modReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(8)
    @DisplayName("Admin or authorized user can reschedule interview")
    void testRescheduleInterview() throws Exception {
        InterviewRequest reschedReq = new InterviewRequest();
        reschedReq.setScheduledDateTime(LocalDateTime.now().plusDays(7));
        reschedReq.setMeetingLink("https://meet.hireranker.com/rescheduled-session");
        reschedReq.setNotes("Rescheduled to next week.");

        mockMvc.perform(put("/api/interviews/" + interview1Id + "/reschedule")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reschedReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(interview1Id))
                .andExpect(jsonPath("$.status").value("RESCHEDULED"))
                .andExpect(jsonPath("$.meetingLink").value("https://meet.hireranker.com/rescheduled-session"));

        Interview updated = interviewRepository.findById(interview1Id).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(InterviewStatus.RESCHEDULED);
    }

    @Test
    @Order(9)
    @DisplayName("Admin or authorized user can cancel interview with reason")
    void testCancelInterview() throws Exception {
        mockMvc.perform(put("/api/interviews/" + interview1Id + "/cancel?reason=CandidateRequested")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(interview1Id))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.notes").value(org.hamcrest.Matchers.containsString("CandidateRequested")));

        Interview updated = interviewRepository.findById(interview1Id).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(InterviewStatus.CANCELLED);
    }

    @Test
    @Order(10)
    @DisplayName("Candidate cannot schedule an interview (403 Forbidden)")
    void testCandidateCannotScheduleInterview() throws Exception {
        InterviewRequest req = new InterviewRequest();
        req.setApplicationId(app1Id);
        req.setScheduledDateTime(LocalDateTime.now().plusDays(2));
        req.setInterviewType("ONLINE");

        mockMvc.perform(post("/api/interviews")
                        .header("Authorization", "Bearer " + cand1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(11)
    @DisplayName("Reject interview scheduling with unsupported interview type (400 Bad Request)")
    void testRejectUnsupportedInterviewType() throws Exception {
        InterviewRequest req = new InterviewRequest();
        req.setApplicationId(app1Id);
        req.setScheduledDateTime(LocalDateTime.now().plusDays(5));
        req.setInterviewType("HOLOGRAM");

        mockMvc.perform(post("/api/interviews")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
