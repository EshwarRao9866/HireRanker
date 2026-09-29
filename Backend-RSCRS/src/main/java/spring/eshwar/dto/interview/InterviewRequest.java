package spring.eshwar.dto.interview;

import jakarta.validation.constraints.NotNull;
import spring.eshwar.entity.InterviewStatus;

import java.time.LocalDateTime;

public class InterviewRequest {

    @NotNull(message = "Application ID is required")
    private Long applicationId;

    @NotNull(message = "Scheduled date and time is required")
    private LocalDateTime scheduledDateTime;

    private String interviewType;
    private String meetingLink;
    private String notes;
    private InterviewStatus status;

    public InterviewRequest() {
    }

    public InterviewRequest(Long applicationId, LocalDateTime scheduledDateTime,
                            String interviewType, String meetingLink, String notes,
                            InterviewStatus status) {
        this.applicationId = applicationId;
        this.scheduledDateTime = scheduledDateTime;
        this.interviewType = interviewType;
        this.meetingLink = meetingLink;
        this.notes = notes;
        this.status = status;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public InterviewStatus getStatus() {
        return status;
    }

    public void setStatus(InterviewStatus status) {
        this.status = status;
    }
}
