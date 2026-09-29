package spring.eshwar.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import spring.eshwar.dto.screening.ScreeningResultResponse;
import spring.eshwar.entity.Application;
import spring.eshwar.entity.ApplicationStatus;
import spring.eshwar.entity.EvaluationCriteria;
import spring.eshwar.entity.Job;
import spring.eshwar.entity.Resume;
import spring.eshwar.entity.ScreeningResult;
import spring.eshwar.exception.BadRequestException;
import spring.eshwar.exception.ResourceNotFoundException;
import spring.eshwar.repository.ApplicationRepository;
import spring.eshwar.repository.EvaluationCriteriaRepository;
import spring.eshwar.repository.ResumeRepository;
import spring.eshwar.repository.ScreeningResultRepository;
import spring.eshwar.service.ai.AIScreeningProvider;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AIResumeScreeningService {

    private static final Logger log = LoggerFactory.getLogger(AIResumeScreeningService.class);

    private final ApplicationRepository applicationRepository;
    private final ScreeningResultRepository screeningResultRepository;
    private final ResumeRepository resumeRepository;
    private final EvaluationCriteriaRepository evaluationCriteriaRepository;
    private final ResumeTextExtractionService textExtractionService;
    private final spring.eshwar.service.ai.AIProvider aiProvider;
    private final TransactionTemplate transactionTemplate;

    public AIResumeScreeningService(ApplicationRepository applicationRepository,
                                    ScreeningResultRepository screeningResultRepository,
                                    ResumeRepository resumeRepository,
                                    EvaluationCriteriaRepository evaluationCriteriaRepository,
                                    ResumeTextExtractionService textExtractionService,
                                    spring.eshwar.service.ai.AIProvider aiProvider,
                                    PlatformTransactionManager transactionManager) {
        this.applicationRepository = applicationRepository;
        this.screeningResultRepository = screeningResultRepository;
        this.resumeRepository = resumeRepository;
        this.evaluationCriteriaRepository = evaluationCriteriaRepository;
        this.textExtractionService = textExtractionService;
        this.aiProvider = aiProvider;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * Executes the AI resume screening process with decoupled database lifecycle:
     * Phase 1 (DB Tx): Validate & load context, update status to SCREENING -> Tx commits, DB connection released!
     * Phase 2 (No Tx): Invoke remote AI provider -> Zero database connections held during external HTTP call!
     * Phase 3 (DB Tx): Persist ScreeningResult and transition application status -> Short Tx commits!
     *
     * @param applicationId the ID of the application to screen
     * @param force if true, forces re-screening even if a valid result exists
     * @return the screening result response
     */
    public ScreeningResultResponse screenApplication(Long applicationId, boolean force) {
        // Phase 1: Retrieve data, perform text extraction if missing, and prepare context (in short transaction)
        ScreeningPreparationData prep = transactionTemplate.execute(status -> prepareScreeningContext(applicationId, force));
        if (prep == null) {
            throw new BadRequestException("Failed to prepare screening context for application " + applicationId);
        }

        // If an existing result was found and force is false, return it immediately without calling AI
        if (prep.existingResponse() != null) {
            return prep.existingResponse();
        }

        // Phase 2: Invoke AI Provider OUTSIDE any database transaction
        // Ensures database connection pool is NEVER exhausted by external AI network latency
        log.info("[AIResumeScreeningService] Calling AI screening provider '{}' for application id {} (Job: '{}')",
                aiProvider.getProviderName(), applicationId, prep.jobTitle());
        AIScreeningProvider.ScreeningEvaluationResult evalResult;
        try {
            evalResult = aiProvider.evaluate(prep.context());
        } catch (Exception e) {
            log.error("[AIResumeScreeningService] AI provider threw exception: {}. Using fallback.", e.getMessage());
            evalResult = aiProvider.evaluateFallback(prep.context(), "Provider error: " + e.getMessage());
        }

        final AIScreeningProvider.ScreeningEvaluationResult finalEvalResult = evalResult;

        // Phase 3: Persist results in a dedicated, short database transaction
        return transactionTemplate.execute(status -> persistScreeningResult(applicationId, finalEvalResult));
    }

    private ScreeningPreparationData prepareScreeningContext(Long applicationId, boolean force) {
        // 1. Retrieve and validate Application
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        // 2. Check if valid ScreeningResult already exists and force is false
        Optional<ScreeningResult> existingOpt = screeningResultRepository.findByApplicationId(applicationId);
        if (existingOpt.isPresent() && !force) {
            log.info("Application id {} already has a screening result. Returning existing record without calling AI.", applicationId);
            return new ScreeningPreparationData(null, application.getJob() != null ? application.getJob().getTitle() : "",
                    ScreeningResultResponse.fromEntity(existingOpt.get()));
        }

        // 3. Retrieve and validate Resume
        Resume resume = application.getResume();
        if (resume == null && application.getCandidate() != null) {
            resume = resumeRepository.findByCandidateId(application.getCandidate().getId()).orElse(null);
            if (resume != null) {
                application.setResume(resume);
            }
        }

        if (resume == null) {
            throw new BadRequestException("Application id " + applicationId + " has no associated resume to evaluate.");
        }

        // 4. Ensure Resume has extracted text
        if (resume.getExtractedText() == null || resume.getExtractedText().isBlank() || resume.getExtractedText().startsWith("[EXTRACTION_FAILED")) {
            log.info("[AIResumeScreeningService] Resume text missing or marked as failed for resume id {}. Triggering text extraction now.", resume.getId());
            textExtractionService.extractText(resume);
            resume = resumeRepository.findById(resume.getId()).orElse(resume);
        }

        // 5. Update Application status to SCREENING while screening is underway
        application.setStatus(ApplicationStatus.SCREENING);
        applicationRepository.save(application);

        // 6. Gather Job details and Evaluation Criteria
        Job job = application.getJob();
        if (job == null) {
            throw new BadRequestException("Application id " + applicationId + " is not associated with a valid job.");
        }

        EvaluationCriteria criteria = evaluationCriteriaRepository.findByJobId(job.getId()).orElse(null);

        String requiredSkills = (criteria != null && criteria.getRequiredSkills() != null && !criteria.getRequiredSkills().isBlank())
                ? criteria.getRequiredSkills()
                : (job.getRequiredSkills() != null ? job.getRequiredSkills() : "");

        Double minExperience = (criteria != null && criteria.getMinimumExperience() != null)
                ? criteria.getMinimumExperience()
                : 0.0;

        String educationRequirements = (criteria != null && criteria.getEducationRequirements() != null)
                ? criteria.getEducationRequirements()
                : "Bachelor's Degree or equivalent";

        Double skillWeight = (criteria != null && criteria.getSkillWeight() != null) ? criteria.getSkillWeight() : 50.0;
        Double expWeight = (criteria != null && criteria.getExperienceWeight() != null) ? criteria.getExperienceWeight() : 30.0;
        Double eduWeight = (criteria != null && criteria.getEducationWeight() != null) ? criteria.getEducationWeight() : 20.0;

        // 7. Assemble ScreeningContext
        String sanitizedResumeText = sanitizePersonalInformation(resume.getExtractedText());

        AIScreeningProvider.ScreeningContext context = new AIScreeningProvider.ScreeningContext(
                job.getTitle(),
                job.getDescription() != null ? job.getDescription() : "",
                requiredSkills,
                job.getExperienceRequired() != null ? job.getExperienceRequired() : "",
                minExperience,
                educationRequirements,
                skillWeight,
                expWeight,
                eduWeight,
                sanitizedResumeText
        );

        return new ScreeningPreparationData(context, job.getTitle(), null);
    }

    private ScreeningResultResponse persistScreeningResult(Long applicationId, AIScreeningProvider.ScreeningEvaluationResult evalResult) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        Optional<ScreeningResult> existingOpt = screeningResultRepository.findByApplicationId(applicationId);
        ScreeningResult screeningResult = existingOpt.orElseGet(ScreeningResult::new);
        screeningResult.setApplication(application);
        screeningResult.setOverallScore(evalResult.overallScore());
        screeningResult.setSkillsScore(evalResult.skillsScore());
        screeningResult.setExperienceScore(evalResult.experienceScore());
        screeningResult.setEducationScore(evalResult.educationScore());
        screeningResult.setKeywordScore(evalResult.keywordScore());
        screeningResult.setProjectScore(evalResult.projectScore());
        screeningResult.setCertificationScore(evalResult.certificationScore());
        screeningResult.setFormattingScore(evalResult.formattingScore());
        screeningResult.setAchievementScore(evalResult.achievementScore());
        screeningResult.setMatchingSkills(evalResult.matchingSkills());
        screeningResult.setMissingSkills(evalResult.missingSkills());
        screeningResult.setRecommendedSkills(evalResult.recommendedSkills());
        screeningResult.setStrengths(evalResult.strengths());
        screeningResult.setWeaknesses(evalResult.weaknesses());
        screeningResult.setImprovementSuggestions(evalResult.improvementSuggestions());
        screeningResult.setResumeSummary(evalResult.resumeSummary());
        screeningResult.setRecommendation(evalResult.recommendation());
        screeningResult.setScreenedAt(LocalDateTime.now());

        ScreeningResult savedResult = screeningResultRepository.save(screeningResult);

        // Update Application status appropriately after successful screening
        if ("Strong Match".equalsIgnoreCase(evalResult.recommendation()) || "RECOMMENDED".equalsIgnoreCase(evalResult.recommendation()) || evalResult.overallScore() >= 75.0) {
            application.setStatus(ApplicationStatus.SHORTLISTED);
        } else if ("Weak Match".equalsIgnoreCase(evalResult.recommendation()) || "NOT_RECOMMENDED".equalsIgnoreCase(evalResult.recommendation()) || evalResult.overallScore() < 50.0) {
            application.setStatus(ApplicationStatus.REJECTED);
        } else {
            application.setStatus(ApplicationStatus.SCREENING);
        }
        applicationRepository.save(application);

        log.info("Completed AI screening for application id {}. Overall Score: {}, Recommendation: {}",
                applicationId, savedResult.getOverallScore(), savedResult.getRecommendation());

        return ScreeningResultResponse.fromEntity(savedResult);
    }

    /**
     * Sanitizes personal contact details (emails, phone numbers) to ensure
     * unnecessary PII is never transmitted to external AI APIs.
     */
    private String sanitizePersonalInformation(String text) {
        if (text == null) {
            return "";
        }
        // Redact email addresses
        String sanitized = text.replaceAll("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}", "[REDACTED_EMAIL]");
        // Redact phone numbers (standard international and US formats)
        sanitized = sanitized.replaceAll("(\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}", "[REDACTED_PHONE]");
        return sanitized;
    }

    private record ScreeningPreparationData(
            AIScreeningProvider.ScreeningContext context,
            String jobTitle,
            ScreeningResultResponse existingResponse
    ) {}
}
