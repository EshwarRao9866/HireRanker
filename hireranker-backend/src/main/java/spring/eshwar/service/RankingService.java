package spring.eshwar.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.eshwar.dto.ranking.RankingResponse;
import spring.eshwar.entity.Application;
import spring.eshwar.entity.Candidate;
import spring.eshwar.entity.Job;
import spring.eshwar.entity.ScreeningResult;
import spring.eshwar.exception.ResourceNotFoundException;
import spring.eshwar.repository.ApplicationRepository;
import spring.eshwar.repository.JobRepository;
import spring.eshwar.repository.ScreeningResultRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class RankingService {

    private static final Logger logger = LoggerFactory.getLogger(RankingService.class);

    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final ScreeningResultRepository screeningResultRepository;

    public RankingService(JobRepository jobRepository,
                          ApplicationRepository applicationRepository,
                          ScreeningResultRepository screeningResultRepository) {
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.screeningResultRepository = screeningResultRepository;
    }

    public List<RankingResponse> getAllRankings() {
        List<Job> jobs = jobRepository.findAll();
        if (jobs.isEmpty()) {
            return Collections.emptyList();
        }
        List<RankingResponse> all = new ArrayList<>();
        for (Job j : jobs) {
            all.addAll(getRankingForJob(j.getId()));
        }
        all.sort((a, b) -> {
            if (a.getOverallScore() == null && b.getOverallScore() == null) return 0;
            if (a.getOverallScore() == null) return 1;
            if (b.getOverallScore() == null) return -1;
            return Double.compare(b.getOverallScore(), a.getOverallScore());
        });
        int rank = 1;
        for (RankingResponse r : all) {
            r.setRank(rank++);
        }
        return all;
    }

    /**
     * Computes candidate ranking for a given job based on existing ScreeningResult records.
     * Process:
     * 1. Find job.
     * 2. Find applications.
     * 3. Find screening results.
     * 4. Get scores.
     * 5. Sort descending by overallScore (with consistent tie-breaking for equal scores).
     * 6. Assign rank (highest score = rank 1).
     * 7. Return ranking response.
     *
     * Zero AI calls are made.
     *
     * @param jobId the ID of the job to rank
     * @return ordered list of RankingResponse DTOs
     */
    public List<RankingResponse> getRankingForJob(Long jobId) {
        // 1. Find job
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));

        // 2. Find applications
        List<Application> applications = applicationRepository.findByJobId(job.getId());
        if (applications == null || applications.isEmpty()) {
            logger.info("No applications found for job id: {}. Returning empty ranking list.", jobId);
            return Collections.emptyList();
        }

        logger.info("Computing ranking for job id: {} ('{}') across {} applications.",
                jobId, job.getTitle(), applications.size());

        List<RankingResponse> rankingList = new ArrayList<>(applications.size());

        // 3. Find screening results & 4. Get scores
        for (Application application : applications) {
            Candidate candidate = application.getCandidate();
            Long candidateId = candidate != null ? candidate.getId() : null;
            String candidateName = candidate != null ? candidate.getFullName() : null;
            Long applicationId = application.getId();
            String applicationStatus = application.getStatus() != null ? application.getStatus().name() : null;

            Optional<ScreeningResult> optScreening = screeningResultRepository.findByApplicationId(applicationId);

            Double overallScore = null;
            Double skillsScore = null;
            Double experienceScore = null;
            Double educationScore = null;

            if (optScreening.isPresent()) {
                ScreeningResult sr = optScreening.get();
                overallScore = sr.getOverallScore();
                skillsScore = sr.getSkillsScore();
                experienceScore = sr.getExperienceScore();
                educationScore = sr.getEducationScore();
            }

            rankingList.add(new RankingResponse(
                    null,
                    candidateId,
                    candidateName,
                    applicationId,
                    overallScore,
                    skillsScore,
                    experienceScore,
                    educationScore,
                    applicationStatus
            ));
        }

        // 5. Sort candidates descending by overallScore.
        // - Candidates without screening results (overallScore == null) are placed last.
        // - Equal scores are handled consistently through secondary criteria:
        //   skillsScore desc -> experienceScore desc -> educationScore desc -> applicationId asc.
        rankingList.sort((a, b) -> {
            if (a.getOverallScore() == null && b.getOverallScore() == null) {
                return compareApplicationIds(a, b);
            }
            if (a.getOverallScore() == null) {
                return 1;
            }
            if (b.getOverallScore() == null) {
                return -1;
            }

            int overallComparison = Double.compare(b.getOverallScore(), a.getOverallScore());
            if (overallComparison != 0) {
                return overallComparison;
            }

            // Equal overall scores tie-breaker 1: skillsScore descending
            int skillsComparison = compareScoresDesc(a.getSkillsScore(), b.getSkillsScore());
            if (skillsComparison != 0) {
                return skillsComparison;
            }

            // Equal scores tie-breaker 2: experienceScore descending
            int expComparison = compareScoresDesc(a.getExperienceScore(), b.getExperienceScore());
            if (expComparison != 0) {
                return expComparison;
            }

            // Equal scores tie-breaker 3: educationScore descending
            int eduComparison = compareScoresDesc(a.getEducationScore(), b.getEducationScore());
            if (eduComparison != 0) {
                return eduComparison;
            }

            // Final deterministic tie-breaker: applicationId ascending
            return compareApplicationIds(a, b);
        });

        // 6. Assign rank (highest score = rank 1)
        int currentRank = 1;
        for (RankingResponse item : rankingList) {
            item.setRank(currentRank++);
        }

        // 7. Return ranking response
        logger.info("Successfully ranked {} candidates for job id: {}", rankingList.size(), jobId);
        return rankingList;
    }

    private int compareScoresDesc(Double s1, Double s2) {
        if (s1 == null && s2 == null) {
            return 0;
        }
        if (s1 == null) {
            return 1;
        }
        if (s2 == null) {
            return -1;
        }
        return Double.compare(s2, s1);
    }

    private int compareApplicationIds(RankingResponse a, RankingResponse b) {
        if (a.getApplicationId() == null && b.getApplicationId() == null) {
            return 0;
        }
        if (a.getApplicationId() == null) {
            return 1;
        }
        if (b.getApplicationId() == null) {
            return -1;
        }
        return a.getApplicationId().compareTo(b.getApplicationId());
    }
}
