package spring.eshwar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import spring.eshwar.entity.Job;
import spring.eshwar.entity.JobStatus;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByStatus(JobStatus status);

    List<Job> findAllByOrderByCreatedAtDesc();

    @Query("SELECT j.requiredSkills FROM Job j WHERE j.requiredSkills IS NOT NULL")
    List<String> findAllRequiredSkills();
}
