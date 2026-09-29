package spring.eshwar.dto.candidate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CandidateProfileUpdateRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    @Pattern(regexp = "^[+0-9\\s()\\-]{7,20}$|^$", message = "Invalid phone number format")
    private String phone;

    @Size(max = 100, message = "Location cannot exceed 100 characters")
    private String location;

    @Size(max = 2000, message = "Skills description cannot exceed 2000 characters")
    private String skills;

    @Size(max = 50, message = "Experience cannot exceed 50 characters")
    private String experience;

    @Size(max = 255, message = "Education cannot exceed 255 characters")
    private String education;

    @Size(max = 255, message = "GitHub profile link cannot exceed 255 characters")
    private String github;

    @Size(max = 255, message = "LinkedIn profile link cannot exceed 255 characters")
    private String linkedin;

    public CandidateProfileUpdateRequest() {
    }

    public CandidateProfileUpdateRequest(String fullName, String phone, String location,
                                        String skills, String experience, String education,
                                        String github, String linkedin) {
        this.fullName = fullName;
        this.phone = phone;
        this.location = location;
        this.skills = skills;
        this.experience = experience;
        this.education = education;
        this.github = github;
        this.linkedin = linkedin;
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
