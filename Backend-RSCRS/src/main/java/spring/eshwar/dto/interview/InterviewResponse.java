package spring.eshwar.dto.interview;

import spring.eshwar.entity.Interview;
import spring.eshwar.entity.InterviewStatus;

import java.time.LocalDateTime;

public class InterviewResponse {

    private Long id;
    private Long applicationId;
    private String candidateName;
    private String jobTitle;
    private LocalDateTime scheduledDateTime;
    private String interviewType;
    private String meetingLink;
    private InterviewStatus status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public InterviewResponse() {
    }

    public InterviewResponse(Long id, Long applicationId, String candidateName, String jobTitle,
                             LocalDateTime scheduledDateTime, String interviewType,
                             String meetingLink, InterviewStatus status, String notes,
                             LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.applicationId = applicationId;
        this.candidateName = candidateName;
        this.jobTitle = jobTitle;
        this.scheduledDateTime = scheduledDateTime;
        this.interviewType = interviewType;
        this.meetingLink = meetingLink;
        this.status = status;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static InterviewResponse fromEntity(Interview interview) {
        if (interview == null) {
            return null;
        }

        Long applicationId = null;
        String candidateName = null;
        String jobTitle = null;
        try {
            if (interview.getApplication() != null) {
                applicationId = interview.getApplication().getId();
                if (interview.getApplication().getCandidate() != null) {
                    candidateName = interview.getApplication().getCandidate().getFullName();
                }
                if (interview.getApplication().getJob() != null) {
                    jobTitle = interview.getApplication().getJob().getTitle();
                }
            }
        } catch (Exception ignored) {
        }

        return new InterviewResponse(
                interview.getId(),
                applicationId,
                candidateName,
                jobTitle,
                interview.getScheduledDateTime(),
                interview.getInterviewType(),
                interview.getMeetingLink(),
                interview.getStatus(),
                interview.getNotes(),
                interview.getCreatedAt(),
                interview.getUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
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

    public String getType() {
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
}
