package spring.eshwar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Job title is required")
    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @NotBlank(message = "Company name is required")
    @Column(name = "company", nullable = false, length = 100)
    private String company;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "responsibilities", columnDefinition = "TEXT")
    private String responsibilities;

    @Column(name = "required_skills", columnDefinition = "TEXT")
    private String requiredSkills;

    @Column(name = "experience_required", length = 50)
    private String experienceRequired;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "salary_range", length = 50)
    private String salaryRange;

    @Column(name = "employment_type", length = 50)
    private String employmentType;

    @NotNull(message = "Job status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JobStatus status = JobStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Job() {
    }

    public Job(String title, String company, String description, String requiredSkills,
               String experienceRequired, String location, String salaryRange,
               String employmentType, JobStatus status) {
        this(title, company, description, null, requiredSkills, experienceRequired, location, salaryRange, employmentType, status);
    }

    public Job(String title, String company, String description, String responsibilities, String requiredSkills,
               String experienceRequired, String location, String salaryRange,
               String employmentType, JobStatus status) {
        this.title = title;
        this.company = company;
        this.description = description;
        this.responsibilities = responsibilities;
        this.requiredSkills = requiredSkills;
        this.experienceRequired = experienceRequired;
        this.location = location;
        this.salaryRange = salaryRange;
        this.employmentType = employmentType;
        this.status = status != null ? status : JobStatus.ACTIVE;
    }

    public Job(Long id, String title, String company, String description, String requiredSkills,
               String experienceRequired, String location, String salaryRange,
               String employmentType, JobStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, title, company, description, null, requiredSkills, experienceRequired, location, salaryRange, employmentType, status, createdAt, updatedAt);
    }

    public Job(Long id, String title, String company, String description, String responsibilities, String requiredSkills,
               String experienceRequired, String location, String salaryRange,
               String employmentType, JobStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.company = company;
        this.description = description;
        this.responsibilities = responsibilities;
        this.requiredSkills = requiredSkills;
        this.experienceRequired = experienceRequired;
        this.location = location;
        this.salaryRange = salaryRange;
        this.employmentType = employmentType;
        this.status = status != null ? status : JobStatus.ACTIVE;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = JobStatus.ACTIVE;
        }
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getResponsibilities() {
        return responsibilities;
    }

    public void setResponsibilities(String responsibilities) {
        this.responsibilities = responsibilities;
    }

    public String getRequiredSkills() {
        return requiredSkills;
    }

    public void setRequiredSkills(String requiredSkills) {
        this.requiredSkills = requiredSkills;
    }

    public String getExperienceRequired() {
        return experienceRequired;
    }

    public void setExperienceRequired(String experienceRequired) {
        this.experienceRequired = experienceRequired;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSalaryRange() {
        return salaryRange;
    }

    public void setSalaryRange(String salaryRange) {
        this.salaryRange = salaryRange;
    }

    public String getEmploymentType() {
        return employmentType;
    }

    public void setEmploymentType(String employmentType) {
        this.employmentType = employmentType;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
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
        return "Job{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", company='" + company + '\'' +
                ", experienceRequired='" + experienceRequired + '\'' +
                ", location='" + location + '\'' +
                ", salaryRange='" + salaryRange + '\'' +
                ", employmentType='" + employmentType + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
