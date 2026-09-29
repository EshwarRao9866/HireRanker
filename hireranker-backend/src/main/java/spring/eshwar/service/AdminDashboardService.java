package spring.eshwar.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.eshwar.dto.dashboard.AdminDashboardResponse;
import spring.eshwar.dto.dashboard.ApplicantsOverviewDTO;
import spring.eshwar.dto.dashboard.SkillStatDTO;
import spring.eshwar.dto.dashboard.StatusBreakdownDTO;
import spring.eshwar.dto.dashboard.TopCandidateDTO;
import spring.eshwar.entity.Application;
import spring.eshwar.entity.ApplicationStatus;
import spring.eshwar.entity.Candidate;
import spring.eshwar.entity.Job;
import spring.eshwar.entity.ScreeningResult;
import spring.eshwar.repository.ApplicationRepository;
import spring.eshwar.repository.CandidateRepository;
import spring.eshwar.repository.InterviewRepository;
import spring.eshwar.repository.JobRepository;
import spring.eshwar.repository.ResumeRepository;
import spring.eshwar.repository.ScreeningResultRepository;
import spring.eshwar.repository.UserRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminDashboardService {

    private static final Logger logger = LoggerFactory.getLogger(AdminDashboardService.class);

    private final CandidateRepository candidateRepository;
    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;
    private final ScreeningResultRepository screeningResultRepository;
    private final InterviewRepository interviewRepository;
    private final UserRepository userRepository;

    public AdminDashboardService(CandidateRepository candidateRepository,
                                 ApplicationRepository applicationRepository,
                                 JobRepository jobRepository,
                                 ResumeRepository resumeRepository,
                                 ScreeningResultRepository screeningResultRepository,
                                 InterviewRepository interviewRepository,
                                 UserRepository userRepository) {
        this.candidateRepository = candidateRepository;
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.resumeRepository = resumeRepository;
        this.screeningResultRepository = screeningResultRepository;
        this.interviewRepository = interviewRepository;
        this.userRepository = userRepository;
    }

    /**
     * Aggregates real-time statistics and chart metrics for the admin dashboard.
     * Handles empty database states gracefully without division-by-zero or NPEs.
     */
    /**
     * Aggregates real-time statistics and chart metrics for the admin dashboard.
     * Handles empty database states gracefully without division-by-zero or NPEs.
     */
    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboardData() {
        return getDashboardData("This Week");
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboardData(String period) {
        // Scalar KPI counts via database aggregation queries
        long totalCandidates = candidateRepository.count();
        long totalApplications = applicationRepository.count();
        long totalJobs = jobRepository.count();
        long screenedResumes = screeningResultRepository.count();
        long shortlistedCandidates = applicationRepository.countByStatus(ApplicationStatus.SHORTLISTED)
                + applicationRepository.countByStatus(ApplicationStatus.INTERVIEW);
        long totalInterviews = interviewRepository.count();
        long totalUsers = userRepository.count();

        Double avgScore = screeningResultRepository.getAverageOverallScore();
        double averageMatchScore = (avgScore != null && !avgScore.isNaN())
                ? Math.round(avgScore * 10.0) / 10.0
                : 0.0;

        // Chart 1: Applicants Overview
        ApplicantsOverviewDTO applicantsOverview = buildApplicantsOverview(
                totalApplications, screenedResumes, shortlistedCandidates, totalInterviews, period);

        // Chart 2: Application Pipeline Status Breakdown
        List<StatusBreakdownDTO> applicationStatus = buildStatusBreakdown(totalApplications);

        // Chart 3: Top Skills across jobs, candidates, and screening matches
        List<SkillStatDTO> topSkills = buildTopSkills();

        // Chart 4: Top Ranked Candidates
        List<TopCandidateDTO> topRankedCandidates = buildTopRankedCandidates();

        logger.info("Admin dashboard generated: {} candidates, {} applications, {} jobs, {} interviews, avgScore={}, period={}",
                totalCandidates, totalApplications, totalJobs, totalInterviews, averageMatchScore, period);

        return new AdminDashboardResponse(
                totalCandidates,
                totalApplications,
                totalJobs,
                screenedResumes,
                shortlistedCandidates,
                averageMatchScore,
                totalInterviews,
                totalUsers,
                applicantsOverview,
                applicationStatus,
                topSkills,
                topRankedCandidates
        );
    }

    private ApplicantsOverviewDTO buildApplicantsOverview(long totalApplications,
                                                         long screenedResumes,
                                                         long shortlisted,
                                                         long totalInterviews,
                                                         String period) {
        List<String> labels = new ArrayList<>();
        List<Long> appSeries = new ArrayList<>();
        List<Long> screenSeries = new ArrayList<>();

        LocalDate today = LocalDate.now();

        if ("This Month".equalsIgnoreCase(period)) {
            // Last 30 days grouped in 5 6-day buckets or 4 weeks
            for (int w = 3; w >= 0; w--) {
                LocalDate start = today.minusDays((w + 1) * 7L - 1);
                LocalDate end = today.minusDays(w * 7L);
                labels.add("Wk " + (4 - w));
                try {
                    List<LocalDateTime> appDates = applicationRepository.findAppliedAtSince(start.atStartOfDay());
                    long appCount = appDates.stream().filter(dt -> dt != null && !dt.toLocalDate().isAfter(end)).count();
                    appSeries.add(appCount);

                    List<LocalDateTime> screenDates = screeningResultRepository.findScreenedAtSince(start.atStartOfDay());
                    long scrCount = screenDates.stream().filter(dt -> dt != null && !dt.toLocalDate().isAfter(end)).count();
                    screenSeries.add(scrCount);
                } catch (Exception e) {
                    appSeries.add(0L);
                    screenSeries.add(0L);
                }
            }
        } else if ("This Year".equalsIgnoreCase(period) || "All Time".equalsIgnoreCase(period)) {
            // Last 6 or 12 months
            for (int m = 5; m >= 0; m--) {
                LocalDate monthDate = today.minusMonths(m);
                String monthLabel = monthDate.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                labels.add(monthLabel);

                LocalDate startOfMonth = monthDate.withDayOfMonth(1);
                LocalDate endOfMonth = monthDate.withDayOfMonth(monthDate.lengthOfMonth());
                try {
                    List<LocalDateTime> appDates = applicationRepository.findAppliedAtSince(startOfMonth.atStartOfDay());
                    long appCount = appDates.stream().filter(dt -> dt != null && !dt.toLocalDate().isAfter(endOfMonth)).count();
                    appSeries.add(appCount);

                    List<LocalDateTime> screenDates = screeningResultRepository.findScreenedAtSince(startOfMonth.atStartOfDay());
                    long scrCount = screenDates.stream().filter(dt -> dt != null && !dt.toLocalDate().isAfter(endOfMonth)).count();
                    screenSeries.add(scrCount);
                } catch (Exception e) {
                    appSeries.add(0L);
                    screenSeries.add(0L);
                }
            }
        } else {
            // Default: "This Week" (Last 7 days chronological)
            LocalDateTime sevenDaysAgo = today.minusDays(6).atStartOfDay();
            Map<LocalDate, Long> appByDate = new HashMap<>();
            Map<LocalDate, Long> screenByDate = new HashMap<>();

            try {
                List<LocalDateTime> appDates = applicationRepository.findAppliedAtSince(sevenDaysAgo);
                for (LocalDateTime dt : appDates) {
                    if (dt != null) {
                        LocalDate d = dt.toLocalDate();
                        appByDate.put(d, appByDate.getOrDefault(d, 0L) + 1L);
                    }
                }

                List<LocalDateTime> screenDates = screeningResultRepository.findScreenedAtSince(sevenDaysAgo);
                for (LocalDateTime dt : screenDates) {
                    if (dt != null) {
                        LocalDate d = dt.toLocalDate();
                        screenByDate.put(d, screenByDate.getOrDefault(d, 0L) + 1L);
                    }
                }
            } catch (Exception e) {
                logger.warn("Error computing timeline trends: {}", e.getMessage());
            }

            for (int i = 6; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                String dayLabel = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                labels.add(dayLabel);
                appSeries.add(appByDate.getOrDefault(date, 0L));
                screenSeries.add(screenByDate.getOrDefault(date, 0L));
            }
        }

        return new ApplicantsOverviewDTO(
                totalApplications,
                screenedResumes,
                shortlisted,
                totalInterviews,
                labels,
                appSeries,
                screenSeries
        );
    }

    private List<StatusBreakdownDTO> buildStatusBreakdown(long totalApplications) {
        List<StatusBreakdownDTO> breakdown = new ArrayList<>();

        Map<ApplicationStatus, StatusConfig> configs = new LinkedHashMap<>();
        configs.put(ApplicationStatus.APPLIED, new StatusConfig("New / Applied", "new"));
        configs.put(ApplicationStatus.SCREENING, new StatusConfig("Screened", "screened"));
        configs.put(ApplicationStatus.SHORTLISTED, new StatusConfig("Shortlisted", "shortlisted"));
        configs.put(ApplicationStatus.INTERVIEW, new StatusConfig("Interview Scheduled", "interview"));
        configs.put(ApplicationStatus.REJECTED, new StatusConfig("Rejected", "rejected"));
        configs.put(ApplicationStatus.HIRED, new StatusConfig("Hired", "hired"));

        Map<ApplicationStatus, Long> countsMap = new HashMap<>();
        try {
            List<Object[]> grouped = applicationRepository.countGroupedByStatus();
            for (Object[] row : grouped) {
                if (row.length >= 2 && row[0] != null && row[1] instanceof Number num) {
                    ApplicationStatus status = null;
                    if (row[0] instanceof ApplicationStatus appStatus) {
                        status = appStatus;
                    } else {
                        try {
                            status = ApplicationStatus.valueOf(row[0].toString().trim().toUpperCase());
                        } catch (Exception ignored) {}
                    }
                    if (status != null) {
                        countsMap.put(status, num.longValue());
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Error grouping applications by status: {}", e.getMessage());
        }

        for (Map.Entry<ApplicationStatus, StatusConfig> entry : configs.entrySet()) {
            ApplicationStatus status = entry.getKey();
            StatusConfig cfg = entry.getValue();

            long count = countsMap.getOrDefault(status, 0L);
            double percent = (totalApplications > 0)
                    ? Math.round((count * 100.0 / totalApplications) * 10.0) / 10.0
                    : 0.0;

            breakdown.add(new StatusBreakdownDTO(
                    status.name(),
                    cfg.label,
                    count,
                    percent,
                    cfg.cssClass
            ));
        }

        return breakdown;
    }

    private List<SkillStatDTO> buildTopSkills() {
        Map<String, Long> skillCounts = new HashMap<>();

        // 1. Aggregate skills from applications & candidates
        try {
            List<Application> apps = applicationRepository.findAll();
            for (Application app : apps) {
                if (app.getCandidate() != null && app.getCandidate().getSkills() != null) {
                    extractAndTallySkills(app.getCandidate().getSkills(), skillCounts);
                }
            }
        } catch (Exception ignored) {
        }

        // 2. From Candidate profiles in database
        try {
            List<Candidate> candidates = candidateRepository.findAll();
            for (Candidate cand : candidates) {
                if (cand.getSkills() != null && !cand.getSkills().isBlank()) {
                    extractAndTallySkills(cand.getSkills(), skillCounts);
                }
            }
        } catch (Exception ignored) {
        }

        // 3. From Screening matching skills
        try {
            List<String> matchingSkills = screeningResultRepository.findAllMatchingSkills();
            for (String skills : matchingSkills) {
                extractAndTallySkills(skills, skillCounts);
            }
        } catch (Exception ignored) {
        }

        // 4. From Active Job requirements
        try {
            List<Job> jobs = jobRepository.findAll();
            for (Job job : jobs) {
                if (job.getRequiredSkills() != null && !job.getRequiredSkills().isBlank()) {
                    extractAndTallySkills(job.getRequiredSkills(), skillCounts);
                }
            }
        } catch (Exception ignored) {
        }

        if (skillCounts.isEmpty()) {
            return new ArrayList<>();
        }

        long totalOccurrences = skillCounts.values().stream().mapToLong(Long::longValue).sum();

        // Sort descending by frequency and pick top 6
        List<Map.Entry<String, Long>> sorted = skillCounts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(6)
                .toList();

        List<String> classes = Arrays.asList("java", "spring", "sql", "js", "react", "other");
        List<SkillStatDTO> result = new ArrayList<>();

        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<String, Long> entry = sorted.get(i);
            double percent = (totalOccurrences > 0)
                    ? Math.round((entry.getValue() * 100.0 / totalOccurrences) * 10.0) / 10.0
                    : 0.0;
            String css = i < classes.size() ? classes.get(i) : "other";
            result.add(new SkillStatDTO(entry.getKey(), entry.getValue(), percent, css));
        }

        return result;
    }

    private void extractAndTallySkills(String skillsText, Map<String, Long> counts) {
        if (skillsText == null || skillsText.isBlank()) {
            return;
        }
        // Split by comma, semicolon, slash, or newline
        String[] tokens = skillsText.split("[,;\\n/|]+");
        for (String token : tokens) {
            String skill = token.trim();
            if (!skill.isEmpty() && skill.length() < 50) {
                String normalized = normalizeSkillName(skill);
                counts.put(normalized, counts.getOrDefault(normalized, 0L) + 1L);
            }
        }
    }

    private String normalizeSkillName(String raw) {
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.contains("java") && !lower.contains("javascript")) {
            return "Java";
        }
        if (lower.contains("spring")) {
            return "Spring Boot";
        }
        if (lower.contains("sql") || lower.contains("postgres") || lower.contains("mysql")) {
            return "SQL";
        }
        if (lower.contains("javascript") || lower.contains("typescript") || lower.contains("js") || lower.contains("ts")) {
            return "JavaScript / TypeScript";
        }
        if (lower.contains("angular") || lower.contains("react")) {
            return "Angular / React";
        }
        if (lower.contains("docker") || lower.contains("kubernetes")) {
            return "Docker / K8s";
        }
        if (lower.contains("python")) {
            return "Python";
        }
        if (lower.contains("aws") || lower.contains("cloud")) {
            return "Cloud / AWS";
        }
        // Capitalize first character
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1).trim();
    }

    private List<TopCandidateDTO> buildTopRankedCandidates() {
        List<TopCandidateDTO> list = new ArrayList<>();
        try {
            List<ScreeningResult> results = screeningResultRepository.findTop5ByOrderByOverallScoreDesc();
            int rank = 1;
            for (ScreeningResult sr : results) {
                Application app = sr.getApplication();
                Long candId = (app != null && app.getCandidate() != null) ? app.getCandidate().getId() : null;
                String candName = (app != null && app.getCandidate() != null) ? app.getCandidate().getFullName() : "Candidate";
                String email = (app != null && app.getCandidate() != null && app.getCandidate().getUser() != null)
                        ? app.getCandidate().getUser().getEmail()
                        : "";
                String role = (app != null && app.getJob() != null) ? app.getJob().getTitle() : "Software Engineer";
                String status = (app != null && app.getStatus() != null) ? app.getStatus().name() : "SCREENING";
                String resumeFile = (app != null && app.getResume() != null) ? app.getResume().getFileName() : "Resume.pdf";
                Long resumeId = (app != null && app.getResume() != null) ? app.getResume().getId() : null;
                Long appId = (app != null) ? app.getId() : null;

                list.add(new TopCandidateDTO(
                        rank++,
                        candId,
                        candName,
                        email,
                        role,
                        sr.getOverallScore(),
                        sr.getSkillsScore(),
                        sr.getExperienceScore(),
                        sr.getEducationScore(),
                        status,
                        resumeFile,
                        resumeId,
                        appId
                ));
            }
        } catch (Exception e) {
            logger.warn("Error assembling top ranked candidates: {}", e.getMessage());
        }
        return list;
    }

    private static class StatusConfig {
        String label;
        String cssClass;

        StatusConfig(String label, String cssClass) {
            this.label = label;
            this.cssClass = cssClass;
        }
    }
}
