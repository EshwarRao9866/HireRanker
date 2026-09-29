package spring.eshwar.dto.dashboard;

public class TopCandidateDTO {

    private int rank;
    private Long candidateId;
    private String candidateName;
    private String email;
    private String appliedRole;
    private double matchScore;
    private double skillsScore;
    private double experienceScore;
    private double educationScore;
    private String applicationStatus;
    private String resumeFileName;
    private Long resumeId;
    private Long applicationId;

    public TopCandidateDTO() {
    }

    public TopCandidateDTO(int rank, Long candidateId, String candidateName, String email,
                           String appliedRole, double matchScore, double skillsScore,
                           double experienceScore, double educationScore,
                           String applicationStatus, String resumeFileName) {
        this(rank, candidateId, candidateName, email, appliedRole, matchScore, skillsScore,
             experienceScore, educationScore, applicationStatus, resumeFileName, null, null);
    }

    public TopCandidateDTO(int rank, Long candidateId, String candidateName, String email,
                           String appliedRole, double matchScore, double skillsScore,
                           double experienceScore, double educationScore,
                           String applicationStatus, String resumeFileName,
                           Long resumeId, Long applicationId) {
        this.rank = rank;
        this.candidateId = candidateId;
        this.candidateName = candidateName;
        this.email = email;
        this.appliedRole = appliedRole;
        this.matchScore = matchScore;
        this.skillsScore = skillsScore;
        this.experienceScore = experienceScore;
        this.educationScore = educationScore;
        this.applicationStatus = applicationStatus;
        this.resumeFileName = resumeFileName;
        this.resumeId = resumeId;
        this.applicationId = applicationId;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public Long getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(Long candidateId) {
        this.candidateId = candidateId;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAppliedRole() {
        return appliedRole;
    }

    public void setAppliedRole(String appliedRole) {
        this.appliedRole = appliedRole;
    }

    public double getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(double matchScore) {
        this.matchScore = matchScore;
    }

    public double getSkillsScore() {
        return skillsScore;
    }

    public void setSkillsScore(double skillsScore) {
        this.skillsScore = skillsScore;
    }

    public double getExperienceScore() {
        return experienceScore;
    }

    public void setExperienceScore(double experienceScore) {
        this.experienceScore = experienceScore;
    }

    public double getEducationScore() {
        return educationScore;
    }

    public void setEducationScore(double educationScore) {
        this.educationScore = educationScore;
    }

    public String getApplicationStatus() {
        return applicationStatus;
    }

    public void setApplicationStatus(String applicationStatus) {
        this.applicationStatus = applicationStatus;
    }

    public String getResumeFileName() {
        return resumeFileName;
    }

    public void setResumeFileName(String resumeFileName) {
        this.resumeFileName = resumeFileName;
    }

    public Long getResumeId() {
        return resumeId;
    }

    public void setResumeId(Long resumeId) {
        this.resumeId = resumeId;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }
}
