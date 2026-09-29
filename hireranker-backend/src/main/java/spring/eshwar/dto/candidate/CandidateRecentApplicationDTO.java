package spring.eshwar.dto.candidate;

import java.time.LocalDateTime;

public class CandidateRecentApplicationDTO {

    private Long applicationId;
    private Long jobId;
    private String jobTitle;
    private String company;
    private String location;
    private String status;
    private Double matchScore;
    private String resumeFileName;
    private LocalDateTime appliedAt;

    public CandidateRecentApplicationDTO() {
    }

    public CandidateRecentApplicationDTO(Long applicationId, Long jobId, String jobTitle,
                                         String company, String location, String status,
                                         Double matchScore, String resumeFileName,
                                         LocalDateTime appliedAt) {
        this.applicationId = applicationId;
        this.jobId = jobId;
        this.jobTitle = jobTitle;
        this.company = company;
        this.location = location;
        this.status = status;
        this.matchScore = matchScore;
        this.resumeFileName = resumeFileName;
        this.appliedAt = appliedAt;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(Double matchScore) {
        this.matchScore = matchScore;
    }

    public String getResumeFileName() {
        return resumeFileName;
    }

    public void setResumeFileName(String resumeFileName) {
        this.resumeFileName = resumeFileName;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }
}
