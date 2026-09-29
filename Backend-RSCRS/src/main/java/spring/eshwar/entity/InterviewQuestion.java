package spring.eshwar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "interview_questions")
public class InterviewQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Interview is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", referencedColumnName = "id", nullable = false)
    private Interview interview;

    @NotBlank(message = "Question text cannot be blank")
    @Column(name = "question_text", columnDefinition = "TEXT", nullable = false)
    private String questionText;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "difficulty", length = 30)
    private String difficulty;

    @Column(name = "expected_concepts", columnDefinition = "TEXT")
    private String expectedConcepts;

    @Column(name = "key_points", columnDefinition = "TEXT")
    private String keyPoints;

    @Column(name = "question_order", nullable = false)
    private int questionOrder = 1;

    @NotNull(message = "Question status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private QuestionStatus status = QuestionStatus.PENDING;

    @Column(name = "time_limit_seconds")
    private Integer timeLimitSeconds = 15;

    @CreationTimestamp
    @Column(name = "asked_at", nullable = false, updatable = false)
    private LocalDateTime askedAt;

    public InterviewQuestion() {
    }

    public InterviewQuestion(Interview interview, String questionText, String category,
                             String difficulty, int questionOrder) {
        this.interview = interview;
        this.questionText = questionText;
        this.category = category;
        this.difficulty = difficulty;
        this.questionOrder = questionOrder;
        this.status = QuestionStatus.PENDING;
        this.timeLimitSeconds = 15;
    }

    @PrePersist
    protected void onCreate() {
        if (this.askedAt == null) {
            this.askedAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = QuestionStatus.PENDING;
        }
        if (this.timeLimitSeconds == null) {
            this.timeLimitSeconds = 15;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Interview getInterview() {
        return interview;
    }

    public void setInterview(Interview interview) {
        this.interview = interview;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public int getQuestionOrder() {
        return questionOrder;
    }

    public void setQuestionOrder(int questionOrder) {
        this.questionOrder = questionOrder;
    }

    public QuestionStatus getStatus() {
        return status;
    }

    public void setStatus(QuestionStatus status) {
        this.status = status;
    }

    public Integer getTimeLimitSeconds() {
        return timeLimitSeconds;
    }

    public void setTimeLimitSeconds(Integer timeLimitSeconds) {
        this.timeLimitSeconds = timeLimitSeconds;
    }

    public InterviewQuestion(Interview interview, String questionText, String category,
                             String difficulty, int questionOrder, String expectedConcepts, String keyPoints) {
        this.interview = interview;
        this.questionText = questionText;
        this.category = category;
        this.difficulty = difficulty;
        this.questionOrder = questionOrder;
        this.expectedConcepts = expectedConcepts;
        this.keyPoints = keyPoints;
        this.status = QuestionStatus.PENDING;
        this.timeLimitSeconds = 15;
    }

    public String getExpectedConcepts() {
        return expectedConcepts;
    }

    public void setExpectedConcepts(String expectedConcepts) {
        this.expectedConcepts = expectedConcepts;
    }

    public String getKeyPoints() {
        return keyPoints;
    }

    public void setKeyPoints(String keyPoints) {
        this.keyPoints = keyPoints;
    }

    public LocalDateTime getAskedAt() {
        return askedAt;
    }

    public void setAskedAt(LocalDateTime askedAt) {
        this.askedAt = askedAt;
    }
}
