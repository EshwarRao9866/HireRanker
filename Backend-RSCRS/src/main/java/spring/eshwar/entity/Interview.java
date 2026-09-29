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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "interviews")
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Application is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", referencedColumnName = "id", nullable = false)
    private Application application;

    @NotNull(message = "Scheduled date and time is required")
    @Column(name = "scheduled_date_time", nullable = false)
    private LocalDateTime scheduledDateTime;

    @Column(name = "interview_type", length = 50)
    private String interviewType;

    @Column(name = "meeting_link", length = 500)
    private String meetingLink;

    @NotNull(message = "Interview status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private InterviewStatus status = InterviewStatus.SCHEDULED;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Interview() {
    }

    public Interview(Application application, LocalDateTime scheduledDateTime, String interviewType,
                     String meetingLink, String notes) {
        this.application = application;
        this.scheduledDateTime = scheduledDateTime;
        this.interviewType = interviewType;
        this.meetingLink = meetingLink;
        this.notes = notes;
        this.status = InterviewStatus.SCHEDULED;
    }

    public Interview(Application application, LocalDateTime scheduledDateTime, String interviewType,
                     String meetingLink, InterviewStatus status, String notes) {
        this.application = application;
        this.scheduledDateTime = scheduledDateTime;
        this.interviewType = interviewType;
        this.meetingLink = meetingLink;
        this.status = status != null ? status : InterviewStatus.SCHEDULED;
        this.notes = notes;
    }

    public Interview(Long id, Application application, LocalDateTime scheduledDateTime, String interviewType,
                     String meetingLink, InterviewStatus status, String notes,
                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.application = application;
        this.scheduledDateTime = scheduledDateTime;
        this.interviewType = interviewType;
        this.meetingLink = meetingLink;
        this.status = status != null ? status : InterviewStatus.SCHEDULED;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = InterviewStatus.SCHEDULED;
        }
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
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

    public LocalDateTime getScheduledDateTime() {
        return scheduledDateTime;
    }

    public void setScheduledDateTime(LocalDateTime scheduledDateTime) {
        this.scheduledDateTime = scheduledDateTime;
    }

    public String getInterviewType() {
        return interviewType;
    }

    public void setInterviewType(String interviewType) {
        this.interviewType = interviewType;
    }

    public String getMeetingLink() {
        return meetingLink;
    }

    public void setMeetingLink(String meetingLink) {
        this.meetingLink = meetingLink;
    }

    public InterviewStatus getStatus() {
        return status;
    }

    public void setStatus(InterviewStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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

    @Override
    public String toString() {
        return "Interview{" +
                "id=" + id +
                ", applicationId=" + (application != null ? application.getId() : null) +
                ", scheduledDateTime=" + scheduledDateTime +
                ", interviewType='" + interviewType + '\'' +
                ", meetingLink='" + meetingLink + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
