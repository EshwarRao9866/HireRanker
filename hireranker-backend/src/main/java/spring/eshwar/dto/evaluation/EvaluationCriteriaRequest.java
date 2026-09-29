package spring.eshwar.dto.evaluation;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public class EvaluationCriteriaRequest {

    @NotNull(message = "Job ID is required")
    private Long jobId;

    private String requiredSkills;

    @DecimalMin(value = "0.0", message = "Minimum experience must be non-negative")
    private Double minimumExperience;

    private String educationRequirements;

    @NotNull(message = "Skill weight is required")
    @DecimalMin(value = "0.0", message = "Skill weight must be at least 0.0")
    @DecimalMax(value = "100.0", message = "Skill weight cannot exceed 100.0")
    private Double skillWeight;

    @NotNull(message = "Experience weight is required")
    @DecimalMin(value = "0.0", message = "Experience weight must be at least 0.0")
    @DecimalMax(value = "100.0", message = "Experience weight cannot exceed 100.0")
    private Double experienceWeight;

    @NotNull(message = "Education weight is required")
    @DecimalMin(value = "0.0", message = "Education weight must be at least 0.0")
    @DecimalMax(value = "100.0", message = "Education weight cannot exceed 100.0")
    private Double educationWeight;

    public EvaluationCriteriaRequest() {
    }

    public EvaluationCriteriaRequest(Long jobId, String requiredSkills, Double minimumExperience,
                                     String educationRequirements, Double skillWeight,
                                     Double experienceWeight, Double educationWeight) {
        this.jobId = jobId;
        this.requiredSkills = requiredSkills;
        this.minimumExperience = minimumExperience;
        this.educationRequirements = educationRequirements;
        this.skillWeight = skillWeight;
        this.experienceWeight = experienceWeight;
        this.educationWeight = educationWeight;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getRequiredSkills() {
        return requiredSkills;
    }

    public void setRequiredSkills(String requiredSkills) {
        this.requiredSkills = requiredSkills;
    }

    public Double getMinimumExperience() {
        return minimumExperience;
    }

    public void setMinimumExperience(Double minimumExperience) {
        this.minimumExperience = minimumExperience;
    }

    public String getEducationRequirements() {
        return educationRequirements;
    }

    public void setEducationRequirements(String educationRequirements) {
        this.educationRequirements = educationRequirements;
    }

    public Double getSkillWeight() {
        return skillWeight;
    }

    public void setSkillWeight(Double skillWeight) {
        this.skillWeight = skillWeight;
    }

    public Double getExperienceWeight() {
        return experienceWeight;
    }

    public void setExperienceWeight(Double experienceWeight) {
        this.experienceWeight = experienceWeight;
    }

    public Double getEducationWeight() {
        return educationWeight;
    }

    public void setEducationWeight(Double educationWeight) {
        this.educationWeight = educationWeight;
    }
}
