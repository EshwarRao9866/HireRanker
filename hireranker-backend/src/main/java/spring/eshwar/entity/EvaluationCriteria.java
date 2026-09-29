package spring.eshwar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "evaluation_criteria")
public class EvaluationCriteria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Job is required")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", referencedColumnName = "id", nullable = false, unique = true)
    private Job job;

    @Column(name = "required_skills", columnDefinition = "TEXT")
    private String requiredSkills;

    @DecimalMin(value = "0.0", message = "Minimum experience must be non-negative")
    @Column(name = "minimum_experience")
    private Double minimumExperience;

    @Column(name = "education_requirements", length = 255)
    private String educationRequirements;

    @NotNull(message = "Skill weight is required")
    @DecimalMin(value = "0.0", message = "Skill weight must be at least 0.0")
    @DecimalMax(value = "100.0", message = "Skill weight cannot exceed 100.0")
    @Column(name = "skill_weight", nullable = false)
    private Double skillWeight = 50.0;

    @NotNull(message = "Experience weight is required")
    @DecimalMin(value = "0.0", message = "Experience weight must be at least 0.0")
    @DecimalMax(value = "100.0", message = "Experience weight cannot exceed 100.0")
    @Column(name = "experience_weight", nullable = false)
    private Double experienceWeight = 30.0;

    @NotNull(message = "Education weight is required")
    @DecimalMin(value = "0.0", message = "Education weight must be at least 0.0")
    @DecimalMax(value = "100.0", message = "Education weight cannot exceed 100.0")
    @Column(name = "education_weight", nullable = false)
    private Double educationWeight = 20.0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public EvaluationCriteria() {
    }

    public EvaluationCriteria(Job job, String requiredSkills, Double minimumExperience,
                              String educationRequirements, Double skillWeight,
                              Double experienceWeight, Double educationWeight) {
        this.job = job;
        this.requiredSkills = requiredSkills;
        this.minimumExperience = minimumExperience;
        this.educationRequirements = educationRequirements;
        this.skillWeight = skillWeight != null ? skillWeight : 50.0;
        this.experienceWeight = experienceWeight != null ? experienceWeight : 30.0;
        this.educationWeight = educationWeight != null ? educationWeight : 20.0;
    }

    public EvaluationCriteria(Long id, Job job, String requiredSkills, Double minimumExperience,
                              String educationRequirements, Double skillWeight, Double experienceWeight,
                              Double educationWeight, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.job = job;
        this.requiredSkills = requiredSkills;
        this.minimumExperience = minimumExperience;
        this.educationRequirements = educationRequirements;
        this.skillWeight = skillWeight != null ? skillWeight : 50.0;
        this.experienceWeight = experienceWeight != null ? experienceWeight : 30.0;
        this.educationWeight = educationWeight != null ? educationWeight : 20.0;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Double getTotalWeight() {
        double sw = (skillWeight != null) ? skillWeight : 0.0;
        double ew = (experienceWeight != null) ? experienceWeight : 0.0;
        double edw = (educationWeight != null) ? educationWeight : 0.0;
        return sw + ew + edw;
    }

    public boolean isTotalWeightValid() {
        return Math.abs(getTotalWeight() - 100.0) < 0.001;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
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

    @Override
    public String toString() {
        return "EvaluationCriteria{" +
                "id=" + id +
                ", jobId=" + (job != null ? job.getId() : null) +
                ", minimumExperience=" + minimumExperience +
                ", skillWeight=" + skillWeight +
                ", experienceWeight=" + experienceWeight +
                ", educationWeight=" + educationWeight +
                ", totalWeight=" + getTotalWeight() +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
