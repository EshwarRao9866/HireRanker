package spring.eshwar.interview;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import spring.eshwar.dto.interview.LiveQuestionResponse;
import spring.eshwar.dto.interview.SkipLiveQuestionRequest;
import spring.eshwar.dto.interview.StartInterviewRequest;
import spring.eshwar.dto.interview.StartInterviewResponse;
import spring.eshwar.dto.interview.SubmitLiveAnswerRequest;
import spring.eshwar.dto.interview.SubmitLiveAnswerResponse;
import spring.eshwar.entity.Application;
import spring.eshwar.entity.Candidate;
import spring.eshwar.entity.InterviewStatus;
import spring.eshwar.entity.Job;
import spring.eshwar.entity.JobStatus;
import spring.eshwar.entity.QuestionStatus;
import spring.eshwar.entity.Role;
import spring.eshwar.entity.User;
import spring.eshwar.repository.ApplicationRepository;
import spring.eshwar.repository.CandidateRepository;
import spring.eshwar.repository.InterviewAnswerRepository;
import spring.eshwar.repository.InterviewQuestionRepository;
import spring.eshwar.repository.InterviewRepository;
import spring.eshwar.repository.InterviewResultRepository;
import spring.eshwar.repository.JobRepository;
import spring.eshwar.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class LiveInterviewIntegrationTest {

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
    private ApplicationRepository applicationRepository;

    @Autowired
    private InterviewRepository interviewRepository;

    @Autowired
    private InterviewQuestionRepository interviewQuestionRepository;

    @Autowired
    private InterviewAnswerRepository interviewAnswerRepository;

    @Autowired
    private InterviewResultRepository interviewResultRepository;

    @Autowired
    private spring.eshwar.repository.ResumeRepository resumeRepository;

    private Application testApplication;

    @BeforeEach
    void setUp() {
        interviewResultRepository.deleteAll();
        interviewAnswerRepository.deleteAll();
        interviewQuestionRepository.deleteAll();
        interviewRepository.deleteAll();
        applicationRepository.deleteAll();
        resumeRepository.deleteAll();
        candidateRepository.deleteAll();
        jobRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create User
        User user = new User("Jane Doe", "jane.doe@example.com", "Password@123", Role.CANDIDATE);
        user = userRepository.save(user);

        // 2. Create Candidate
        Candidate candidate = new Candidate(
                user,
                "Jane Doe",
                "+1234567890",
                "San Francisco, CA",
                "Java, Spring Boot, Microservices, React, Docker",
                "4 years",
                "B.S. in Computer Science",
                "https://github.com/janedoe",
                "https://linkedin.com/in/janedoe"
        );
        candidate = candidateRepository.save(candidate);

        // 3. Create Job
        Job job = new Job(
                "Senior Backend Engineer",
                "HireRanker Labs",
                "Design and maintain distributed high-performance REST microservices.",
                "Java, Spring Boot, REST APIs, SQL, Docker",
                "3-5 years",
                "Remote",
                "$120,000 - $150,000",
                "Full-time",
                JobStatus.ACTIVE
        );
        job = jobRepository.save(job);

        // 4. Create Application
        Application application = new Application(candidate, job, null);
        testApplication = applicationRepository.save(application);
    }

    @Test
    @DisplayName("Complete AI Live Interview flow: Start -> Answer -> 15s Timeout Skip -> Complete -> View Scorecard")
    void testFullLiveInterviewFlow() throws Exception {
        // ==========================================
        // 1. START INTERVIEW
        // ==========================================
        StartInterviewRequest startRequest = new StartInterviewRequest(testApplication.getId());

        MvcResult startResult = mockMvc.perform(post("/api/interviews/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(startRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.interviewId", notNullValue()))
                .andExpect(jsonPath("$.applicationId").value(testApplication.getId()))
                .andExpect(jsonPath("$.candidateName").value("Jane Doe"))
                .andExpect(jsonPath("$.status").value(InterviewStatus.IN_PROGRESS.name()))
                .andExpect(jsonPath("$.firstQuestion", notNullValue()))
                .andExpect(jsonPath("$.firstQuestion.questionNumber").value(1))
                .andExpect(jsonPath("$.firstQuestion.timeLimitSeconds").value(15))
                .andExpect(jsonPath("$.firstQuestion.status").value(QuestionStatus.PENDING.name()))
                .andReturn();

        StartInterviewResponse startResponse = objectMapper.readValue(
                startResult.getResponse().getContentAsString(),
                StartInterviewResponse.class
        );

        Long interviewId = startResponse.getInterviewId();
        LiveQuestionResponse q1 = startResponse.getFirstQuestion();
        assertThat(interviewId).isNotNull();
        assertThat(q1).isNotNull();
        assertThat(q1.getQuestionText()).isNotEmpty();

        // ==========================================
        // 2. SUBMIT SPOKEN ANSWER FOR QUESTION #1
        // Candidate starts speaking at 3.2s, speaks for 35s
        // ==========================================
        SubmitLiveAnswerRequest answerReq = new SubmitLiveAnswerRequest(
                "I have over 4 years of hands-on experience designing REST APIs and microservices using Spring Boot, JPA, and PostgreSQL. In my previous role, I built a payment gateway adapter with high concurrency and fault tolerance.",
                3.2,
                35.0,
                "https://storage.hireranker.internal/recordings/ans_q1.webm"
        );

        MvcResult answerResult = mockMvc.perform(post("/api/interviews/" + interviewId + "/questions/" + q1.getQuestionId() + "/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(answerReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interviewId").value(interviewId))
                .andExpect(jsonPath("$.questionId").value(q1.getQuestionId()))
                .andExpect(jsonPath("$.status").value(QuestionStatus.ANSWERED.name()))
                .andExpect(jsonPath("$.technicalScore", greaterThanOrEqualTo(0.0)))
                .andExpect(jsonPath("$.clarityScore", greaterThanOrEqualTo(0.0)))
                .andExpect(jsonPath("$.feedback", notNullValue()))
                .andExpect(jsonPath("$.interviewFinished").value(false))
                .andExpect(jsonPath("$.nextQuestion", notNullValue()))
                .andExpect(jsonPath("$.nextQuestion.questionNumber").value(2))
                .andReturn();

        SubmitLiveAnswerResponse ansResponse = objectMapper.readValue(
                answerResult.getResponse().getContentAsString(),
                SubmitLiveAnswerResponse.class
        );

        LiveQuestionResponse q2 = ansResponse.getNextQuestion();
        assertThat(q2).isNotNull();
        assertThat(q2.getQuestionNumber()).isEqualTo(2);

        // ==========================================
        // 3. 15-SECOND RULE: QUESTION #2 TIMEOUT / SKIP
        // Candidate did not speak within 15s countdown
        // ==========================================
        SkipLiveQuestionRequest skipReq = new SkipLiveQuestionRequest("TIMEOUT_15S", 15.0);

        MvcResult skipResult = mockMvc.perform(post("/api/interviews/" + interviewId + "/questions/" + q2.getQuestionId() + "/skip")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(skipReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interviewId").value(interviewId))
                .andExpect(jsonPath("$.questionId").value(q2.getQuestionId()))
                .andExpect(jsonPath("$.status").value(QuestionStatus.SKIPPED.name()))
                .andExpect(jsonPath("$.technicalScore").value(0.0))
                .andExpect(jsonPath("$.feedback").isNotEmpty())
                .andExpect(jsonPath("$.nextQuestion", notNullValue()))
                .andExpect(jsonPath("$.nextQuestion.questionNumber").value(3))
                .andReturn();

        SubmitLiveAnswerResponse skipResponse = objectMapper.readValue(
                skipResult.getResponse().getContentAsString(),
                SubmitLiveAnswerResponse.class
        );

        LiveQuestionResponse q3 = skipResponse.getNextQuestion();
        assertThat(q3).isNotNull();
        assertThat(q3.getQuestionNumber()).isEqualTo(3);

        // ==========================================
        // 4. COMPLETE INTERVIEW & GENERATE ZERO-BIAS SCORECARD
        // ==========================================
        mockMvc.perform(post("/api/interviews/" + interviewId + "/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interviewId").value(interviewId))
                .andExpect(jsonPath("$.applicationId").value(testApplication.getId()))
                .andExpect(jsonPath("$.candidateName").value("Jane Doe"))
                .andExpect(jsonPath("$.jobTitle").value("Senior Backend Engineer"))
                .andExpect(jsonPath("$.technicalScore", notNullValue()))
                .andExpect(jsonPath("$.communicationScore", notNullValue()))
                .andExpect(jsonPath("$.overallScore", notNullValue()))
                .andExpect(jsonPath("$.recommendation", notNullValue()))
                .andExpect(jsonPath("$.strengths", notNullValue()))
                .andExpect(jsonPath("$.weaknesses", notNullValue()))
                .andExpect(jsonPath("$.improvementTopics", notNullValue()))
                .andExpect(jsonPath("$.totalQuestions").value(3))
                .andExpect(jsonPath("$.answeredQuestions").value(1))
                .andExpect(jsonPath("$.skippedQuestions").value(1));

        // ==========================================
        // 5. GET INTERVIEW RESULT BY INTERVIEW ID
        // ==========================================
        mockMvc.perform(get("/api/interviews/" + interviewId + "/result"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interviewId").value(interviewId))
                .andExpect(jsonPath("$.overallScore", notNullValue()));

        // ==========================================
        // 6. GET INTERVIEW RESULT BY APPLICATION ID
        // ==========================================
        mockMvc.perform(get("/api/interviews/application/" + testApplication.getId() + "/result"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value(testApplication.getId()))
                .andExpect(jsonPath("$.overallScore", notNullValue()));
    }
}
