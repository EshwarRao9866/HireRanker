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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "applications", uniqueConstraints = {
        @UniqueConstraint(name = "uk_candidate_job", columnNames = {"candidate_id", "job_id"})
})
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Candidate is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", referencedColumnName = "id", nullable = false)
    private Candidate candidate;

    @NotNull(message = "Job is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", referencedColumnName = "id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", referencedColumnName = "id")
    private Resume resume;

    @NotNull(message = "Application status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    @CreationTimestamp
    @Column(name = "applied_at", nullable = false, updatable = false)
    private LocalDateTime appliedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Application() {
    }

    public Application(Candidate candidate, Job job, Resume resume) {
        this.candidate = candidate;
        this.job = job;
        this.resume = resume;
        this.status = ApplicationStatus.APPLIED;
    }

    public Application(Candidate candidate, Job job, Resume resume, ApplicationStatus status) {
        this.candidate = candidate;
        this.job = job;
        this.resume = resume;
        this.status = status != null ? status : ApplicationStatus.APPLIED;
    }

    public Application(Long id, Candidate candidate, Job job, Resume resume, ApplicationStatus status,
                       LocalDateTime appliedAt, LocalDateTime updatedAt) {
        this.id = id;
        this.candidate = candidate;
        this.job = job;
        this.resume = resume;
        this.status = status != null ? status : ApplicationStatus.APPLIED;
        this.appliedAt = appliedAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.appliedAt == null) {
            this.appliedAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = ApplicationStatus.APPLIED;
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

    public Candidate getCandidate() {
        return candidate;
    }

    public void setCandidate(Candidate candidate) {
        this.candidate = candidate;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public Resume getResume() {
        return resume;
    }

    public void setResume(Resume resume) {
        this.resume = resume;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Application{" +
                "id=" + id +
                ", candidateId=" + (candidate != null ? candidate.getId() : null) +
                ", jobId=" + (job != null ? job.getId() : null) +
                ", resumeId=" + (resume != null ? resume.getId() : null) +
                ", status=" + status +
                ", appliedAt=" + appliedAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
