package spring.eshwar.dto.job;

import spring.eshwar.entity.Job;
import spring.eshwar.entity.JobStatus;

import java.time.LocalDateTime;

public class JobResponse {

    private Long id;
    private String title;
    private String company;
    private String department;
    private String description;
    private String responsibilities;
    private String requiredSkills;
    private String experienceRequired;
    private String location;
    private String salaryRange;
    private String employmentType;
    private JobStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public JobResponse() {
    }

    public JobResponse(Long id, String title, String company, String description, String requiredSkills,
                       String experienceRequired, String location, String salaryRange,
                       String employmentType, JobStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, title, company, null, description, null, requiredSkills, experienceRequired, location, salaryRange, employmentType, status, createdAt, updatedAt);
    }

    public JobResponse(Long id, String title, String company, String description, String responsibilities, String requiredSkills,
                       String experienceRequired, String location, String salaryRange,
                       String employmentType, JobStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, title, company, null, description, responsibilities, requiredSkills, experienceRequired, location, salaryRange, employmentType, status, createdAt, updatedAt);
    }

    public JobResponse(Long id, String title, String company, String department, String description, String responsibilities, String requiredSkills,
                       String experienceRequired, String location, String salaryRange,
                       String employmentType, JobStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.company = company;
        this.department = department;
        this.description = description;
        this.responsibilities = responsibilities;
        this.requiredSkills = requiredSkills;
        this.experienceRequired = experienceRequired;
        this.location = location;
        this.salaryRange = salaryRange;
        this.employmentType = employmentType;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static JobResponse fromEntity(Job job) {
        if (job == null) {
            return null;
        }
        return new JobResponse(
                job.getId(),
                spring.eshwar.util.SalaryFormatter.cleanTitle(job.getTitle()),
                job.getCompany(),
                job.getDepartment(),
                job.getDescription(),
                job.getResponsibilities(),
                job.getRequiredSkills(),
                job.getExperienceRequired(),
                job.getLocation(),
                spring.eshwar.util.SalaryFormatter.format(job.getSalaryRange()),
                job.getEmploymentType(),
                job.getStatus(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
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
}
