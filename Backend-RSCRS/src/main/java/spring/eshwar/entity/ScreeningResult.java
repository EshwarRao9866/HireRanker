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
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "screening_results")
public class ScreeningResult {

    public enum Recommendation {
        RECOMMENDED,
        MAYBE,
        NOT_RECOMMENDED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Application is required")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", referencedColumnName = "id", nullable = false, unique = true)
    private Application application;

    @NotNull(message = "Overall score is required")
    @DecimalMin(value = "0.0", message = "Overall score must be at least 0.0")
    @DecimalMax(value = "100.0", message = "Overall score cannot exceed 100.0")
    @Column(name = "overall_score", nullable = false)
    private Double overallScore;

    @DecimalMin(value = "0.0", message = "Skills score must be at least 0.0")
    @DecimalMax(value = "100.0", message = "Skills score cannot exceed 100.0")
    @Column(name = "skills_score")
    private Double skillsScore;

    @DecimalMin(value = "0.0", message = "Experience score must be at least 0.0")
    @DecimalMax(value = "100.0", message = "Experience score cannot exceed 100.0")
    @Column(name = "experience_score")
    private Double experienceScore;

    @DecimalMin(value = "0.0", message = "Education score must be at least 0.0")
    @DecimalMax(value = "100.0", message = "Education score cannot exceed 100.0")
    @Column(name = "education_score")
    private Double educationScore;

    @Column(name = "keyword_score")
    private Double keywordScore;

    @Column(name = "project_score")
    private Double projectScore;

    @Column(name = "certification_score")
    private Double certificationScore;

    @Column(name = "formatting_score")
    private Double formattingScore;

    @Column(name = "achievement_score")
    private Double achievementScore;

    @Column(name = "matching_skills", columnDefinition = "TEXT")
    private String matchingSkills;

    @Column(name = "missing_skills", columnDefinition = "TEXT")
    private String missingSkills;

    @Column(name = "recommended_skills", columnDefinition = "TEXT")
    private String recommendedSkills;

    @Column(name = "strengths", columnDefinition = "TEXT")
    private String strengths;

    @Column(name = "weaknesses", columnDefinition = "TEXT")
    private String weaknesses;

    @Column(name = "improvement_suggestions", columnDefinition = "TEXT")
    private String improvementSuggestions;

    @Column(name = "resume_summary", columnDefinition = "TEXT")
    private String resumeSummary;

    @Column(name = "recommendation", length = 50)
    private String recommendation;

    @CreationTimestamp
    @Column(name = "screened_at", nullable = false, updatable = false)
    private LocalDateTime screenedAt;

    public ScreeningResult() {
    }

    public ScreeningResult(Application application, Double overallScore, Double skillsScore,
                           Double experienceScore, Double educationScore, String matchingSkills,
                           String missingSkills, String recommendation) {
        this.application = application;
        this.overallScore = overallScore;
        this.skillsScore = skillsScore;
        this.experienceScore = experienceScore;
        this.educationScore = educationScore;
        this.matchingSkills = matchingSkills;
        this.missingSkills = missingSkills;
        this.recommendation = recommendation;
    }

    public ScreeningResult(Application application, Double overallScore, Double skillsScore,
                           Double experienceScore, Double educationScore, String matchingSkills,
                           String missingSkills, Recommendation recommendation) {
        this.application = application;
        this.overallScore = overallScore;
        this.skillsScore = skillsScore;
        this.experienceScore = experienceScore;
        this.educationScore = educationScore;
        this.matchingSkills = matchingSkills;
        this.missingSkills = missingSkills;
        this.recommendation = recommendation != null ? recommendation.name() : null;
    }

    @PrePersist
    protected void onCreate() {
        if (this.screenedAt == null) {
            this.screenedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public Double getOverallScore() {
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

    public Double getKeywordScore() {
        return keywordScore;
    }

    public void setKeywordScore(Double keywordScore) {
        this.keywordScore = keywordScore;
    }

    public Double getProjectScore() {
        return projectScore;
    }

    public void setProjectScore(Double projectScore) {
        this.projectScore = projectScore;
    }

    public Double getCertificationScore() {
        return certificationScore;
    }

    public void setCertificationScore(Double certificationScore) {
        this.certificationScore = certificationScore;
    }

    public Double getFormattingScore() {
        return formattingScore;
    }

    public void setFormattingScore(Double formattingScore) {
        this.formattingScore = formattingScore;
    }

    public Double getAchievementScore() {
        return achievementScore;
    }

    public void setAchievementScore(Double achievementScore) {
        this.achievementScore = achievementScore;
    }

    public String getRecommendedSkills() {
        return recommendedSkills;
    }

    public void setRecommendedSkills(String recommendedSkills) {
        this.recommendedSkills = recommendedSkills;
    }

    public String getStrengths() {
        return strengths;
    }

    public void setStrengths(String strengths) {
        this.strengths = strengths;
    }

    public String getWeaknesses() {
        return weaknesses;
    }

    public void setWeaknesses(String weaknesses) {
        this.weaknesses = weaknesses;
    }

    public String getImprovementSuggestions() {
        return improvementSuggestions;
    }

    public void setImprovementSuggestions(String improvementSuggestions) {
        this.improvementSuggestions = improvementSuggestions;
    }

    public String getResumeSummary() {
        return resumeSummary;
    }

    public void setResumeSummary(String resumeSummary) {
        this.resumeSummary = resumeSummary;
    }

    public String getMatchingSkills() {
        return matchingSkills;
    }

    public void setMatchingSkills(String matchingSkills) {
        this.matchingSkills = matchingSkills;
    }

    public String getMissingSkills() {
        return missingSkills;
    }

    public void setMissingSkills(String missingSkills) {
        this.missingSkills = missingSkills;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public void setRecommendation(Recommendation recommendation) {
        this.recommendation = recommendation != null ? recommendation.name() : null;
    }

    public LocalDateTime getScreenedAt() {
        return screenedAt;
    }

    public void setScreenedAt(LocalDateTime screenedAt) {
        this.screenedAt = screenedAt;
    }

    @Override
    public String toString() {
        return "ScreeningResult{" +
                "id=" + id +
                ", applicationId=" + (application != null ? application.getId() : null) +
                ", overallScore=" + overallScore +
                ", skillsScore=" + skillsScore +
                ", experienceScore=" + experienceScore +
                ", educationScore=" + educationScore +
                ", recommendation='" + recommendation + '\'' +
                ", screenedAt=" + screenedAt +
                '}';
    }
}
