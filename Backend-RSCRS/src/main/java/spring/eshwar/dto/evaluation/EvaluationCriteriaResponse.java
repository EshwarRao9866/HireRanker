package spring.eshwar.dto.evaluation;

import spring.eshwar.entity.EvaluationCriteria;

import java.time.LocalDateTime;

public class EvaluationCriteriaResponse {

    private Long id;
    private Long jobId;
    private String jobTitle;
    private String requiredSkills;
    private Double minimumExperience;
    private String educationRequirements;
    private Double skillWeight;
    private Double experienceWeight;
    private Double educationWeight;
    private Double totalWeight;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public EvaluationCriteriaResponse() {
    }

    public EvaluationCriteriaResponse(Long id, Long jobId, String jobTitle, String requiredSkills,
                                      Double minimumExperience, String educationRequirements,
                                      Double skillWeight, Double experienceWeight, Double educationWeight,
                                      Double totalWeight, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.jobId = jobId;
        this.jobTitle = jobTitle;
        this.requiredSkills = requiredSkills;
        this.minimumExperience = minimumExperience;
        this.educationRequirements = educationRequirements;
        this.skillWeight = skillWeight;
        this.experienceWeight = experienceWeight;
        this.educationWeight = educationWeight;
        this.totalWeight = totalWeight;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static EvaluationCriteriaResponse fromEntity(EvaluationCriteria criteria) {
        if (criteria == null) {
            return null;
        }

        Long jobId = (criteria.getJob() != null) ? criteria.getJob().getId() : null;
        String jobTitle = (criteria.getJob() != null) ? criteria.getJob().getTitle() : null;

        return new EvaluationCriteriaResponse(
                criteria.getId(),
                jobId,
                jobTitle,
                criteria.getRequiredSkills(),
                criteria.getMinimumExperience(),
                criteria.getEducationRequirements(),
                criteria.getSkillWeight(),
                criteria.getExperienceWeight(),
                criteria.getEducationWeight(),
                criteria.getTotalWeight(),
                criteria.getCreatedAt(),
                criteria.getUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Double getTotalWeight() {
        return totalWeight;
    }

    public void setTotalWeight(Double totalWeight) {
        this.totalWeight = totalWeight;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
