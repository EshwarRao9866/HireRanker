package spring.eshwar.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "interview_integrity_events")
public class InterviewIntegrityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Interview is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", nullable = false)
    private Interview interview;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType; // MULTIPLE_PERSON, ATTENTION_AWAY, TAB_SWITCH, WINDOW_BLUR, FULLSCREEN_EXIT, AUDIO_ANOMALY

    @Column(name = "severity", nullable = false, length = 20)
    private String severity; // LOW, MEDIUM, HIGH

    @Column(name = "start_time")
    private Long startTime; // Milliseconds or epoch seconds

    @Column(name = "end_time")
    private Long endTime;

    @Column(name = "duration_seconds")
    private Double durationSeconds;

    @Column(name = "message", length = 500)
    private String message;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public InterviewIntegrityEvent() {
    }

    public InterviewIntegrityEvent(Interview interview, String eventType, String severity,
                                  Long startTime, Long endTime, Double durationSeconds, String message) {
        this.interview = interview;
        this.eventType = eventType;
        this.severity = severity;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationSeconds = durationSeconds;
        this.message = message;
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

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public Double getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Double durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
