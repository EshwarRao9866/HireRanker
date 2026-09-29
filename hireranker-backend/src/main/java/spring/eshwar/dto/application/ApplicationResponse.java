package spring.eshwar.dto.application;

import spring.eshwar.entity.Application;
import spring.eshwar.entity.ApplicationStatus;
import spring.eshwar.entity.ScreeningResult;

import java.time.LocalDateTime;

public class ApplicationResponse {

    private Long id;
    private Long candidateId;
    private String candidateName;
    private String candidateEmail;
    private Long jobId;
    private String jobTitle;
    private String company;
    private Long resumeId;
    private String resumeFileName;
    private ApplicationStatus status;
    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;

    private String phone;
    private String location;
    private String skills;
    private String experience;
    private String education;
    private String github;
    private String linkedin;
    private Double matchScore;

    public ApplicationResponse() {
    }

    public ApplicationResponse(Long id, Long candidateId, String candidateName, String candidateEmail,
                               Long jobId, String jobTitle, String company, Long resumeId,
                               String resumeFileName, ApplicationStatus status,
                               LocalDateTime appliedAt, LocalDateTime updatedAt) {
        this.id = id;
        this.candidateId = candidateId;
        this.candidateName = candidateName;
        this.candidateEmail = candidateEmail;
        this.jobId = jobId;
        this.jobTitle = jobTitle;
        this.company = company;
        this.resumeId = resumeId;
        this.resumeFileName = resumeFileName;
        this.status = status;
        this.appliedAt = appliedAt;
        this.updatedAt = updatedAt;
    }

    public static ApplicationResponse fromEntity(Application app) {
        if (app == null) {
            return null;
        }

        Long candidateId = null;
        String candidateName = null;
        String candidateEmail = null;
        try {
            if (app.getCandidate() != null) {
                candidateId = app.getCandidate().getId();
                candidateName = app.getCandidate().getFullName();
                if (app.getCandidate().getUser() != null) {
                    candidateEmail = app.getCandidate().getUser().getEmail();
                }
            }
        } catch (Exception ignored) {}

        Long jobId = null;
        String jobTitle = null;
        String company = null;
        try {
            if (app.getJob() != null) {
                jobId = app.getJob().getId();
                jobTitle = app.getJob().getTitle();
                company = app.getJob().getCompany();
            }
        } catch (Exception ignored) {}

        Long resumeId = null;
        String resumeFileName = null;
        try {
            if (app.getResume() != null) {
                resumeId = app.getResume().getId();
                resumeFileName = app.getResume().getFileName();
            }
        } catch (Exception ignored) {}

        ApplicationResponse resp = new ApplicationResponse(
                app.getId(),
                candidateId,
                candidateName,
                candidateEmail,
                jobId,
                jobTitle,
                company,
                resumeId,
                resumeFileName,
                app.getStatus(),
                app.getAppliedAt(),
                app.getUpdatedAt()
        );

        try {
            if (app.getCandidate() != null) {
                resp.setPhone(app.getCandidate().getPhone());
                resp.setLocation(app.getCandidate().getLocation());
                resp.setSkills(app.getCandidate().getSkills());
                resp.setExperience(app.getCandidate().getExperience());
                resp.setEducation(app.getCandidate().getEducation());
                resp.setGithub(app.getCandidate().getGithub());
                resp.setLinkedin(app.getCandidate().getLinkedin());
            }
        } catch (Exception ignored) {}

        return resp;
    }

    public static ApplicationResponse fromEntity(Application app, ScreeningResult sr) {
        ApplicationResponse resp = fromEntity(app);
        if (resp != null && sr != null) {
            resp.setMatchScore(sr.getOverallScore());
        }
        return resp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(Long candidateId) {
        this.candidateId = candidateId;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public String getCandidateEmail() {
        return candidateEmail;
    }

    public void setCandidateEmail(String candidateEmail) {
        this.candidateEmail = candidateEmail;
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

    public Long getResumeId() {
        return resumeId;
    }

    public void setResumeId(Long resumeId) {
        this.resumeId = resumeId;
    }

    public String getResumeFileName() {
        return resumeFileName;
    }

    public void setResumeFileName(String resumeFileName) {
        this.resumeFileName = resumeFileName;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public String getEducation() {
        return education;
    }

    public void setEducation(String education) {
        this.education = education;
    }

    public String getGithub() {
        return github;
    }

    public void setGithub(String github) {
        this.github = github;
    }

    public String getLinkedin() {
        return linkedin;
    }

    public void setLinkedin(String linkedin) {
        this.linkedin = linkedin;
    }

    public Double getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(Double matchScore) {
        this.matchScore = matchScore;
    }
}
