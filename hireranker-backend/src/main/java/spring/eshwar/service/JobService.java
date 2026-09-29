package spring.eshwar.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.eshwar.entity.Application;
import spring.eshwar.entity.Job;
import spring.eshwar.entity.JobStatus;
import spring.eshwar.exception.ResourceNotFoundException;
import spring.eshwar.repository.ApplicationRepository;
import spring.eshwar.repository.EvaluationCriteriaRepository;
import spring.eshwar.repository.JobRepository;

import java.util.List;

@Service
public class JobService {

    private static final Logger logger = LoggerFactory.getLogger(JobService.class);

    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationService applicationService;
    private final EvaluationCriteriaRepository evaluationCriteriaRepository;

    public JobService(JobRepository jobRepository,
                      ApplicationRepository applicationRepository,
                      ApplicationService applicationService,
                      EvaluationCriteriaRepository evaluationCriteriaRepository) {
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.applicationService = applicationService;
        this.evaluationCriteriaRepository = evaluationCriteriaRepository;
    }

    @Transactional(readOnly = true)
    public Job getJobById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", id));
    }

    @Transactional(readOnly = true)
    public List<Job> getAllJobs() {
        return jobRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Job> getJobsByStatus(JobStatus status) {
        return jobRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<Job> getActiveJobs() {
        return jobRepository.findByStatus(JobStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return jobRepository.existsById(id);
    }

    @Transactional
    public Job createJob(Job job) {
        if (job.getTitle() == null || job.getTitle().isBlank()) {
            throw new IllegalArgumentException("Job title cannot be blank");
        }
        if (job.getCompany() == null || job.getCompany().isBlank()) {
            throw new IllegalArgumentException("Company name cannot be blank");
        }
        if (job.getStatus() == null) {
            job.setStatus(JobStatus.ACTIVE);
        }
        return jobRepository.save(job);
    }

    @Transactional
    public Job updateJob(Long id, Job jobDetails) {
        Job existing = getJobById(id);

        if (jobDetails.getTitle() != null && !jobDetails.getTitle().isBlank()) {
            existing.setTitle(jobDetails.getTitle().trim());
        }
        if (jobDetails.getCompany() != null && !jobDetails.getCompany().isBlank()) {
            existing.setCompany(jobDetails.getCompany().trim());
        }
        if (jobDetails.getDepartment() != null) {
            existing.setDepartment(jobDetails.getDepartment().trim());
        }
        if (jobDetails.getDescription() != null) {
            existing.setDescription(jobDetails.getDescription());
        }
        if (jobDetails.getResponsibilities() != null) {
            existing.setResponsibilities(jobDetails.getResponsibilities());
        }
        if (jobDetails.getRequiredSkills() != null) {
            existing.setRequiredSkills(jobDetails.getRequiredSkills());
        }
        if (jobDetails.getExperienceRequired() != null) {
            existing.setExperienceRequired(jobDetails.getExperienceRequired());
        }
        if (jobDetails.getLocation() != null) {
            existing.setLocation(jobDetails.getLocation());
        }
        if (jobDetails.getSalaryRange() != null) {
            existing.setSalaryRange(jobDetails.getSalaryRange());
        }
        if (jobDetails.getEmploymentType() != null) {
            existing.setEmploymentType(jobDetails.getEmploymentType());
        }
        if (jobDetails.getStatus() != null) {
            existing.setStatus(jobDetails.getStatus());
        }

        return jobRepository.save(existing);
    }

    @Transactional
    public Job updateJobStatus(Long id, JobStatus status) {
        Job existing = getJobById(id);
        existing.setStatus(status);
        return jobRepository.save(existing);
    }

    @Transactional
    public void deleteJob(Long id) {
        if (!jobRepository.existsById(id)) {
            throw new ResourceNotFoundException("Job", "id", id);
        }
        logger.info("Admin initiated deletion of job id: {}", id);

        // 1. Delete associated EvaluationCriteria if any
        try {
            evaluationCriteriaRepository.deleteByJobId(id);
        } catch (Exception e) {
            logger.warn("Could not delete evaluation criteria for job id {}: {}", id, e.getMessage());
        }

        // 2. Cascade delete all applications for this job
        List<Application> jobApps = applicationRepository.findByJobId(id);
        for (Application app : jobApps) {
            applicationService.deleteApplicationCascade(app.getId());
        }

        // 3. Delete the job entity
        jobRepository.deleteById(id);
        logger.info("Job id: {} and its {} associated application(s) deleted successfully.", id, jobApps.size());
    }
}
