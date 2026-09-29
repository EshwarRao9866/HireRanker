package spring.eshwar.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.eshwar.entity.Application;
import spring.eshwar.entity.ApplicationStatus;
import spring.eshwar.entity.Interview;
import spring.eshwar.entity.InterviewStatus;
import spring.eshwar.exception.BadRequestException;
import spring.eshwar.exception.ResourceNotFoundException;
import spring.eshwar.repository.ApplicationRepository;
import spring.eshwar.repository.InterviewRepository;

import spring.eshwar.service.email.ResendEmailService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class InterviewService {

    private static final Logger logger = LoggerFactory.getLogger(InterviewService.class);

    private final InterviewRepository interviewRepository;
    private final ApplicationRepository applicationRepository;
    private final ResendEmailService resendEmailService;

    public InterviewService(InterviewRepository interviewRepository,
                            ApplicationRepository applicationRepository,
                            ResendEmailService resendEmailService) {
        this.interviewRepository = interviewRepository;
        this.applicationRepository = applicationRepository;
        this.resendEmailService = resendEmailService;
    }

    @Transactional(readOnly = true)
    public Interview getInterviewById(Long id) {
        return interviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", id));
    }

    @Transactional(readOnly = true)
    public List<Interview> getInterviewsByApplicationId(Long applicationId) {
        return interviewRepository.findByApplicationId(applicationId);
    }

    @Transactional(readOnly = true)
    public List<Interview> getInterviewsByCandidateId(Long candidateId) {
        return interviewRepository.findByApplicationCandidateIdOrderByScheduledDateTimeAsc(candidateId);
    }

    @Transactional(readOnly = true)
    public List<Interview> getInterviewsByStatus(InterviewStatus status) {
        return interviewRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<Interview> getAllInterviewsSortedByDate() {
        return interviewRepository.findAllByOrderByScheduledDateTimeAsc();
    }

    /**
     * Schedules a new interview for an application.
     * Enforces that the candidate must be shortlisted before scheduling.
     * Validates scheduledDateTime (cannot be in the past).
     * Supports ONLINE, OFFLINE, and PHONE interview types.
     * Ensures meeting link for ONLINE interviews.
     * Transitions Application status to INTERVIEW.
     */
    @Transactional
    public Interview scheduleInterview(Interview interview) {
        if (interview.getApplication() == null || interview.getApplication().getId() == null) {
            throw new BadRequestException("Interview must be linked to a valid Application.");
        }

        Long applicationId = interview.getApplication().getId();
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        // Requirement: Candidate must be shortlisted before interview scheduling
        if (application.getStatus() != ApplicationStatus.SHORTLISTED && application.getStatus() != ApplicationStatus.INTERVIEW) {
            throw new BadRequestException("Candidate must be shortlisted before interview scheduling. Current application status: "
                    + application.getStatus());
        }

        // Requirement: Validate scheduledDateTime & prevent past scheduling
        if (interview.getScheduledDateTime() == null) {
            throw new BadRequestException("Scheduled date and time is required.");
        }
        if (interview.getScheduledDateTime().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Interview cannot be scheduled in the past.");
        }

        // Requirement: Support interview types such as ONLINE, OFFLINE, PHONE
        String type = validateAndNormalizeInterviewType(interview.getInterviewType());
        interview.setInterviewType(type);

        // Requirement: Store meetingLink for online interviews
        if ("ONLINE".equalsIgnoreCase(type) || "LIVE_AI".equalsIgnoreCase(type)) {
            if (interview.getMeetingLink() == null || interview.getMeetingLink().isBlank()) {
                interview.setMeetingLink("https://meet.hireranker.com/interview-" + UUID.randomUUID().toString().substring(0, 8));
            } else {
                interview.setMeetingLink(interview.getMeetingLink().trim());
            }
        }

        if (interview.getStatus() == null) {
            interview.setStatus(InterviewStatus.SCHEDULED);
        }

        // Requirement: Update application status to INTERVIEW when appropriate
        application.setStatus(ApplicationStatus.INTERVIEW);
        application.setUpdatedAt(LocalDateTime.now());
        applicationRepository.save(application);

        interview.setApplication(application);
        Interview saved = interviewRepository.save(interview);
        logger.info("Successfully scheduled interview id: {} for application id: {} at {}",
                saved.getId(), applicationId, saved.getScheduledDateTime());

        // Send interview scheduled email after database save succeeds
        try {
            resendEmailService.sendInterviewScheduledEmail(saved);
        } catch (Exception ex) {
            logger.error("Failed to send interview scheduled notification email for interview id {}: {}", saved.getId(), ex.getMessage());
        }

        return saved;
    }

    @Transactional
    public Interview updateInterviewStatus(Long id, InterviewStatus status) {
        if (status == null) {
            throw new BadRequestException("Interview status cannot be null");
        }
        Interview interview = getInterviewById(id);
        interview.setStatus(status);
        return interviewRepository.save(interview);
    }

    @Transactional
    public Interview rescheduleInterview(Long id, LocalDateTime newDateTime, String meetingLink, String notes) {
        if (newDateTime == null) {
            throw new BadRequestException("New scheduled date and time cannot be null.");
        }
        if (newDateTime.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Interview cannot be rescheduled to a past date and time.");
        }

        Interview interview = getInterviewById(id);
        interview.setScheduledDateTime(newDateTime);
        if (meetingLink != null && !meetingLink.isBlank()) {
            interview.setMeetingLink(meetingLink.trim());
        }
        if (notes != null) {
            interview.setNotes(notes);
        }
        interview.setStatus(InterviewStatus.RESCHEDULED);

        Application app = interview.getApplication();
        if (app != null && app.getStatus() != ApplicationStatus.INTERVIEW) {
            app.setStatus(ApplicationStatus.INTERVIEW);
            applicationRepository.save(app);
        }

        logger.info("Rescheduled interview id: {} to {}", id, newDateTime);
        return interviewRepository.save(interview);
    }

    @Transactional
    public Interview rescheduleInterview(Long id, LocalDateTime newDateTime, String meetingLink) {
        return rescheduleInterview(id, newDateTime, meetingLink, null);
    }

    @Transactional
    public Interview cancelInterview(Long id, String reason) {
        Interview interview = getInterviewById(id);
        interview.setStatus(InterviewStatus.CANCELLED);
        if (reason != null && !reason.isBlank()) {
            String updatedNotes = (interview.getNotes() != null ? interview.getNotes() + " | " : "") + "Cancelled: " + reason.trim();
            interview.setNotes(updatedNotes);
        }
        logger.info("Cancelled interview id: {}. Reason: {}", id, reason);
        return interviewRepository.save(interview);
    }

    @Transactional
    public void cancelInterview(Long id) {
        cancelInterview(id, null);
    }

    @Transactional
    public Interview updateInterview(Long id, Interview updated) {
        Interview existing = getInterviewById(id);

        if (updated.getScheduledDateTime() != null) {
            if (updated.getScheduledDateTime().isBefore(LocalDateTime.now())) {
                throw new BadRequestException("Interview cannot be scheduled in the past.");
            }
            existing.setScheduledDateTime(updated.getScheduledDateTime());
        }
        if (updated.getInterviewType() != null) {
            existing.setInterviewType(validateAndNormalizeInterviewType(updated.getInterviewType()));
        }
        if (updated.getMeetingLink() != null) {
            existing.setMeetingLink(updated.getMeetingLink());
        }
        if (updated.getStatus() != null) {
            existing.setStatus(updated.getStatus());
        }
        if (updated.getNotes() != null) {
            existing.setNotes(updated.getNotes());
        }

        return interviewRepository.save(existing);
    }

    @Transactional
    public void deleteInterview(Long id) {
        if (!interviewRepository.existsById(id)) {
            throw new ResourceNotFoundException("Interview", "id", id);
        }
        interviewRepository.deleteById(id);
    }

    private String validateAndNormalizeInterviewType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return "ONLINE";
        }
        String normalized = rawType.trim().toUpperCase();
        if (normalized.equals("ONLINE") || normalized.equals("OFFLINE") || normalized.equals("PHONE") || normalized.equals("LIVE_AI")) {
            return normalized;
        }
        throw new BadRequestException("Invalid interview type: '" + rawType + "'. Supported types are ONLINE, OFFLINE, and PHONE.");
    }
}
