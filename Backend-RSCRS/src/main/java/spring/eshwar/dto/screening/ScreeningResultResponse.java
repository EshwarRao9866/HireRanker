package spring.eshwar.dto.screening;

import spring.eshwar.entity.ScreeningResult;

import java.time.LocalDateTime;

public class ScreeningResultResponse {

    private Long id;
    private Long applicationId;
    private String candidateName;
    private String jobTitle;
    private Double overallScore;
    private Double skillsScore;
    private Double experienceScore;
    private Double educationScore;
    private Double keywordScore;
    private Double projectScore;
    private Double certificationScore;
    private Double formattingScore;
    private Double achievementScore;
    private String matchingSkills;
    private String missingSkills;
    private String recommendedSkills;
    private String strengths;
    private String weaknesses;
    private String improvementSuggestions;
    private String resumeSummary;
    private String recommendation;
    private LocalDateTime screenedAt;

    public ScreeningResultResponse() {
    }

    public ScreeningResultResponse(Long id, Long applicationId, String candidateName, String jobTitle,
                                   Double overallScore, Double skillsScore, Double experienceScore,
                                   Double educationScore, String matchingSkills, String missingSkills,
                                   String recommendation, LocalDateTime screenedAt) {
        this.id = id;
        this.applicationId = applicationId;
        this.candidateName = candidateName;
        this.jobTitle = jobTitle;
        this.overallScore = overallScore;
        this.skillsScore = skillsScore;
        this.experienceScore = experienceScore;
        this.educationScore = educationScore;
        this.matchingSkills = matchingSkills;
        this.missingSkills = missingSkills;
        this.recommendation = recommendation;
        this.screenedAt = screenedAt;
    }

    public static ScreeningResultResponse fromEntity(ScreeningResult result) {
        if (result == null) {
            return null;
        }

        Long applicationId = (result.getApplication() != null) ? result.getApplication().getId() : null;
        String candidateName = (result.getApplication() != null && result.getApplication().getCandidate() != null)
                ? result.getApplication().getCandidate().getFullName() : null;
        String jobTitle = (result.getApplication() != null && result.getApplication().getJob() != null)
                ? result.getApplication().getJob().getTitle() : null;

        ScreeningResultResponse resp = new ScreeningResultResponse(
                result.getId(),
                applicationId,
                candidateName,
                jobTitle,
                result.getOverallScore(),
                result.getSkillsScore(),
                result.getExperienceScore(),
                result.getEducationScore(),
                result.getMatchingSkills(),
                result.getMissingSkills(),
                result.getRecommendation(),
                result.getScreenedAt()
        );
        resp.setKeywordScore(result.getKeywordScore());
        resp.setProjectScore(result.getProjectScore());
        resp.setCertificationScore(result.getCertificationScore());
        resp.setFormattingScore(result.getFormattingScore());
        resp.setAchievementScore(result.getAchievementScore());
        resp.setRecommendedSkills(result.getRecommendedSkills());
        resp.setStrengths(result.getStrengths());
        resp.setWeaknesses(result.getWeaknesses());
        resp.setImprovementSuggestions(result.getImprovementSuggestions());
        resp.setResumeSummary(result.getResumeSummary());
        return resp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
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

    public Double getKeywordScore() {
        return keywordScore;
    }

    public void setKeywordScore(Double keywordScore) {
        this.keywordScore = keywordScore;
    }

    public Double getProjectScore() {
        return projectScore;
    }

    public void setProjectScore(Double projectScore) {
        this.projectScore = projectScore;
    }

    public Double getCertificationScore() {
        return certificationScore;
    }

    public void setCertificationScore(Double certificationScore) {
        this.certificationScore = certificationScore;
    }

    public Double getFormattingScore() {
        return formattingScore;
    }

    public void setFormattingScore(Double formattingScore) {
        this.formattingScore = formattingScore;
    }

    public Double getAchievementScore() {
        return achievementScore;
    }

    public void setAchievementScore(Double achievementScore) {
        this.achievementScore = achievementScore;
    }

    public String getRecommendedSkills() {
        return recommendedSkills;
    }

    public void setRecommendedSkills(String recommendedSkills) {
        this.recommendedSkills = recommendedSkills;
    }

    public String getStrengths() {
        return strengths;
    }

    public void setStrengths(String strengths) {
        this.strengths = strengths;
    }

    public String getWeaknesses() {
        return weaknesses;
    }

    public void setWeaknesses(String weaknesses) {
        this.weaknesses = weaknesses;
    }

    public String getImprovementSuggestions() {
        return improvementSuggestions;
    }

    public void setImprovementSuggestions(String improvementSuggestions) {
        this.improvementSuggestions = improvementSuggestions;
    }

    public String getResumeSummary() {
        return resumeSummary;
    }

    public void setResumeSummary(String resumeSummary) {
        this.resumeSummary = resumeSummary;
    }

    public String getMatchingSkills() {
        return matchingSkills;
    }

    public void setMatchingSkills(String matchingSkills) {
        this.matchingSkills = matchingSkills;
    }

    public String getMissingSkills() {
        return missingSkills;
    }

    public void setMissingSkills(String missingSkills) {
        this.missingSkills = missingSkills;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public LocalDateTime getScreenedAt() {
        return screenedAt;
    }

    public void setScreenedAt(LocalDateTime screenedAt) {
        this.screenedAt = screenedAt;
    }
}
