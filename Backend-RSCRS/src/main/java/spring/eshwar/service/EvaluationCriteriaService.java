package spring.eshwar.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.eshwar.entity.EvaluationCriteria;
import spring.eshwar.entity.Job;
import spring.eshwar.exception.DuplicateResourceException;
import spring.eshwar.exception.ResourceNotFoundException;
import spring.eshwar.repository.EvaluationCriteriaRepository;
import spring.eshwar.repository.JobRepository;

@Service
public class EvaluationCriteriaService {

    private final EvaluationCriteriaRepository evaluationCriteriaRepository;
    private final JobRepository jobRepository;

    public EvaluationCriteriaService(EvaluationCriteriaRepository evaluationCriteriaRepository,
                                     JobRepository jobRepository) {
        this.evaluationCriteriaRepository = evaluationCriteriaRepository;
        this.jobRepository = jobRepository;
    }

    @Transactional(readOnly = true)
    public EvaluationCriteria getCriteriaById(Long id) {
        return evaluationCriteriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EvaluationCriteria", "id", id));
    }

    @Transactional(readOnly = true)
    public EvaluationCriteria getCriteriaByJobId(Long jobId) {
        return evaluationCriteriaRepository.findByJobId(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("EvaluationCriteria", "jobId", jobId));
    }

    @Transactional(readOnly = true)
    public boolean existsByJobId(Long jobId) {
        return evaluationCriteriaRepository.existsByJobId(jobId);
    }

    @Transactional
    public EvaluationCriteria createCriteria(EvaluationCriteria criteria) {
        if (criteria.getJob() == null || criteria.getJob().getId() == null) {
            throw new IllegalArgumentException("Evaluation criteria must be linked to a valid Job.");
        }

        Long jobId = criteria.getJob().getId();
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId));

        if (evaluationCriteriaRepository.existsByJobId(jobId)) {
            throw new DuplicateResourceException("Evaluation criteria already exists for job id: " + jobId);
        }

        if (!criteria.isTotalWeightValid()) {
            throw new IllegalArgumentException(
                    "Total weight must equal 100%. Current sum: " + criteria.getTotalWeight()
            );
        }

        criteria.setJob(job);
        return evaluationCriteriaRepository.save(criteria);
    }

    @Transactional
    public EvaluationCriteria updateCriteria(Long id, EvaluationCriteria updated) {
        EvaluationCriteria existing = getCriteriaById(id);

        if (updated.getRequiredSkills() != null) {
            existing.setRequiredSkills(updated.getRequiredSkills());
        }
        if (updated.getMinimumExperience() != null) {
            existing.setMinimumExperience(updated.getMinimumExperience());
        }
        if (updated.getEducationRequirements() != null) {
            existing.setEducationRequirements(updated.getEducationRequirements());
        }
        if (updated.getSkillWeight() != null) {
            existing.setSkillWeight(updated.getSkillWeight());
        }
        if (updated.getExperienceWeight() != null) {
            existing.setExperienceWeight(updated.getExperienceWeight());
        }
        if (updated.getEducationWeight() != null) {
            existing.setEducationWeight(updated.getEducationWeight());
        }

        if (!existing.isTotalWeightValid()) {
            throw new IllegalArgumentException(
                    "Total weight must equal 100%. Current sum: " + existing.getTotalWeight()
            );
        }

        return evaluationCriteriaRepository.save(existing);
    }

    @Transactional
    public void deleteCriteria(Long id) {
        if (!evaluationCriteriaRepository.existsById(id)) {
            throw new ResourceNotFoundException("EvaluationCriteria", "id", id);
        }
        evaluationCriteriaRepository.deleteById(id);
    }
}
