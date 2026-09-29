package spring.eshwar.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spring.eshwar.dto.application.ShortlistedCandidateResponse;
import spring.eshwar.dto.job.JobRequest;
import spring.eshwar.dto.job.JobResponse;
import spring.eshwar.entity.Job;
import spring.eshwar.service.ApplicationService;
import spring.eshwar.service.JobService;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201", "http://localhost:4202", "http://127.0.0.1:4200", "http://127.0.0.1:4201"}, allowCredentials = "true")
public class JobController {

    private final JobService jobService;
    private final ApplicationService applicationService;

    public JobController(JobService jobService, ApplicationService applicationService) {
        this.jobService = jobService;
        this.applicationService = applicationService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody JobRequest request) {
        Job job = new Job();
        job.setTitle(request.getTitle().trim());
        job.setCompany(request.getCompany().trim());
        job.setDepartment(request.getDepartment() != null ? request.getDepartment().trim() : null);
        job.setDescription(request.getDescription() != null && !request.getDescription().isBlank()
                ? request.getDescription().trim()
                : (request.getTitle().trim() + " position at " + request.getCompany().trim()));
        job.setResponsibilities(request.getResponsibilities() != null && !request.getResponsibilities().isBlank()
                ? request.getResponsibilities().trim()
                : ("Core technical responsibilities for " + request.getTitle().trim() + " role."));
        job.setRequiredSkills(request.getRequiredSkills());
        job.setExperienceRequired(request.getExperienceRequired());
        job.setLocation(request.getLocation());
        job.setSalaryRange(request.getSalaryRange());
        job.setEmploymentType(request.getEmploymentType());
        if (request.getStatus() != null) {
            job.setStatus(request.getStatus());
        }

        Job saved = jobService.createJob(job);
        return ResponseEntity.status(HttpStatus.CREATED).body(JobResponse.fromEntity(saved));
    }

    @GetMapping
    public ResponseEntity<List<JobResponse>> getAllJobs() {
        List<JobResponse> jobs = jobService.getAllJobs().stream()
                .map(JobResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJobById(@PathVariable Long id) {
        Job job = jobService.getJobById(id);
        return ResponseEntity.ok(JobResponse.fromEntity(job));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JobResponse> updateJob(@PathVariable Long id,
                                                 @Valid @RequestBody JobRequest request) {
        Job jobDetails = new Job();
        jobDetails.setTitle(request.getTitle());
        jobDetails.setCompany(request.getCompany());
        jobDetails.setDepartment(request.getDepartment() != null ? request.getDepartment().trim() : null);
        jobDetails.setDescription(request.getDescription() != null && !request.getDescription().isBlank()
                ? request.getDescription().trim()
                : (request.getTitle().trim() + " position at " + request.getCompany().trim()));
        jobDetails.setResponsibilities(request.getResponsibilities() != null && !request.getResponsibilities().isBlank()
                ? request.getResponsibilities().trim()
                : ("Core technical responsibilities for " + request.getTitle().trim() + " role."));
        jobDetails.setRequiredSkills(request.getRequiredSkills());
        jobDetails.setExperienceRequired(request.getExperienceRequired());
        jobDetails.setLocation(request.getLocation());
        jobDetails.setSalaryRange(request.getSalaryRange());
        jobDetails.setEmploymentType(request.getEmploymentType());
        jobDetails.setStatus(request.getStatus());

        Job updated = jobService.updateJob(id, jobDetails);
        return ResponseEntity.ok(JobResponse.fromEntity(updated));
    }

    @GetMapping("/{jobId}/shortlisted")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ShortlistedCandidateResponse>> getShortlistedCandidatesForJob(@PathVariable("jobId") Long jobId) {
        return ResponseEntity.ok(applicationService.getShortlistedCandidatesByJob(jobId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id) {
        jobService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }
}
