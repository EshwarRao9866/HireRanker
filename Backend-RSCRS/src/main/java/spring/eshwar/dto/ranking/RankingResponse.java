package spring.eshwar.dto.ranking;

import java.io.Serializable;

/**
 * DTO representing candidate ranking details for a job.
 */
public class RankingResponse implements Serializable {

    private Integer rank;
    private Long candidateId;
    private String candidateName;
    private Long applicationId;
    private Double overallScore;
    private Double skillsScore;
    private Double experienceScore;
    private Double educationScore;
    private String applicationStatus;

    public RankingResponse() {
    }

    public RankingResponse(Integer rank, Long candidateId, String candidateName,
                           Long applicationId, Double overallScore, Double skillsScore,
                           Double experienceScore, Double educationScore, String applicationStatus) {
        this.rank = rank;
        this.candidateId = candidateId;
        this.candidateName = candidateName;
        this.applicationId = applicationId;
        this.overallScore = overallScore;
        this.skillsScore = skillsScore;
        this.experienceScore = experienceScore;
        this.educationScore = educationScore;
        this.applicationStatus = applicationStatus;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
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

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public Double getOverallScore() {
        return overallScore;
    }

    public Double getMatchScore() {
        return overallScore;
    }

    public void setOverallScore(Double overallScore) {
        this.overallScore = overallScore;
    }

    public Double getSkillsScore() {
        return skillsScore;
    }

    public void setSkillsScore(Double skillsScore) {
        this.skillsScore = skillsScore;
    }

    public Double getExperienceScore() {
        return experienceScore;
    }

    public void setExperienceScore(Double experienceScore) {
        this.experienceScore = experienceScore;
    }

    public Double getEducationScore() {
        return educationScore;
    }

    public void setEducationScore(Double educationScore) {
        this.educationScore = educationScore;
    }

    public String getApplicationStatus() {
        return applicationStatus;
    }

    public void setApplicationStatus(String applicationStatus) {
        this.applicationStatus = applicationStatus;
    }

    @Override
    public String toString() {
        return "RankingResponse{" +
                "rank=" + rank +
                ", candidateId=" + candidateId +
                ", candidateName='" + candidateName + '\'' +
                ", applicationId=" + applicationId +
                ", overallScore=" + overallScore +
                ", skillsScore=" + skillsScore +
                ", experienceScore=" + experienceScore +
                ", educationScore=" + educationScore +
                ", applicationStatus='" + applicationStatus + '\'' +
                '}';
    }
}
