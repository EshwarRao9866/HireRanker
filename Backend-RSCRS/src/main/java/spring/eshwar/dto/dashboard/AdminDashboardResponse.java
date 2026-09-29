package spring.eshwar.dto.dashboard;

import java.util.ArrayList;
import java.util.List;

public class AdminDashboardResponse {

    private long totalCandidates;
    private long totalApplications;
    private long totalJobs;
    private long screenedResumes;
    private long shortlistedCandidates;
    private double averageMatchScore;
    private long totalInterviews;
    private long totalUsers;

    private ApplicantsOverviewDTO applicantsOverview;
    private List<StatusBreakdownDTO> applicationStatus = new ArrayList<>();
    private List<SkillStatDTO> topSkills = new ArrayList<>();
    private List<TopCandidateDTO> topRankedCandidates = new ArrayList<>();

    public AdminDashboardResponse() {
    }

    public AdminDashboardResponse(long totalCandidates, long totalApplications, long totalJobs,
                                  long screenedResumes, long shortlistedCandidates,
                                  double averageMatchScore, long totalInterviews, long totalUsers,
                                  ApplicantsOverviewDTO applicantsOverview,
                                  List<StatusBreakdownDTO> applicationStatus,
                                  List<SkillStatDTO> topSkills,
                                  List<TopCandidateDTO> topRankedCandidates) {
        this.totalCandidates = totalCandidates;
        this.totalApplications = totalApplications;
        this.totalJobs = totalJobs;
        this.screenedResumes = screenedResumes;
        this.shortlistedCandidates = shortlistedCandidates;
        this.averageMatchScore = averageMatchScore;
        this.totalInterviews = totalInterviews;
        this.totalUsers = totalUsers;
        this.applicantsOverview = applicantsOverview;
        this.applicationStatus = applicationStatus != null ? applicationStatus : new ArrayList<>();
        this.topSkills = topSkills != null ? topSkills : new ArrayList<>();
        this.topRankedCandidates = topRankedCandidates != null ? topRankedCandidates : new ArrayList<>();
    }

    public long getTotalCandidates() {
        return totalCandidates;
    }

    public void setTotalCandidates(long totalCandidates) {
        this.totalCandidates = totalCandidates;
    }

    public long getTotalApplications() {
        return totalApplications;
    }

    public void setTotalApplications(long totalApplications) {
        this.totalApplications = totalApplications;
    }

    public long getTotalJobs() {
        return totalJobs;
    }

    public void setTotalJobs(long totalJobs) {
        this.totalJobs = totalJobs;
    }

    public long getScreenedResumes() {
        return screenedResumes;
    }

    public void setScreenedResumes(long screenedResumes) {
        this.screenedResumes = screenedResumes;
    }

    public long getShortlistedCandidates() {
        return shortlistedCandidates;
    }

    public void setShortlistedCandidates(long shortlistedCandidates) {
        this.shortlistedCandidates = shortlistedCandidates;
    }

    public double getAverageMatchScore() {
        return averageMatchScore;
    }

    public void setAverageMatchScore(double averageMatchScore) {
        this.averageMatchScore = averageMatchScore;
    }

    public long getTotalInterviews() {
        return totalInterviews;
    }

    public void setTotalInterviews(long totalInterviews) {
        this.totalInterviews = totalInterviews;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public ApplicantsOverviewDTO getApplicantsOverview() {
        return applicantsOverview;
    }

    public void setApplicantsOverview(ApplicantsOverviewDTO applicantsOverview) {
        this.applicantsOverview = applicantsOverview;
    }

    public List<StatusBreakdownDTO> getApplicationStatus() {
        return applicationStatus;
    }

    public void setApplicationStatus(List<StatusBreakdownDTO> applicationStatus) {
        this.applicationStatus = applicationStatus;
    }

    public List<StatusBreakdownDTO> getApplicationStatusDistribution() {
        return applicationStatus;
    }

    public void setApplicationStatusDistribution(List<StatusBreakdownDTO> applicationStatusDistribution) {
        if (applicationStatusDistribution != null) {
            this.applicationStatus = applicationStatusDistribution;
        }
    }

    public List<SkillStatDTO> getTopSkills() {
        return topSkills;
    }

    public void setTopSkills(List<SkillStatDTO> topSkills) {
        this.topSkills = topSkills;
    }

    public List<TopCandidateDTO> getTopRankedCandidates() {
        return topRankedCandidates;
    }

    public void setTopRankedCandidates(List<TopCandidateDTO> topRankedCandidates) {
        this.topRankedCandidates = topRankedCandidates;
    }
}
