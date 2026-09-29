package spring.eshwar.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.eshwar.dto.candidate.CandidateDashboardResponse;
import spring.eshwar.dto.candidate.CandidateRecentApplicationDTO;
import spring.eshwar.dto.candidate.CandidateResponse;
import spring.eshwar.dto.candidate.RecommendedJobDTO;
import spring.eshwar.dto.interview.InterviewResponse;
import spring.eshwar.entity.Application;
import spring.eshwar.entity.ApplicationStatus;
import spring.eshwar.entity.Candidate;
import spring.eshwar.entity.Interview;
import spring.eshwar.entity.InterviewStatus;
import spring.eshwar.entity.Job;
import spring.eshwar.entity.JobStatus;
import spring.eshwar.entity.Resume;
import spring.eshwar.entity.ScreeningResult;
import spring.eshwar.exception.ResourceNotFoundException;
import spring.eshwar.repository.ApplicationRepository;
import spring.eshwar.repository.CandidateRepository;
import spring.eshwar.repository.InterviewRepository;
import spring.eshwar.repository.JobRepository;
import spring.eshwar.repository.ResumeRepository;
import spring.eshwar.repository.ScreeningResultRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CandidateDashboardService {

    private static final Logger logger = LoggerFactory.getLogger(CandidateDashboardService.class);

    private final CandidateRepository candidateRepository;
    private final ApplicationRepository applicationRepository;
    private final ResumeRepository resumeRepository;
    private final InterviewRepository interviewRepository;
    private final ScreeningResultRepository screeningResultRepository;
    private final JobRepository jobRepository;
    private final spring.eshwar.service.skill.SkillNormalizationService skillNormalizationService;

    public CandidateDashboardService(CandidateRepository candidateRepository,
                                     ApplicationRepository applicationRepository,
                                     ResumeRepository resumeRepository,
                                     InterviewRepository interviewRepository,
                                     ScreeningResultRepository screeningResultRepository,
                                     JobRepository jobRepository,
                                     spring.eshwar.service.skill.SkillNormalizationService skillNormalizationService) {
        this.candidateRepository = candidateRepository;
        this.applicationRepository = applicationRepository;
        this.resumeRepository = resumeRepository;
        this.interviewRepository = interviewRepository;
        this.screeningResultRepository = screeningResultRepository;
        this.jobRepository = jobRepository;
        this.skillNormalizationService = skillNormalizationService;
    }

    /**
     * Retrieves the complete dashboard data for the authenticated candidate.
     * The candidate identity is strictly resolved from the authenticated JWT user email.
     */
    @Transactional(readOnly = true)
    public CandidateDashboardResponse getCandidateDashboard(String email) {
        Candidate candidate = candidateRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile not found for email: " + email));

        Long candidateId = candidate.getId();
        CandidateResponse profile = CandidateResponse.fromEntity(candidate);

        // Core metric counts
        long totalApplications = applicationRepository.countByCandidateId(candidateId);
        long totalResumes = resumeRepository.countByCandidateId(candidateId);
        long shortlistedApplications = applicationRepository.countByCandidateIdAndStatus(
                candidateId, ApplicationStatus.SHORTLISTED)
                + applicationRepository.countByCandidateIdAndStatus(candidateId, ApplicationStatus.INTERVIEW);

        List<Application> candidateApps = applicationRepository.findByCandidateIdOrderByAppliedAtDesc(candidateId);

        // Screened count: applications with a screening result or advanced status
        long screenedApplications = 0L;
        for (Application app : candidateApps) {
            if (screeningResultRepository.existsByApplicationId(app.getId())
                    || app.getStatus() == ApplicationStatus.SCREENING
                    || app.getStatus() == ApplicationStatus.SHORTLISTED
                    || app.getStatus() == ApplicationStatus.INTERVIEW
                    || app.getStatus() == ApplicationStatus.HIRED) {
                screenedApplications++;
            }
        }

        // Upcoming interviews (future scheduled/rescheduled dates)
        List<InterviewResponse> upcomingInterviews = buildUpcomingInterviews(candidateId);

        // Recent applications (top 5 with job info and match scores)
        List<CandidateRecentApplicationDTO> recentApplications = buildRecentApplications(candidateApps);

        // Application status breakdown map
        Map<String, Long> applicationStatuses = buildApplicationStatuses(candidateId);

        // AI recommended active jobs
        List<RecommendedJobDTO> recommendedJobs = buildRecommendedJobs(candidate);

        logger.info("Candidate dashboard generated for candidate id: {}, apps: {}, resumes: {}, interviews: {}",
                candidateId, totalApplications, totalResumes, upcomingInterviews.size());

        // Active resume information
        String activeResumeFileName = null;
        Double activeResumeScore = null;
        List<Resume> candidateResumes = resumeRepository.findAllByCandidateId(candidateId);
        if (!candidateResumes.isEmpty()) {
            Resume activeResume = candidateResumes.get(candidateResumes.size() - 1);
            activeResumeFileName = activeResume.getFileName();
            // Check for overall score from candidate's latest screening result
            if (!candidateApps.isEmpty()) {
                Optional<ScreeningResult> srOpt = screeningResultRepository.findByApplicationId(candidateApps.get(0).getId());
                if (srOpt.isPresent()) {
                    activeResumeScore = srOpt.get().getOverallScore();
                }
            }
        }

        CandidateDashboardResponse resp = new CandidateDashboardResponse(
                profile,
                totalApplications,
                totalResumes,
                screenedApplications,
                shortlistedApplications,
                upcomingInterviews,
                recentApplications,
                applicationStatuses,
                recommendedJobs
        );
        resp.setActiveResumeFileName(activeResumeFileName);
        resp.setActiveResumeScore(activeResumeScore);
        return resp;
    }

    private List<InterviewResponse> buildUpcomingInterviews(Long candidateId) {
        List<Interview> candidateInterviews = interviewRepository
                .findByApplicationCandidateIdOrderByScheduledDateTimeAsc(candidateId);

        LocalDateTime threshold = LocalDateTime.now().minusHours(2);
        return candidateInterviews.stream()
                .filter(i -> i.getStatus() != InterviewStatus.CANCELLED
                        && i.getStatus() != InterviewStatus.COMPLETED
                        && (i.getScheduledDateTime() == null || i.getScheduledDateTime().isAfter(threshold)))
                .map(InterviewResponse::fromEntity)
                .toList();
    }

    private List<CandidateRecentApplicationDTO> buildRecentApplications(List<Application> candidateApps) {
        List<CandidateRecentApplicationDTO> list = new ArrayList<>();
        int count = 0;
        for (Application app : candidateApps) {
            if (count++ >= 5) {
                break;
            }
            Double matchScore = null;
            Optional<ScreeningResult> srOpt = screeningResultRepository.findByApplicationId(app.getId());
            if (srOpt.isPresent()) {
                matchScore = srOpt.get().getOverallScore();
            }

            Long jobId = app.getJob() != null ? app.getJob().getId() : null;
            String jobTitle = app.getJob() != null ? app.getJob().getTitle() : "Unknown Role";
            String company = app.getJob() != null ? app.getJob().getCompany() : "Unknown Company";
            String location = app.getJob() != null ? app.getJob().getLocation() : "Remote";
            String status = app.getStatus() != null ? app.getStatus().name() : "APPLIED";
            String resumeFileName = app.getResume() != null ? app.getResume().getFileName() : null;

            list.add(new CandidateRecentApplicationDTO(
                    app.getId(),
                    jobId,
                    jobTitle,
                    company,
                    location,
                    status,
                    matchScore,
                    resumeFileName,
                    app.getAppliedAt()
            ));
        }
        return list;
    }

    private Map<String, Long> buildApplicationStatuses(Long candidateId) {
        Map<String, Long> statuses = new LinkedHashMap<>();
        for (ApplicationStatus status : ApplicationStatus.values()) {
            long count = applicationRepository.countByCandidateIdAndStatus(candidateId, status);
            statuses.put(status.name(), count);
        }
        return statuses;
    }

    private List<RecommendedJobDTO> buildRecommendedJobs(Candidate candidate) {
        List<Job> activeJobs = jobRepository.findByStatus(JobStatus.ACTIVE);
        if (activeJobs.isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> candidateSkillTokens = extractSkillTokens(candidate.getSkills());

        List<RecommendedJobDTO> list = new ArrayList<>();
        for (Job job : activeJobs) {
            double matchScore = calculateJobSkillMatch(candidateSkillTokens, job.getRequiredSkills());
            list.add(new RecommendedJobDTO(
                    job.getId(),
                    job.getTitle(),
                    job.getCompany(),
                    job.getLocation(),
                    job.getSalaryRange(),
                    job.getEmploymentType(),
                    job.getRequiredSkills(),
                    matchScore
            ));
        }

        // Sort descending by matchScore and take top 4
        return list.stream()
                .sorted((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()))
                .limit(4)
                .toList();
    }

    private Set<String> extractSkillTokens(String skills) {
        if (skills == null || skills.isBlank()) {
            return Set.of();
        }
        return new LinkedHashSet<>(skillNormalizationService.decomposeAndNormalizeSkills(skills));
    }

    private double calculateJobSkillMatch(Set<String> candidateSkills, String requiredSkillsText) {
        if (requiredSkillsText == null || requiredSkillsText.isBlank()) {
            return 80.0;
        }
        List<String> requiredTokens = skillNormalizationService.decomposeAndNormalizeSkills(requiredSkillsText);
        if (requiredTokens.isEmpty() || candidateSkills.isEmpty()) {
            return 70.0;
        }

        spring.eshwar.service.skill.SkillNormalizationService.SkillMatchResult result =
                skillNormalizationService.matchSkills(candidateSkills, requiredTokens, "");
        double ratio = result.matchRatio();
        double score = 60.0 + (ratio * 38.0); // scales between 60% and 98%
        return Math.round(score * 10.0) / 10.0;
    }
}
