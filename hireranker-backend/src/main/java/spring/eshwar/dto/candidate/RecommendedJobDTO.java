package spring.eshwar.dto.candidate;

public class RecommendedJobDTO {

    private Long id;
    private String title;
    private String company;
    private String location;
    private String salaryRange;
    private String employmentType;
    private String requiredSkills;
    private double matchScore;

    public RecommendedJobDTO() {
    }

    public RecommendedJobDTO(Long id, String title, String company, String location,
                             String salaryRange, String employmentType, String requiredSkills,
                             double matchScore) {
        this.id = id;
        this.title = title;
        this.company = company;
        this.location = location;
        this.salaryRange = salaryRange;
        this.employmentType = employmentType;
        this.requiredSkills = requiredSkills;
        this.matchScore = matchScore;
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

    public String getRequiredSkills() {
        return requiredSkills;
    }

    public void setRequiredSkills(String requiredSkills) {
        this.requiredSkills = requiredSkills;
    }

    public double getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(double matchScore) {
        this.matchScore = matchScore;
    }
}
