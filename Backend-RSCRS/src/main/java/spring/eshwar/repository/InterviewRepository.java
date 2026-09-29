package spring.eshwar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import spring.eshwar.entity.Application;
import spring.eshwar.entity.Candidate;
import spring.eshwar.entity.Interview;
import spring.eshwar.entity.InterviewStatus;

import java.util.List;
import java.util.Optional;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    List<Interview> findByApplication(Application application);

    List<Interview> findByApplicationId(Long applicationId);

    List<Interview> findByApplicationCandidate(Candidate candidate);

    List<Interview> findByApplicationCandidateId(Long candidateId);

    List<Interview> findByStatus(InterviewStatus status);

    List<Interview> findAllByOrderByScheduledDateTimeAsc();

    List<Interview> findByApplicationIdAndStatus(Long applicationId, InterviewStatus status);

    Optional<Interview> findFirstByApplicationIdAndStatus(Long applicationId, InterviewStatus status);

    Optional<Interview> findFirstByApplicationIdOrderByCreatedAtDesc(Long applicationId);

    Optional<Interview> findFirstByApplicationIdOrderByIdDesc(Long applicationId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"application", "application.candidate", "application.job", "application.resume"})
    List<Interview> findByApplicationCandidateIdOrderByScheduledDateTimeAsc(Long candidateId);

    @org.springframework.data.jpa.repository.Query("SELECT i FROM Interview i " +
           "LEFT JOIN FETCH i.application a " +
           "LEFT JOIN FETCH a.candidate c " +
           "LEFT JOIN FETCH a.job j " +
           "LEFT JOIN FETCH a.resume r " +
           "WHERE i.id = :id")
    Optional<Interview> findByIdWithDetails(@org.springframework.data.repository.query.Param("id") Long id);

    void deleteByApplicationId(Long applicationId);
}
