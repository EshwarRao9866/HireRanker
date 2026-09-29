package spring.eshwar.dto.application;

import spring.eshwar.dto.candidate.CandidateResponse;
import spring.eshwar.dto.resume.ResumeResponse;
import spring.eshwar.entity.Application;
import spring.eshwar.entity.ApplicationStatus;
import spring.eshwar.entity.ScreeningResult;

import java.io.Serializable;

public class ShortlistedCandidateResponse implements Serializable {

    public static class ScreeningScores implements Serializable {
        private Double overallScore;
        private Double skillsScore;
        private Double experienceScore;
        private Double educationScore;

        public ScreeningScores() {
        }

        public ScreeningScores(Double overallScore, Double skillsScore, Double experienceScore, Double educationScore) {
            this.overallScore = overallScore;
            this.skillsScore = skillsScore;
            this.experienceScore = experienceScore;
            this.educationScore = educationScore;
        }

        public Double getOverallScore() {
            return overallScore;
        }

        public void setOverallScore(Double overallScore) {
            this.overallScore = overallScore;
        }

        public Double getSkillsScore() {
            return skillsScore;
        }

        public void setSkillsScore(Double skillsScore) {
            this.skillsScore = skillsScore;
        }

        public Double getExperienceScore() {
            return experienceScore;
        }

        public void setExperienceScore(Double experienceScore) {
            this.experienceScore = experienceScore;
        }

        public Double getEducationScore() {
            return educationScore;
        }

        public void setEducationScore(Double educationScore) {
            this.educationScore = educationScore;
        }
    }

    private CandidateResponse candidate;
    private ApplicationResponse application;
    private ResumeResponse resume;
    private Double screeningScore;
    private Double overallScore;
    private Double skillsScore;
    private Double experienceScore;
    private Double educationScore;
    private ScreeningScores screeningScores;
    private ApplicationStatus applicationStatus;
    private ApplicationStatus status;

    public ShortlistedCandidateResponse() {
    }

    public ShortlistedCandidateResponse(CandidateResponse candidate,
                                        ApplicationResponse application,
                                        ResumeResponse resume,
                                        Double screeningScore,
                                        ApplicationStatus applicationStatus) {
        this.candidate = candidate;
        this.application = application;
        this.resume = resume;
        this.screeningScore = screeningScore;
        this.overallScore = screeningScore;
        this.applicationStatus = applicationStatus;
        this.status = applicationStatus;
    }

    public static ShortlistedCandidateResponse of(Application app, ScreeningResult screeningResult) {
        if (app == null) {
            return null;
        }
        CandidateResponse candRes = CandidateResponse.fromEntity(app.getCandidate());
        ApplicationResponse appRes = ApplicationResponse.fromEntity(app);
        ResumeResponse resumeRes = ResumeResponse.fromEntity(app.getResume());
        Double overall = (screeningResult != null) ? screeningResult.getOverallScore() : null;
        Double skills = (screeningResult != null) ? screeningResult.getSkillsScore() : null;
        Double exp = (screeningResult != null) ? screeningResult.getExperienceScore() : null;
        Double edu = (screeningResult != null) ? screeningResult.getEducationScore() : null;
        ApplicationStatus appStatus = app.getStatus();

        ShortlistedCandidateResponse response = new ShortlistedCandidateResponse(candRes, appRes, resumeRes, overall, appStatus);
        response.setSkillsScore(skills);
        response.setExperienceScore(exp);
        response.setEducationScore(edu);
        if (screeningResult != null) {
            response.setScreeningScores(new ScreeningScores(overall, skills, exp, edu));
        }
        return response;
    }

    public CandidateResponse getCandidate() {
        return candidate;
    }

    public void setCandidate(CandidateResponse candidate) {
        this.candidate = candidate;
    }

    public ApplicationResponse getApplication() {
        return application;
    }

    public void setApplication(ApplicationResponse application) {
        this.application = application;
    }

    public ResumeResponse getResume() {
        return resume;
    }

    public void setResume(ResumeResponse resume) {
        this.resume = resume;
    }

    public Double getScreeningScore() {
        return screeningScore;
    }

    public void setScreeningScore(Double screeningScore) {
        this.screeningScore = screeningScore;
    }

    public ApplicationStatus getApplicationStatus() {
        return applicationStatus;
    }

    public void setApplicationStatus(ApplicationStatus applicationStatus) {
        this.applicationStatus = applicationStatus;
    }

    public Long getApplicationId() {
        return application != null ? application.getId() : null;
    }

    public Long getCandidateId() {
        return candidate != null ? candidate.getId() : null;
    }

    public String getCandidateName() {
        return candidate != null ? candidate.getFullName() : null;
    }

    public String getEmail() {
        return candidate != null ? candidate.getEmail() : null;
    }

    public Double getOverallScore() {
        return overallScore != null ? overallScore : screeningScore;
    }

    public void setOverallScore(Double overallScore) {
        this.overallScore = overallScore;
        this.screeningScore = overallScore;
    }

    public Double getSkillsScore() {
        return skillsScore;
    }

    public void setSkillsScore(Double skillsScore) {
        this.skillsScore = skillsScore;
    }

    public Double getExperienceScore() {
        return experienceScore;
    }

    public void setExperienceScore(Double experienceScore) {
        this.experienceScore = experienceScore;
    }

    public Double getEducationScore() {
        return educationScore;
    }

    public void setEducationScore(Double educationScore) {
        this.educationScore = educationScore;
    }

    public ScreeningScores getScreeningScores() {
        return screeningScores;
    }

    public void setScreeningScores(ScreeningScores screeningScores) {
        this.screeningScores = screeningScores;
    }

    public ApplicationStatus getStatus() {
        return status != null ? status : applicationStatus;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
        this.applicationStatus = status;
    }

    public String getResumeFileName() {
        return resume != null ? resume.getFileName() : null;
    }

    public Long getJobId() {
        return application != null ? application.getJobId() : null;
    }

    public String getJobTitle() {
        return application != null ? application.getJobTitle() : null;
    }

    public Double getMatchScore() {
        return overallScore != null ? overallScore : screeningScore;
    }

    @Override
    public String toString() {
        return "ShortlistedCandidateResponse{" +
                "candidate=" + (candidate != null ? candidate.getFullName() : null) +
                ", applicationId=" + (application != null ? application.getId() : null) +
                ", resumeId=" + (resume != null ? resume.getId() : null) +
                ", screeningScore=" + screeningScore +
                ", applicationStatus=" + applicationStatus +
                '}';
    }
}
