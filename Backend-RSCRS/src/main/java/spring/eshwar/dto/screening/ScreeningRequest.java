package spring.eshwar.dto.screening;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public class ScreeningRequest {

    @NotNull(message = "Application ID is required")
    private Long applicationId;

    @DecimalMin(value = "0.0", message = "Overall score must be at least 0.0")
    @DecimalMax(value = "100.0", message = "Overall score cannot exceed 100.0")
    private Double overallScore;

    @DecimalMin(value = "0.0", message = "Skills score must be at least 0.0")
    @DecimalMax(value = "100.0", message = "Skills score cannot exceed 100.0")
    private Double skillsScore;

    @DecimalMin(value = "0.0", message = "Experience score must be at least 0.0")
    @DecimalMax(value = "100.0", message = "Experience score cannot exceed 100.0")
    private Double experienceScore;

    @DecimalMin(value = "0.0", message = "Education score must be at least 0.0")
    @DecimalMax(value = "100.0", message = "Education score cannot exceed 100.0")
    private Double educationScore;

    private String matchingSkills;
    private String missingSkills;
    private String recommendation;

    public ScreeningRequest() {
    }

    public ScreeningRequest(Long applicationId, Double overallScore, Double skillsScore,
                            Double experienceScore, Double educationScore, String matchingSkills,
                            String missingSkills, String recommendation) {
        this.applicationId = applicationId;
        this.overallScore = overallScore;
        this.skillsScore = skillsScore;
        this.experienceScore = experienceScore;
        this.educationScore = educationScore;
        this.matchingSkills = matchingSkills;
        this.missingSkills = missingSkills;
        this.recommendation = recommendation;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
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
}
