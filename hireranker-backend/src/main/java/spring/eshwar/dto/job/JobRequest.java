package spring.eshwar.dto.job;

import jakarta.validation.constraints.NotBlank;
import spring.eshwar.entity.JobStatus;

public class JobRequest {

    @NotBlank(message = "Job title is required")
    private String title;

    @NotBlank(message = "Company name is required")
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

    public JobRequest() {
    }

    public JobRequest(String title, String company, String description, String requiredSkills,
                      String experienceRequired, String location, String salaryRange,
                      String employmentType, JobStatus status) {
        this(title, company, description, null, requiredSkills, experienceRequired, location, salaryRange, employmentType, status);
    }

    public JobRequest(String title, String company, String description, String responsibilities, String requiredSkills,
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
        this.status = status;
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

    public void setRequiredSkills(Object skills) {
        if (skills == null) {
            this.requiredSkills = null;
        } else if (skills instanceof java.util.Collection<?>) {
            this.requiredSkills = String.join(", ", ((java.util.Collection<?>) skills).stream().map(Object::toString).toList());
        } else if (skills.getClass().isArray()) {
            this.requiredSkills = String.join(", ", java.util.Arrays.stream((Object[]) skills).map(Object::toString).toList());
        } else {
            this.requiredSkills = skills.toString();
        }
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
}
