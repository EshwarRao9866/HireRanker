package spring.eshwar.dto.candidate;

import spring.eshwar.dto.interview.InterviewResponse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CandidateDashboardResponse {

    private CandidateResponse candidateProfile;
    private long totalApplications;
    private long totalResumes;
    private long screenedApplications;
    private long shortlistedApplications;
    private List<InterviewResponse> upcomingInterviews = new ArrayList<>();
    private List<CandidateRecentApplicationDTO> recentApplications = new ArrayList<>();
    private Map<String, Long> applicationStatuses = new LinkedHashMap<>();
    private List<RecommendedJobDTO> recommendedJobs = new ArrayList<>();
    private String activeResumeFileName;
    private Double activeResumeScore;

    public CandidateDashboardResponse() {
    }

    public CandidateDashboardResponse(CandidateResponse candidateProfile,
                                    long totalApplications,
                                    long totalResumes,
                                    long screenedApplications,
                                    long shortlistedApplications,
                                    List<InterviewResponse> upcomingInterviews,
                                    List<CandidateRecentApplicationDTO> recentApplications,
                                    Map<String, Long> applicationStatuses,
                                    List<RecommendedJobDTO> recommendedJobs) {
        this.candidateProfile = candidateProfile;
        this.totalApplications = totalApplications;
        this.totalResumes = totalResumes;
        this.screenedApplications = screenedApplications;
        this.shortlistedApplications = shortlistedApplications;
        this.upcomingInterviews = upcomingInterviews != null ? upcomingInterviews : new ArrayList<>();
        this.recentApplications = recentApplications != null ? recentApplications : new ArrayList<>();
        this.applicationStatuses = applicationStatuses != null ? applicationStatuses : new LinkedHashMap<>();
        this.recommendedJobs = recommendedJobs != null ? recommendedJobs : new ArrayList<>();
    }

    public CandidateResponse getCandidateProfile() {
        return candidateProfile;
    }

    public void setCandidateProfile(CandidateResponse candidateProfile) {
        this.candidateProfile = candidateProfile;
    }

    public long getTotalApplications() {
        return totalApplications;
    }

    public void setTotalApplications(long totalApplications) {
        this.totalApplications = totalApplications;
    }

    public long getTotalResumes() {
        return totalResumes;
    }

    public void setTotalResumes(long totalResumes) {
        this.totalResumes = totalResumes;
    }

    public long getScreenedApplications() {
        return screenedApplications;
    }

    public void setScreenedApplications(long screenedApplications) {
        this.screenedApplications = screenedApplications;
    }

    public long getShortlistedApplications() {
        return shortlistedApplications;
    }

    public void setShortlistedApplications(long shortlistedApplications) {
        this.shortlistedApplications = shortlistedApplications;
    }

    public List<InterviewResponse> getUpcomingInterviews() {
        return upcomingInterviews;
    }

    public void setUpcomingInterviews(List<InterviewResponse> upcomingInterviews) {
        this.upcomingInterviews = upcomingInterviews;
    }

    public List<CandidateRecentApplicationDTO> getRecentApplications() {
        return recentApplications;
    }

    public void setRecentApplications(List<CandidateRecentApplicationDTO> recentApplications) {
        this.recentApplications = recentApplications;
    }

    public Map<String, Long> getApplicationStatuses() {
        return applicationStatuses;
    }

    public void setApplicationStatuses(Map<String, Long> applicationStatuses) {
        this.applicationStatuses = applicationStatuses;
    }

    public List<RecommendedJobDTO> getRecommendedJobs() {
        return recommendedJobs;
    }

    public void setRecommendedJobs(List<RecommendedJobDTO> recommendedJobs) {
        this.recommendedJobs = recommendedJobs;
    }

    public List<RecommendedJobDTO> getAvailableJobs() {
        return recommendedJobs;
    }

    public void setAvailableJobs(List<RecommendedJobDTO> availableJobs) {
        if (availableJobs != null) {
            this.recommendedJobs = availableJobs;
        }
    }

    public String getActiveResumeFileName() {
        return activeResumeFileName;
    }

    public void setActiveResumeFileName(String activeResumeFileName) {
        this.activeResumeFileName = activeResumeFileName;
    }

    public Double getActiveResumeScore() {
        return activeResumeScore;
    }

    public void setActiveResumeScore(Double activeResumeScore) {
        this.activeResumeScore = activeResumeScore;
    }
}
