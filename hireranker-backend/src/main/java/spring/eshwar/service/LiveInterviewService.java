package spring.eshwar.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.eshwar.dto.interview.LiveInterviewResultResponse;
import spring.eshwar.dto.interview.LiveQuestionResponse;
import spring.eshwar.dto.interview.SkipLiveQuestionRequest;
import spring.eshwar.dto.interview.StartInterviewRequest;
import spring.eshwar.dto.interview.StartInterviewResponse;
import spring.eshwar.dto.interview.StructuredCandidateProfileDto;
import spring.eshwar.dto.interview.SubmitLiveAnswerRequest;
import spring.eshwar.dto.interview.SubmitLiveAnswerResponse;
import spring.eshwar.entity.Application;
import spring.eshwar.entity.ApplicationStatus;
import spring.eshwar.entity.Interview;
import spring.eshwar.entity.InterviewAnswer;
import spring.eshwar.entity.InterviewQuestion;
import spring.eshwar.entity.InterviewResult;
import spring.eshwar.entity.InterviewStatus;
import spring.eshwar.entity.QuestionStatus;
import spring.eshwar.exception.BadRequestException;
import spring.eshwar.exception.ResourceNotFoundException;
import spring.eshwar.repository.ApplicationRepository;
import spring.eshwar.repository.InterviewAnswerRepository;
import spring.eshwar.repository.InterviewQuestionRepository;
import spring.eshwar.repository.InterviewRepository;
import spring.eshwar.repository.InterviewResultRepository;
import spring.eshwar.repository.InterviewIntegrityEventRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class LiveInterviewService {

    private static final Logger log = LoggerFactory.getLogger(LiveInterviewService.class);

    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final InterviewAnswerRepository interviewAnswerRepository;
    private final InterviewResultRepository interviewResultRepository;
    private final InterviewIntegrityEventRepository interviewIntegrityEventRepository;
    private final AiInterviewService aiInterviewService;

    public LiveInterviewService(ApplicationRepository applicationRepository,
                                InterviewRepository interviewRepository,
                                InterviewQuestionRepository interviewQuestionRepository,
                                InterviewAnswerRepository interviewAnswerRepository,
                                InterviewResultRepository interviewResultRepository,
                                InterviewIntegrityEventRepository interviewIntegrityEventRepository,
                                AiInterviewService aiInterviewService) {
        this.applicationRepository = applicationRepository;
        this.interviewRepository = interviewRepository;
        this.interviewQuestionRepository = interviewQuestionRepository;
        this.interviewAnswerRepository = interviewAnswerRepository;
        this.interviewResultRepository = interviewResultRepository;
        this.interviewIntegrityEventRepository = interviewIntegrityEventRepository;
        this.aiInterviewService = aiInterviewService;
    }

    /**
     * Starts or resumes a live interview session for a candidate's application.
     */
    public StartInterviewResponse startInterview(StartInterviewRequest request) {
        if (request == null || (request.getApplicationId() == null && request.getInterviewId() == null)) {
            throw new BadRequestException("Either applicationId or interviewId is required to start an interview session.");
        }

        Application application;
        Interview interview;

        if (request.getInterviewId() != null) {
            interview = interviewRepository.findByIdWithDetails(request.getInterviewId())
                    .orElse(null);
            if (interview != null) {
                application = interview.getApplication();
            } else if (request.getApplicationId() != null) {
                application = applicationRepository.findById(request.getApplicationId()).orElse(null);
            } else {
                application = applicationRepository.findAll().stream().findFirst().orElse(null);
            }

            if (application == null) {
                throw new BadRequestException("Interview is not linked to any valid application.");
            }

            // Enforce Retake Rule: Incomplete or abandoned interviews cannot be freely retaken without HR authorization
            if (interview != null) {
                if (interview.getStatus() == InterviewStatus.COMPLETED) {
                    throw new BadRequestException("This assessment session has already been completed. Retakes are not permitted.");
                }
                if (interview.getStatus() == InterviewStatus.ABANDONED
                        || interview.getStatus() == InterviewStatus.PENDING_HR_REVIEW
                        || interview.getStatus() == InterviewStatus.DISCONNECTED) {
                    throw new BadRequestException("This assessment session was abandoned or closed early. Retaking requires HR/administrator authorization.");
                }
                if (interview.getStatus() == InterviewStatus.RETAKE_APPROVED) {
                    log.info("[LiveInterviewService] Retake approved by HR. Initializing fresh live session for application id {}", application.getId());
                    interview.setStatus(InterviewStatus.RESCHEDULED);
                    interviewRepository.save(interview);
                    Interview newInterview = new Interview(
                            application,
                            LocalDateTime.now(),
                            "AI_LIVE_INTERVIEW",
                            null,
                            InterviewStatus.IN_PROGRESS,
                            "Live AI interview session (HR Approved Retake)"
                    );
                    interview = interviewRepository.save(newInterview);
                }
            } else {
                // Initialize initial active session for application
                log.info("[LiveInterviewService] Initializing initial active live session for application id {}", application.getId());
                Interview newInterview = new Interview(
                        application,
                        LocalDateTime.now(),
                        "AI_LIVE_INTERVIEW",
                        null,
                        InterviewStatus.IN_PROGRESS,
                        "Live AI interview session"
                );
                interview = interviewRepository.save(newInterview);
            }
        } else {
            application = applicationRepository.findById(request.getApplicationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Application", "id", request.getApplicationId()));

            final Application appRef = application;
            var inProgressOpt = interviewRepository.findFirstByApplicationIdAndStatus(application.getId(), InterviewStatus.IN_PROGRESS);
            log.info("[LiveInterviewService] inProgressOpt for app {}: {}", application.getId(), inProgressOpt.map(i -> i.getId() + ":" + i.getStatus()).orElse(null));
            if (inProgressOpt.isPresent()) {
                interview = inProgressOpt.get();
            } else {
                var retakeInterviews = interviewRepository.findByApplicationIdAndStatus(application.getId(), InterviewStatus.RETAKE_APPROVED);
                log.info("[LiveInterviewService] retakeInterviews for app {}: {}", application.getId(), retakeInterviews.stream().map(i -> i.getId() + ":" + i.getStatus()).toList());
                if (!retakeInterviews.isEmpty()) {
                    log.info("[LiveInterviewService] Retake is authorized by HR for application id {}", application.getId());
                    for (Interview prev : retakeInterviews) {
                        prev.setStatus(InterviewStatus.RESCHEDULED);
                        interviewRepository.save(prev);
                    }
                    Interview newInterview = new Interview(
                            application,
                            LocalDateTime.now(),
                            "AI_LIVE_INTERVIEW",
                            null,
                            InterviewStatus.IN_PROGRESS,
                            "Live AI interview session (HR Approved Retake)"
                    );
                    interview = interviewRepository.save(newInterview);
                } else {
                    // Check if application has any completed or abandoned assessment without HR retake approval
                    var allInterviews = interviewRepository.findByApplicationId(application.getId());
                    log.info("[LiveInterviewService] allInterviews for app {}: {}", application.getId(), allInterviews.stream().map(i -> i.getId() + ":" + i.getStatus()).toList());
                    boolean hasCompleted = allInterviews.stream()
                            .anyMatch(i -> i.getStatus() == InterviewStatus.COMPLETED);
                    if (hasCompleted) {
                        throw new BadRequestException("This assessment has already been completed. Retakes are not permitted.");
                    }
                    boolean hasAbandoned = allInterviews.stream()
                            .anyMatch(i -> i.getStatus() == InterviewStatus.PENDING_HR_REVIEW
                                    || i.getStatus() == InterviewStatus.ABANDONED
                                    || i.getStatus() == InterviewStatus.DISCONNECTED);
                    if (hasAbandoned) {
                        throw new BadRequestException("This assessment was abandoned or closed early. Retaking requires HR/administrator authorization.");
                    }
                    interview = interviewRepository.findFirstByApplicationIdAndStatus(appRef.getId(), InterviewStatus.SCHEDULED)
                            .orElseGet(() -> interviewRepository.findFirstByApplicationIdAndStatus(appRef.getId(), InterviewStatus.READY)
                                    .orElseGet(() -> {
                                        Interview newInterview = new Interview(
                                                appRef,
                                                LocalDateTime.now(),
                                                "AI_LIVE_INTERVIEW",
                                                null,
                                                InterviewStatus.IN_PROGRESS,
                                                "Live AI interview session"
                                        );
                                        return interviewRepository.save(newInterview);
                                    }));
                }
            }
        }

        if (interview.getStatus() != InterviewStatus.IN_PROGRESS) {
            interview.setStatus(InterviewStatus.IN_PROGRESS);
            interview = interviewRepository.save(interview);
        }

        // Update application status to INTERVIEW
        if (application.getStatus() != ApplicationStatus.INTERVIEW) {
            application.setStatus(ApplicationStatus.INTERVIEW);
            applicationRepository.save(application);
        }

        // Fetch or create the first question
        InterviewQuestion activeQuestion = interviewQuestionRepository
                .findFirstByInterviewIdAndStatusOrderByQuestionOrderAsc(interview.getId(), QuestionStatus.PENDING)
                .orElse(null);

        if (activeQuestion == null) {
            long existingQuestionsCount = interviewQuestionRepository.countByInterviewId(interview.getId());
            if (existingQuestionsCount == 0) {
                // Generate question #1
                var genQ = aiInterviewService.generateNextQuestion(
                        application.getCandidate(),
                        application.getResume(),
                        application.getJob(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        1
                );

                InterviewQuestion q1 = new InterviewQuestion(
                        interview,
                        genQ.questionText(),
                        genQ.category(),
                        genQ.difficulty(),
                        1,
                        genQ.expectedConcepts(),
                        genQ.keyPoints()
                );
                activeQuestion = interviewQuestionRepository.save(q1);
            } else {
                // Generate next pending question
                activeQuestion = generateAndSaveNextQuestion(interview, application, (int) existingQuestionsCount + 1);
            }
        }

        String candidateName = (application.getCandidate() != null) ? application.getCandidate().getFullName() : "Candidate";
        String jobTitle = (application.getJob() != null) ? application.getJob().getTitle() : "Software Engineer";
        int dynamicTarget = calculateDynamicTargetQuestions(application);

        String introductionText = String.format(
                "Welcome to your HireRanker technical interview, %s. I am your AI Technical Interviewer for the %s position. I will ask you a series of technical questions based on your skills and experience. Please answer naturally and clearly. Your responses will be evaluated on technical knowledge, problem-solving, and communication. Let's begin.",
                candidateName, jobTitle
        );

        return new StartInterviewResponse(
                interview.getId(),
                application.getId(),
                candidateName,
                jobTitle,
                interview.getStatus().name(),
                dynamicTarget,
                LiveQuestionResponse.fromEntity(activeQuestion),
                introductionText
        );
    }

    /**
     * Retrieves the current pending question or generates the next adaptive question.
     */
    public LiveQuestionResponse getNextQuestion(Long interviewId) {
        Interview interview = interviewRepository.findByIdWithDetails(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId));

        if (interview.getStatus() == InterviewStatus.COMPLETED) {
            throw new BadRequestException("Interview session has already been completed.");
        }

        // Check if there is an active pending question
        var pendingOpt = interviewQuestionRepository
                .findFirstByInterviewIdAndStatusOrderByQuestionOrderAsc(interview.getId(), QuestionStatus.PENDING);
        if (pendingOpt.isPresent()) {
            return LiveQuestionResponse.fromEntity(pendingOpt.get());
        }

        // Check total questions count and dynamic completion criteria
        long totalAsked = interviewQuestionRepository.countByInterviewId(interview.getId());
        if (shouldConcludeInterview(interview, totalAsked)) {
            log.info("Interview {} complete after {} questions. Finalizing interview.", interviewId, totalAsked);
            completeInterview(interviewId);
            return null;
        }

        InterviewQuestion nextQ = generateAndSaveNextQuestion(interview, interview.getApplication(), (int) totalAsked + 1);
        return LiveQuestionResponse.fromEntity(nextQ);
    }

    /**
     * Submits a candidate's spoken answer transcript and receives immediate feedback and next question.
     */
    public SubmitLiveAnswerResponse submitAnswer(Long interviewId, Long questionId, SubmitLiveAnswerRequest request) {
        Interview interview = interviewRepository.findByIdWithDetails(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId));

        InterviewQuestion question = interviewQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("InterviewQuestion", "id", questionId));

        if (!question.getInterview().getId().equals(interviewId)) {
            throw new BadRequestException("Question ID " + questionId + " does not belong to Interview ID " + interviewId);
        }

        if (question.getStatus() != QuestionStatus.PENDING) {
            throw new BadRequestException("Question has already been " + question.getStatus().name().toLowerCase() + ".");
        }

        // AI Answer Evaluation (Zero-Bias)
        var evaluated = aiInterviewService.evaluateAnswer(
                question,
                request.getAnswerText(),
                request.getResponseTimeSeconds(),
                request.getAnswerDurationSeconds(),
                interview.getApplication().getJob()
        );

        // Mark question as answered
        question.setStatus(QuestionStatus.ANSWERED);
        interviewQuestionRepository.save(question);

        boolean isAssisted = Boolean.TRUE.equals(request.getAssisted());
        Double techScore = isAssisted ? 0.0 : evaluated.technicalScore();
        Double clarityScore = evaluated.clarityScore();
        Double completenessScore = evaluated.completenessScore();
        Double confidenceScore = evaluated.confidenceScore();
        Double overallScore = isAssisted ? 0.0 : evaluated.overallScore();
        String feedback = isAssisted ? "Assisted practice response; excluded from technical scoring." : evaluated.feedback();
        Boolean isCorrect = isAssisted ? false : evaluated.correct();

        // Save candidate answer record
        InterviewAnswer answer = new InterviewAnswer(
                question,
                request.getAnswerText(),
                request.getResponseTimeSeconds(),
                request.getAnswerDurationSeconds(),
                request.getAudioRecordingUrl()
        );
        answer.setTechnicalScore(techScore);
        answer.setClarityScore(clarityScore);
        answer.setCompletenessScore(completenessScore);
        answer.setConfidenceScore(confidenceScore);
        answer.setOverallScore(overallScore);
        answer.setFeedback(feedback);
        interviewAnswerRepository.save(answer);

        // Check if all target questions have been answered or reached adaptive completion
        long totalAsked = interviewQuestionRepository.countByInterviewId(interviewId);
        boolean isFinished = shouldConcludeInterview(interview, totalAsked);

        LiveQuestionResponse nextQuestion = null;
        if (isFinished) {
            log.info("Interview {} complete after {} questions. Synthesizing final scorecard.", interviewId, totalAsked);
            completeInterview(interviewId);
        } else {
            InterviewQuestion nextQ = generateAndSaveNextQuestion(interview, interview.getApplication(), (int) totalAsked + 1);
            nextQuestion = LiveQuestionResponse.fromEntity(nextQ);
        }

        String classification = evaluated.correctnessClassification() != null
                ? evaluated.correctnessClassification()
                : (isCorrect ? "CORRECT" : "INCORRECT");

        int dynamicTarget = calculateDynamicTargetQuestions(interview.getApplication());

        SubmitLiveAnswerResponse response = new SubmitLiveAnswerResponse(
                interviewId,
                questionId,
                QuestionStatus.ANSWERED.name(),
                feedback,
                techScore,
                clarityScore,
                completenessScore,
                confidenceScore,
                overallScore,
                isCorrect,
                classification,
                evaluated.explanation(),
                evaluated.missingConcepts(),
                evaluated.strengths(),
                evaluated.improvements(),
                isFinished,
                nextQuestion
        );
        response.setTotalQuestionsTarget(dynamicTarget);
        return response;
    }

    /**
     * Handles skipping a question (triggered by the 15-second speech countdown expiration or explicit skip).
     */
    public SubmitLiveAnswerResponse skipQuestion(Long interviewId, Long questionId, SkipLiveQuestionRequest request) {
        Interview interview = interviewRepository.findByIdWithDetails(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId));

        InterviewQuestion question = interviewQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("InterviewQuestion", "id", questionId));

        if (!question.getInterview().getId().equals(interviewId)) {
            throw new BadRequestException("Question ID " + questionId + " does not belong to Interview ID " + interviewId);
        }

        if (question.getStatus() != QuestionStatus.PENDING) {
            throw new BadRequestException("Question has already been " + question.getStatus().name().toLowerCase() + ".");
        }

        String reason = (request != null && request.getReason() != null) ? request.getReason() : "TIMEOUT_15S";
        Double responseTime = (request != null && request.getResponseTimeSeconds() != null) ? request.getResponseTimeSeconds() : 15.0;

        question.setStatus(QuestionStatus.SKIPPED);
        interviewQuestionRepository.save(question);

        // Record a skipped answer with 0 score and descriptive feedback
        InterviewAnswer skippedAnswer = new InterviewAnswer(
                question,
                "[SKIPPED: " + reason + "]",
                responseTime,
                0.0,
                null
        );
        skippedAnswer.setTechnicalScore(0.0);
        skippedAnswer.setClarityScore(0.0);
        skippedAnswer.setFeedback("Question was skipped (" + reason + "). No speech detected within the 15-second response window.");
        interviewAnswerRepository.save(skippedAnswer);

        long totalAsked = interviewQuestionRepository.countByInterviewId(interviewId);
        boolean isFinished = shouldConcludeInterview(interview, totalAsked);

        LiveQuestionResponse nextQuestion = null;
        if (isFinished) {
            completeInterview(interviewId);
        } else {
            InterviewQuestion nextQ = generateAndSaveNextQuestion(interview, interview.getApplication(), (int) totalAsked + 1);
            nextQuestion = LiveQuestionResponse.fromEntity(nextQ);
        }

        int dynamicTarget = calculateDynamicTargetQuestions(interview.getApplication());

        SubmitLiveAnswerResponse response = new SubmitLiveAnswerResponse(
                interviewId,
                questionId,
                QuestionStatus.SKIPPED.name(),
                skippedAnswer.getFeedback(),
                0.0,
                0.0,
                false,
                "INCORRECT",
                "Question was skipped without response.",
                Collections.emptyList(),
                isFinished,
                nextQuestion
        );
        response.setTotalQuestionsTarget(dynamicTarget);
        return response;
    }

    /**
     * Concludes the interview and compiles the zero-bias multi-dimensional evaluation scorecard.
     */
    @Transactional
    public LiveInterviewResultResponse completeInterview(Long interviewId) {
        Interview interview = interviewRepository.findByIdWithDetails(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId));

        // If result already computed, return it
        var existingResultOpt = interviewResultRepository.findByInterviewId(interviewId);
        if (existingResultOpt.isPresent()) {
            return LiveInterviewResultResponse.fromEntity(existingResultOpt.get());
        }

        interview.setStatus(InterviewStatus.COMPLETED);
        interviewRepository.save(interview);

        List<InterviewQuestion> questions = interviewQuestionRepository.findByInterviewIdOrderByQuestionOrderAsc(interviewId);
        List<InterviewAnswer> answers = interviewAnswerRepository.findByQuestionInterviewIdOrderByAnsweredAtAsc(interviewId);

        long answeredCount = questions.stream().filter(q -> q.getStatus() == QuestionStatus.ANSWERED).count();
        long skippedCount = questions.stream().filter(q -> q.getStatus() == QuestionStatus.SKIPPED).count();

        // Set fixed session duration to 15.0 minutes
        double durationMinutes = 15.0;
        if (!questions.isEmpty() && questions.get(0).getAskedAt() != null) {
            durationMinutes = Math.min(15.0, Math.max(1.0, Duration.between(questions.get(0).getAskedAt(), LocalDateTime.now()).toSeconds() / 60.0));
        }

        Application application = interview.getApplication();
        var synthesized = aiInterviewService.synthesizeFinalResult(
                application.getCandidate(),
                application.getJob(),
                questions,
                answers,
                durationMinutes
        );

        InterviewResult result = new InterviewResult(
                interview,
                synthesized.technicalScore(),
                synthesized.communicationScore(),
                synthesized.problemSolvingScore(),
                synthesized.answerRelevanceScore(),
                synthesized.completenessScore(),
                synthesized.overallScore(),
                synthesized.strengths(),
                synthesized.weaknesses(),
                synthesized.improvementTopics(),
                synthesized.recommendation(),
                synthesized.summary(),
                questions.size(),
                (int) answeredCount,
                (int) skippedCount,
                durationMinutes
        );

        // Aggregate integrity monitoring events
        List<spring.eshwar.entity.InterviewIntegrityEvent> integrityEvents =
                interviewIntegrityEventRepository.findByInterviewIdOrderByCreatedAtAsc(interviewId);
        int multiplePersonCount = 0;
        int tabSwitchCount = 0;
        int attentionAwayCount = 0;
        int audioAnomalyCount = 0;

        for (var ev : integrityEvents) {
            String type = ev.getEventType() != null ? ev.getEventType().toUpperCase() : "";
            if (type.contains("MULTIPLE_PERSON")) {
                multiplePersonCount++;
            } else if (type.contains("TAB_SWITCH") || type.contains("WINDOW_BLUR") || type.contains("FULLSCREEN_EXIT")) {
                tabSwitchCount++;
            } else if (type.contains("ATTENTION_AWAY") || type.contains("LOOKING_AWAY")) {
                attentionAwayCount++;
            } else if (type.contains("AUDIO_ANOMALY")) {
                audioAnomalyCount++;
            }
        }

        int totalIntegrity = integrityEvents.size();
        // Determine integrity status without modifying technical score
        // High severity or multiple significant occurrences flag for HR/Admin review
        boolean requiresReview = multiplePersonCount > 0 || tabSwitchCount >= 3 || attentionAwayCount >= 4 || totalIntegrity >= 5;
        String integrityStatus = requiresReview ? "REVIEW_REQUIRED" : "NORMAL";

        result.setIntegrityStatus(integrityStatus);
        result.setTotalIntegrityEvents(totalIntegrity);
        result.setMultiplePersonEvents(multiplePersonCount);
        result.setTabSwitchEvents(tabSwitchCount);
        result.setAttentionAwayEvents(attentionAwayCount);
        result.setAudioAnomalyEvents(audioAnomalyCount);

        InterviewResult savedResult = interviewResultRepository.save(result);
        log.info("Saved interview result {} for interview {}. Overall Score: {}, Integrity: {} (Total: {})",
                savedResult.getId(), interviewId, savedResult.getOverallScore(), integrityStatus, totalIntegrity);

        return LiveInterviewResultResponse.fromEntity(savedResult);
    }

    /**
     * Gets the scorecard result for an interview.
     */
    @Transactional(readOnly = true)
    public LiveInterviewResultResponse getInterviewResult(Long interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId));

        InterviewResult result = interviewResultRepository.findByInterviewId(interviewId)
                .orElseThrow(() -> new BadRequestException("Interview result has not yet been generated. Interview status is: " + interview.getStatus()));

        return LiveInterviewResultResponse.fromEntity(result);
    }

    /**
     * Gets the scorecard result for an application's latest interview.
     */
    @Transactional(readOnly = true)
    public LiveInterviewResultResponse getInterviewResultByApplication(Long applicationId) {
        Interview interview = interviewRepository.findFirstByApplicationIdOrderByCreatedAtDesc(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview for application", "applicationId", applicationId));

        return getInterviewResult(interview.getId());
    }

    /**
     * Marks an interview as abandoned / pending HR review when candidate exits early.
     */
    @Transactional
    public void exitInterview(Long interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId));

        if (interview.getStatus() == InterviewStatus.IN_PROGRESS || interview.getStatus() == InterviewStatus.READY) {
            interview.setStatus(InterviewStatus.PENDING_HR_REVIEW);
            interview.setNotes((interview.getNotes() != null ? interview.getNotes() + " | " : "")
                    + "Candidate exited before completion on " + LocalDateTime.now() + ". Requires HR review for retake.");
            interviewRepository.save(interview);
            log.info("Interview {} marked as PENDING_HR_REVIEW due to early candidate exit.", interviewId);
        }
    }

    /**
     * Admin/HR endpoint to approve retake of an abandoned/incomplete assessment.
     */
    @Transactional
    public Interview approveRetake(Long interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId));

        interview.setStatus(InterviewStatus.RETAKE_APPROVED);
        interview.setNotes((interview.getNotes() != null ? interview.getNotes() + " | " : "")
                + "HR approved candidate retake on " + LocalDateTime.now());
        Interview saved = interviewRepository.save(interview);
        log.info("Interview {} retake approved by HR. Status set to RETAKE_APPROVED.", interviewId);
        return saved;
    }

    /**
     * Batch records integrity events sent from the candidate's browser during live interview.
     */
    @Transactional
    public List<spring.eshwar.entity.InterviewIntegrityEvent> recordIntegrityEvents(
            Long interviewId, spring.eshwar.dto.interview.RecordIntegrityEventsRequest request) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId));

        if (request == null || request.getEvents() == null || request.getEvents().isEmpty()) {
            return List.of();
        }

        List<spring.eshwar.entity.InterviewIntegrityEvent> entities = new java.util.ArrayList<>();
        for (var item : request.getEvents()) {
            spring.eshwar.entity.InterviewIntegrityEvent ev = new spring.eshwar.entity.InterviewIntegrityEvent(
                    interview,
                    item.getEventType() != null ? item.getEventType() : "UNKNOWN",
                    item.getSeverity() != null ? item.getSeverity() : "LOW",
                    item.getStartTime(),
                    item.getEndTime(),
                    item.getDurationSeconds(),
                    item.getMessage()
            );
            entities.add(ev);
        }

        List<spring.eshwar.entity.InterviewIntegrityEvent> saved = interviewIntegrityEventRepository.saveAll(entities);
        log.info("Saved {} integrity events for interview {}", saved.size(), interviewId);
        return saved;
    }

    /**
     * Retrieves all recorded integrity events for HR/Admin review.
     */
    @Transactional(readOnly = true)
    public List<spring.eshwar.entity.InterviewIntegrityEvent> getIntegrityEvents(Long interviewId) {
        return interviewIntegrityEventRepository.findByInterviewIdOrderByCreatedAtAsc(interviewId);
    }

    private InterviewQuestion generateAndSaveNextQuestion(Interview interview, Application application, int nextOrder) {
        List<InterviewQuestion> pastQuestions = interviewQuestionRepository
                .findByInterviewIdOrderByQuestionOrderAsc(interview.getId());
        List<InterviewAnswer> pastAnswers = interviewAnswerRepository
                .findByQuestionInterviewIdOrderByAnsweredAtAsc(interview.getId());

        var genQ = aiInterviewService.generateNextQuestion(
                application.getCandidate(),
                application.getResume(),
                application.getJob(),
                pastQuestions,
                pastAnswers,
                nextOrder
        );

        InterviewQuestion newQuestion = new InterviewQuestion(
                interview,
                genQ.questionText(),
                genQ.category(),
                genQ.difficulty(),
                nextOrder,
                genQ.expectedConcepts(),
                genQ.keyPoints()
        );

        return interviewQuestionRepository.save(newQuestion);
    }

    /**
     * Calculates the dynamic target question count (between 6 and 10) based on
     * the candidate's skills and the job's requirements.
     */
    public int calculateDynamicTargetQuestions(Application application) {
        if (application == null) {
            return 6;
        }
        var profile = StructuredCandidateProfileDto.fromCandidateAndResume(
                application.getCandidate(),
                application.getResume()
        );
        return profile.calculateDynamicTargetQuestions(application.getJob());
    }

    /**
     * Evaluates whether the interview should conclude based on:
     * - Hard limits: strictly never exceed 10 questions; never finish before 6 questions.
     * - Dynamic target questions: if reached or exceeded.
     * - Adaptive early completion: after at least 6 questions, if skill coverage is sufficient
     *   and candidate performance is conclusive.
     */
    public boolean shouldConcludeInterview(Interview interview, long totalAsked) {
        if (totalAsked >= 10) {
            log.info("Interview {} reached absolute maximum limit of 10 questions. Concluding interview.", interview.getId());
            return true;
        }
        if (totalAsked < 6) {
            return false;
        }

        Application application = interview.getApplication();
        int dynamicTarget = calculateDynamicTargetQuestions(application);
        if (totalAsked >= dynamicTarget) {
            log.info("Interview {} reached dynamic target ({} questions). Concluding interview.", interview.getId(), dynamicTarget);
            return true;
        }

        // Check early completion criteria after at least 6 questions:
        // Evaluate skill coverage, answer quality, correctness, and consistency
        List<InterviewAnswer> pastAnswers = interviewAnswerRepository.findByQuestionInterviewIdOrderByAnsweredAtAsc(interview.getId());
        if (pastAnswers != null && pastAnswers.size() >= 6) {
            double totalScore = 0.0;
            int scoredCount = 0;
            int highCount = 0;
            int lowCount = 0;

            for (InterviewAnswer a : pastAnswers) {
                if (a.getTechnicalScore() != null) {
                    double score = a.getTechnicalScore();
                    totalScore += score;
                    scoredCount++;
                    if (score >= 85.0) highCount++;
                    if (score < 40.0) lowCount++;
                }
            }

            // Check remaining session time: if nearing the 15-minute limit (>= 14.2 minutes), conclude gracefully
            List<InterviewQuestion> questions = interviewQuestionRepository.findByInterviewIdOrderByQuestionOrderAsc(interview.getId());
            if (!questions.isEmpty() && questions.get(0).getAskedAt() != null) {
                long elapsedSeconds = Duration.between(questions.get(0).getAskedAt(), LocalDateTime.now()).toSeconds();
                if (elapsedSeconds >= 850) { // >= 14 minutes 10 seconds
                    log.info("Interview {} has reached elapsed session time of {}s (approaching 15m limit). Concluding early gracefully after {} questions.",
                            interview.getId(), elapsedSeconds, totalAsked);
                    return true;
                }
            }

            if (scoredCount >= 6) {
                double avg = totalScore / scoredCount;
                // If candidate is exceptionally strong across all 6 core categories (avg >= 85.0 and at least 5 strong answers)
                // or decisively struggling across fundamentals (avg < 40.0 and at least 5 weak answers),
                // early completion is conclusive.
                if ((avg >= 85.0 && highCount >= 5) || (avg < 40.0 && lowCount >= 5)) {
                    log.info("Interview {} has conclusive performance (avg: {}, highCount: {}, lowCount: {}) after {} questions. Concluding early.",
                            interview.getId(), avg, highCount, lowCount, totalAsked);
                    return true;
                }
            }
        }

        return false;
    }
}
