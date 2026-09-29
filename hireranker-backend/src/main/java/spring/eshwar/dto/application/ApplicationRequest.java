package spring.eshwar.dto.application;

import jakarta.validation.constraints.NotNull;

public class ApplicationRequest {

    @NotNull(message = "Candidate ID is required")
    private Long candidateId;

    @NotNull(message = "Job ID is required")
    private Long jobId;

    private Long resumeId;

    public ApplicationRequest() {
    }

    public ApplicationRequest(Long candidateId, Long jobId, Long resumeId) {
        this.candidateId = candidateId;
        this.jobId = jobId;
        this.resumeId = resumeId;
    }

    public Long getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(Long candidateId) {
        this.candidateId = candidateId;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Long getResumeId() {
        return resumeId;
    }

    public void setResumeId(Long resumeId) {
        this.resumeId = resumeId;
    }
}
