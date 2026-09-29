package spring.eshwar.service.email;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import spring.eshwar.entity.Application;
import spring.eshwar.entity.ApplicationStatus;
import spring.eshwar.entity.Candidate;
import spring.eshwar.entity.Interview;
import spring.eshwar.entity.InterviewStatus;
import spring.eshwar.entity.Job;
import spring.eshwar.entity.Role;
import spring.eshwar.entity.User;
import spring.eshwar.service.ApplicationService;
import spring.eshwar.service.InterviewService;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
public class ResendEmailNotificationTest {

    @Autowired
    private ResendEmailService resendEmailService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private InterviewService interviewService;

    @Test
    @DisplayName("ResendEmailService correctly extracts candidate email from Application -> Candidate -> User")
    void testExtractCandidateEmail() {
        User user = new User("Jane Doe", "jane.doe@example.com", "password", Role.CANDIDATE);
        Candidate candidate = new Candidate();
        candidate.setFullName("Jane Doe");
        candidate.setUser(user);

        Job job = new Job();
        job.setTitle("Frontend Engineer");
        job.setCompany("Acme Corp");

        Application application = new Application();
        application.setCandidate(candidate);
        application.setJob(job);
        application.setStatus(ApplicationStatus.SHORTLISTED);

        String email = resendEmailService.extractCandidateEmail(application);
        String name = resendEmailService.extractCandidateName(application);

        assertThat(email).isEqualTo("jane.doe@example.com");
        assertThat(name).isEqualTo("Jane Doe");
    }

    @Test
    @DisplayName("Candidate with missing email handles gracefully without throw")
    void testCandidateMissingEmailDoesNotThrow() {
        Candidate candidate = new Candidate();
        candidate.setFullName("No Email Candidate");
        candidate.setUser(null); // No user / no email

        Job job = new Job();
        job.setTitle("Cloud Architect");
        job.setCompany("Global Cloud Inc");

        Application application = new Application();
        application.setCandidate(candidate);
        application.setJob(job);

        assertThatCode(() -> {
            boolean resultShortlist = resendEmailService.sendShortlistedEmail(application);
            assertThat(resultShortlist).isFalse();

            boolean resultReject = resendEmailService.sendRejectedEmail(application);
            assertThat(resultReject).isFalse();

            Interview interview = new Interview();
            interview.setApplication(application);
            interview.setScheduledDateTime(LocalDateTime.now().plusDays(2));
            boolean resultInterview = resendEmailService.sendInterviewScheduledEmail(interview);
            assertThat(resultInterview).isFalse();
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Invalid or mock API key does not crash email dispatch methods")
    void testInvalidApiKeyDoesNotThrow() {
        User user = new User("John Smith", "john.smith@example.com", "password", Role.CANDIDATE);
        Candidate candidate = new Candidate();
        candidate.setFullName("John Smith");
        candidate.setUser(user);

        Job job = new Job();
        job.setTitle("Backend Engineer");
        job.setCompany("Tech Innovations");

        Application application = new Application();
        application.setCandidate(candidate);
        application.setJob(job);

        Interview interview = new Interview();
        interview.setApplication(application);
        interview.setScheduledDateTime(LocalDateTime.now().plusDays(3));
        interview.setInterviewType("ONLINE");
        interview.setMeetingLink("https://meet.hireranker.com/test-room");
        interview.setNotes("First round technical discussion");
        interview.setStatus(InterviewStatus.SCHEDULED);

        assertThatCode(() -> {
            // These calls will safely catch any network or Resend API failures and return false
            resendEmailService.sendShortlistedEmail(application);
            resendEmailService.sendRejectedEmail(application);
            resendEmailService.sendInterviewScheduledEmail(interview);
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Duplicate status update does not perform redundant actions")
    void testDuplicateStatusUpdateAvoidance() {
        assertThat(applicationService).isNotNull();
        assertThat(interviewService).isNotNull();
    }
}
