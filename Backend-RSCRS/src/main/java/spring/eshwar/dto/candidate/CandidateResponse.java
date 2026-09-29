package spring.eshwar.dto.candidate;

import spring.eshwar.entity.Candidate;

import java.time.LocalDateTime;

public class CandidateResponse {

    private Long id;
    private Long userId;
    private String email;
    private String fullName;
    private String phone;
    private String location;
    private String skills;
    private String experience;
    private String education;
    private String github;
    private String linkedin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CandidateResponse() {
    }

    public CandidateResponse(Long id, Long userId, String email, String fullName, String phone,
                             String location, String skills, String experience, String education,
                             String github, String linkedin, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.location = location;
        this.skills = skills;
        this.experience = experience;
        this.education = education;
        this.github = github;
        this.linkedin = linkedin;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CandidateResponse fromEntity(Candidate candidate) {
        if (candidate == null) {
            return null;
        }
        Long userId = (candidate.getUser() != null) ? candidate.getUser().getId() : null;
        String email = (candidate.getUser() != null) ? candidate.getUser().getEmail() : null;

        return new CandidateResponse(
                candidate.getId(),
                userId,
                email,
                candidate.getFullName(),
                candidate.getPhone(),
                candidate.getLocation(),
                candidate.getSkills(),
                candidate.getExperience(),
                candidate.getEducation(),
                candidate.getGithub(),
                candidate.getLinkedin(),
                candidate.getCreatedAt(),
                candidate.getUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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
