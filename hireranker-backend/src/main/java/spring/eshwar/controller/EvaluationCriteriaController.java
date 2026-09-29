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
import spring.eshwar.dto.evaluation.EvaluationCriteriaRequest;
import spring.eshwar.dto.evaluation.EvaluationCriteriaResponse;
import spring.eshwar.entity.EvaluationCriteria;
import spring.eshwar.entity.Job;
import spring.eshwar.service.EvaluationCriteriaService;
import spring.eshwar.service.JobService;

@RestController
@RequestMapping("/api/evaluation-criteria")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201", "http://localhost:4202", "http://127.0.0.1:4200", "http://127.0.0.1:4201"}, allowCredentials = "true")
public class EvaluationCriteriaController {

    private final EvaluationCriteriaService evaluationCriteriaService;
    private final JobService jobService;

    public EvaluationCriteriaController(EvaluationCriteriaService evaluationCriteriaService,
                                        JobService jobService) {
        this.evaluationCriteriaService = evaluationCriteriaService;
        this.jobService = jobService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EvaluationCriteriaResponse> createCriteria(@Valid @RequestBody EvaluationCriteriaRequest request) {
        Job job = jobService.getJobById(request.getJobId());

        EvaluationCriteria criteria = new EvaluationCriteria();
        criteria.setJob(job);
        criteria.setRequiredSkills(request.getRequiredSkills());
        criteria.setMinimumExperience(request.getMinimumExperience());
        criteria.setEducationRequirements(request.getEducationRequirements());
        criteria.setSkillWeight(request.getSkillWeight());
        criteria.setExperienceWeight(request.getExperienceWeight());
        criteria.setEducationWeight(request.getEducationWeight());

        EvaluationCriteria saved = evaluationCriteriaService.createCriteria(criteria);
        return ResponseEntity.status(HttpStatus.CREATED).body(EvaluationCriteriaResponse.fromEntity(saved));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'ADMIN')")
    public ResponseEntity<EvaluationCriteriaResponse> getCriteriaById(@PathVariable Long id) {
        EvaluationCriteria criteria = evaluationCriteriaService.getCriteriaById(id);
        return ResponseEntity.ok(EvaluationCriteriaResponse.fromEntity(criteria));
    }

    @GetMapping("/job/{jobId}")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'ADMIN')")
    public ResponseEntity<EvaluationCriteriaResponse> getCriteriaByJobId(@PathVariable Long jobId) {
        EvaluationCriteria criteria = evaluationCriteriaService.getCriteriaByJobId(jobId);
        return ResponseEntity.ok(EvaluationCriteriaResponse.fromEntity(criteria));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EvaluationCriteriaResponse> updateCriteria(@PathVariable Long id,
                                                                    @Valid @RequestBody EvaluationCriteriaRequest request) {
        EvaluationCriteria update = new EvaluationCriteria();
        update.setRequiredSkills(request.getRequiredSkills());
        update.setMinimumExperience(request.getMinimumExperience());
        update.setEducationRequirements(request.getEducationRequirements());
        update.setSkillWeight(request.getSkillWeight());
        update.setExperienceWeight(request.getExperienceWeight());
        update.setEducationWeight(request.getEducationWeight());

        EvaluationCriteria updated = evaluationCriteriaService.updateCriteria(id, update);
        return ResponseEntity.ok(EvaluationCriteriaResponse.fromEntity(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCriteria(@PathVariable Long id) {
        evaluationCriteriaService.deleteCriteria(id);
        return ResponseEntity.noContent().build();
    }
}
