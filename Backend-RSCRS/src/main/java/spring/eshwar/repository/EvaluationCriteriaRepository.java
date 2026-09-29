package spring.eshwar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import spring.eshwar.entity.EvaluationCriteria;
import spring.eshwar.entity.Job;

import java.util.Optional;

public interface EvaluationCriteriaRepository extends JpaRepository<EvaluationCriteria, Long> {

    Optional<EvaluationCriteria> findByJob(Job job);

    Optional<EvaluationCriteria> findByJobId(Long jobId);

    boolean existsByJob(Job job);

    boolean existsByJobId(Long jobId);

    void deleteByJobId(Long jobId);
}
