package spring.eshwar.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.eshwar.dto.application.ShortlistedCandidateResponse;
import spring.eshwar.entity.Application;
import spring.eshwar.entity.ApplicationStatus;
import spring.eshwar.entity.Candidate;
import spring.eshwar.entity.Job;
import spring.eshwar.entity.Resume;
import spring.eshwar.entity.ScreeningResult;
import spring.eshwar.exception.BadRequestException;
import spring.eshwar.exception.DuplicateResourceException;
import spring.eshwar.exception.ResourceNotFoundException;
import spring.eshwar.repository.ApplicationRepository;
import spring.eshwar.repository.CandidateRepository;
import spring.eshwar.repository.InterviewAnswerRepository;
import spring.eshwar.repository.InterviewIntegrityEventRepository;
import spring.eshwar.repository.InterviewQuestionRepository;
import spring.eshwar.repository.InterviewRepository;
import spring.eshwar.repository.InterviewResultRepository;
import spring.eshwar.repository.JobRepository;
import spring.eshwar.repository.ResumeRepository;
import spring.eshwar.repository.ScreeningResultRepository;

import spring.eshwar.service.email.ResendEmailService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationService.class);

    private final ApplicationRepository applicationRepository;
    private final CandidateRepository candidateRepository;
    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;
    private final ScreeningResultRepository screeningResultRepository;
    private final InterviewRepository interviewRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final InterviewAnswerRepository interviewAnswerRepository;
    private final InterviewResultRepository interviewResultRepository;
    private final InterviewIntegrityEventRepository interviewIntegrityEventRepository;
    private final ResendEmailService resendEmailService;

    public ApplicationService(ApplicationRepository applicationRepository,
                              CandidateRepository candidateRepository,
                              JobRepository jobRepository,
                              ResumeRepository resumeRepository,
                              ScreeningResultRepository screeningResultRepository,
                              InterviewRepository interviewRepository,
                              InterviewQuestionRepository interviewQuestionRepository,
                              InterviewAnswerRepository interviewAnswerRepository,
                              InterviewResultRepository interviewResultRepository,
                              InterviewIntegrityEventRepository interviewIntegrityEventRepository,
                              ResendEmailService resendEmailService) {
        this.applicationRepository = applicationRepository;
        this.candidateRepository = candidateRepository;
        this.jobRepository = jobRepository;
        this.resumeRepository = resumeRepository;
        this.screeningResultRepository = screeningResultRepository;
        this.interviewRepository = interviewRepository;
        this.interviewQuestionRepository = interviewQuestionRepository;
        this.interviewAnswerRepository = interviewAnswerRepository;
        this.interviewResultRepository = interviewResultRepository;
        this.interviewIntegrityEventRepository = interviewIntegrityEventRepository;
        this.resendEmailService = resendEmailService;
    }

    @Transactional(readOnly = true)
    public Application getApplicationById(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", id));
    }

    @Transactional(readOnly = true)
    public List<Application> getApplicationsByCandidate(Long candidateId) {
        return applicationRepository.findByCandidateId(candidateId);
    }

    @Transactional(readOnly = true)
    public List<Application> getApplicationsByJob(Long jobId) {
        return applicationRepository.findByJobId(jobId);
    }

    @Transactional(readOnly = true)
    public List<Application> getAllApplications() {
        return applicationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public boolean existsByCandidateAndJob(Long candidateId, Long jobId) {
        return applicationRepository.existsByCandidateIdAndJobId(candidateId, jobId);
    }

    @Transactional
    public Application applyToJob(Long candidateId, Long jobId, Long resumeId) {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate", "id", candidateId));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId));

        if (applicationRepository.existsByCandidateIdAndJobId(candidateId, jobId)) {
            throw new DuplicateResourceException("Candidate has already applied for this job.");
        }

        Resume resume = null;
        if (resumeId != null) {
            resume = resumeRepository.findById(resumeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Resume", "id", resumeId));
        }

        Application application = new Application(candidate, job, resume, ApplicationStatus.APPLIED);
        return applicationRepository.save(application);
    }

    @Transactional
    public Application updateApplicationStatus(Long id, ApplicationStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Application status cannot be null");
        }
        Application application = getApplicationById(id);
        ApplicationStatus previousStatus = application.getStatus();

        // Check if status is already the same to prevent redundant duplicate emails and saves
        if (previousStatus == status) {
            logger.info("Application id {} already has status {}. Skipping redundant status update and email notification.", id, status);
            return application;
        }

        application.setStatus(status);
        application.setUpdatedAt(LocalDateTime.now());
        Application saved = applicationRepository.save(application);

        // Send notifications strictly after database save succeeds and status has changed
        if (status == ApplicationStatus.REJECTED) {
            try {
                resendEmailService.sendRejectedEmail(saved);
            } catch (Exception ex) {
                logger.error("Failed to send rejection email for application id {}: {}", id, ex.getMessage());
            }
        } else if (status == ApplicationStatus.SHORTLISTED) {
            try {
                resendEmailService.sendShortlistedEmail(saved);
            } catch (Exception ex) {
                logger.error("Failed to send shortlist email for application id {}: {}", id, ex.getMessage());
            }
        }

        return saved;
    }

    @Transactional
    public void deleteApplication(Long id) {
        if (!applicationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Application", "id", id);
        }
        deleteApplicationCascade(id);
    }

    /**
     * Safely deletes all applications and all child interview/screening records
     * without deleting users, candidates, resumes, or jobs.
     */
    @Transactional
    public void deleteAllApplications() {
        logger.info("Admin requested cleanup of all existing applications.");
        List<Application> allApps = applicationRepository.findAll();
        for (Application app : allApps) {
            deleteApplicationCascade(app.getId());
        }
        logger.info("Successfully cleaned up {} applications and associated interview/screening records.", allApps.size());
    }

    /**
     * Safely deletes child records (interviews, answers, questions, results, integrity events, screening results)
     * before deleting the application itself, preserving referential integrity.
     */
    @Transactional
    public void deleteApplicationCascade(Long applicationId) {
        // 1. Find all interviews for this application
        List<spring.eshwar.entity.Interview> interviews = interviewRepository.findByApplicationId(applicationId);
        for (spring.eshwar.entity.Interview interview : interviews) {
            Long interviewId = interview.getId();
            // Delete integrity events
            interviewIntegrityEventRepository.deleteByInterviewId(interviewId);
            // Delete interview result
            interviewResultRepository.deleteByInterviewId(interviewId);
            // Delete interview answers
            interviewAnswerRepository.deleteByQuestionInterviewId(interviewId);
            // Delete interview questions
            interviewQuestionRepository.deleteByInterviewId(interviewId);
            // Delete interview
            interviewRepository.deleteById(interviewId);
        }

        // 2. Delete screening result
        screeningResultRepository.deleteByApplicationId(applicationId);

        // 3. Delete application
        applicationRepository.deleteById(applicationId);
    }

    /**
     * Shortlists an existing application in-place, updating status to SHORTLISTED.
     * Verifies screening completion where appropriate.
     * Sends shortlist transactional email via Brevo after DB update succeeds.
     * Prevents duplicate emails if already SHORTLISTED.
     *
     * @param applicationId the ID of the application to shortlist
     * @return the updated Application entity
     */
    @Transactional
    public Application shortlistApplication(Long applicationId) {
        // 1. Application must exist
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        // Duplicate-action protection: If already SHORTLISTED, return existing without resending email
        if (application.getStatus() == ApplicationStatus.SHORTLISTED) {
            logger.info("Application id {} is already SHORTLISTED. Skipping duplicate shortlist email and update.", applicationId);
            return application;
        }

        // 2. Prevent invalid state transitions: Only prevent shortlisting if already HIRED or active in INTERVIEW
        if (application.getStatus() == ApplicationStatus.HIRED) {
            throw new BadRequestException("Cannot shortlist an application that is already HIRED.");
        }
        if (application.getStatus() == ApplicationStatus.INTERVIEW) {
            throw new BadRequestException("Cannot shortlist an application that has already advanced to INTERVIEW status.");
        }

        // 3. Screening verification (If screening result exists, log score; allow manual shortlisting even before automated screening)
        Optional<ScreeningResult> screeningOpt = screeningResultRepository.findByApplicationId(applicationId);
        if (screeningOpt.isPresent()) {
            logger.info("Application id {} has completed screening with score: {}. Shortlisting candidate.",
                    applicationId, screeningOpt.get().getOverallScore());
        } else {
            logger.info("Application id {} has no prior screening result. Shortlisting directly.", applicationId);
        }

        // 4. Update status to SHORTLISTED
        application.setStatus(ApplicationStatus.SHORTLISTED);
        application.setUpdatedAt(LocalDateTime.now());
        Application saved = applicationRepository.save(application);

        // 5. Send shortlist email after database save succeeds
        try {
            resendEmailService.sendShortlistedEmail(saved);
        } catch (Exception ex) {
            logger.error("Failed to send shortlist email for application id {}: {}", applicationId, ex.getMessage());
        }

        return saved;
    }

    /**
     * Retrieves all shortlisted candidates for a specific job along with
     * candidate details, application status, resume metadata, and screening scores.
     *
     * @param jobId the ID of the job
     * @return list of ShortlistedCandidateResponse DTOs
     */
    @Transactional(readOnly = true)
    public List<ShortlistedCandidateResponse> getShortlistedCandidatesByJob(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId));

        List<Application> shortlistedApps = applicationRepository.findByJobIdAndStatusIn(
                job.getId(), List.of(ApplicationStatus.SHORTLISTED, ApplicationStatus.INTERVIEW));
        logger.info("Found {} shortlisted applications for job id: {} ('{}')",
                shortlistedApps.size(), jobId, job.getTitle());

        return shortlistedApps.stream().map(app -> {
            ScreeningResult sr = screeningResultRepository.findByApplicationId(app.getId()).orElse(null);
            return ShortlistedCandidateResponse.of(app, sr);
        }).toList();
    }

    /**
     * Retrieves all shortlisted candidates across all jobs.
     *
     * @return list of ShortlistedCandidateResponse DTOs
     */
    @Transactional(readOnly = true)
    public List<ShortlistedCandidateResponse> getAllShortlistedCandidates() {
        List<Application> shortlistedApps = applicationRepository.findByStatusIn(
                List.of(ApplicationStatus.SHORTLISTED, ApplicationStatus.INTERVIEW));
        logger.info("Found {} total shortlisted applications across all jobs.", shortlistedApps.size());

        return shortlistedApps.stream().map(app -> {
            ScreeningResult sr = screeningResultRepository.findByApplicationId(app.getId()).orElse(null);
            return ShortlistedCandidateResponse.of(app, sr);
        }).toList();
    }
}
