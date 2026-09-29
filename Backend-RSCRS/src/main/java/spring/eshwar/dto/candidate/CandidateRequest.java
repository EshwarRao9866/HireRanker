package spring.eshwar.dto.candidate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CandidateRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String phone;
    private String location;
    private String skills;
    private String experience;
    private String education;
    private String github;
    private String linkedin;

    public CandidateRequest() {
    }

    public CandidateRequest(Long userId, String fullName, String phone, String location,
                            String skills, String experience, String education,
                            String github, String linkedin) {
        this.userId = userId;
        this.fullName = fullName;
        this.phone = phone;
        this.location = location;
        this.skills = skills;
        this.experience = experience;
        this.education = education;
        this.github = github;
        this.linkedin = linkedin;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public String getEducation() {
        return education;
    }

    public void setEducation(String education) {
        this.education = education;
    }

    public String getGithub() {
        return github;
    }

    public void setGithub(String github) {
        this.github = github;
    }

    public String getLinkedin() {
        return linkedin;
    }

    public void setLinkedin(String linkedin) {
        this.linkedin = linkedin;
    }
}
