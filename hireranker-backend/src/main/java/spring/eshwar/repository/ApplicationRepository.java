package spring.eshwar.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import spring.eshwar.entity.Application;
import spring.eshwar.entity.ApplicationStatus;
import spring.eshwar.entity.Candidate;
import spring.eshwar.entity.Job;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    @Override
    @EntityGraph(attributePaths = {"candidate", "candidate.user", "job", "resume"})
    List<Application> findAll();

    @Override
    @EntityGraph(attributePaths = {"candidate", "candidate.user", "job", "resume"})
    Optional<Application> findById(Long id);

    List<Application> findByCandidate(Candidate candidate);

    List<Application> findByJob(Job job);

    @EntityGraph(attributePaths = {"candidate", "candidate.user", "job", "resume"})
    List<Application> findByCandidateId(Long candidateId);

    @EntityGraph(attributePaths = {"candidate", "candidate.user", "job", "resume"})
    List<Application> findByJobId(Long jobId);

    @EntityGraph(attributePaths = {"candidate", "candidate.user", "job", "resume"})
    List<Application> findByStatus(ApplicationStatus status);

    @EntityGraph(attributePaths = {"candidate", "candidate.user", "job", "resume"})
    List<Application> findByStatusIn(List<ApplicationStatus> statuses);

    List<Application> findByJobIdAndStatus(Long jobId, ApplicationStatus status);

    List<Application> findByJobIdAndStatusIn(Long jobId, List<ApplicationStatus> statuses);

    boolean existsByCandidateAndJob(Candidate candidate, Job job);

    boolean existsByCandidateIdAndJobId(Long candidateId, Long jobId);

    Optional<Application> findByCandidateAndJob(Candidate candidate, Job job);

    Optional<Application> findByCandidateIdAndJobId(Long candidateId, Long jobId);

    long countByStatus(ApplicationStatus status);

    long countByCandidateId(Long candidateId);

    long countByCandidateIdAndStatus(Long candidateId, ApplicationStatus status);

    @EntityGraph(attributePaths = {"candidate", "candidate.user", "job", "resume"})
    List<Application> findByCandidateIdOrderByAppliedAtDesc(Long candidateId);
 
    @Query("SELECT a.status, COUNT(a) FROM Application a GROUP BY a.status")
    List<Object[]> countGroupedByStatus();

    @Query("SELECT a.appliedAt FROM Application a WHERE a.appliedAt >= :since")
    List<LocalDateTime> findAppliedAtSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(DISTINCT a.candidate.id) FROM Application a")
    long countDistinctCandidates();
}
