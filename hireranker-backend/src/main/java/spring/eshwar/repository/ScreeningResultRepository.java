package spring.eshwar.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import spring.eshwar.entity.Application;
import spring.eshwar.entity.ScreeningResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ScreeningResultRepository extends JpaRepository<ScreeningResult, Long> {

    Optional<ScreeningResult> findByApplication(Application application);

    Optional<ScreeningResult> findByApplicationId(Long applicationId);

    boolean existsByApplication(Application application);

    boolean existsByApplicationId(Long applicationId);

    @Query("SELECT AVG(sr.overallScore) FROM ScreeningResult sr")
    Double getAverageOverallScore();

    @Query("SELECT sr FROM ScreeningResult sr ORDER BY sr.overallScore DESC")
    List<ScreeningResult> findAllOrderByOverallScoreDesc();

    @EntityGraph(attributePaths = {"application", "application.candidate", "application.candidate.user", "application.job", "application.resume"})
    List<ScreeningResult> findTop5ByOrderByOverallScoreDesc();

    @Query("SELECT sr.screenedAt FROM ScreeningResult sr WHERE sr.screenedAt >= :since")
    List<LocalDateTime> findScreenedAtSince(@Param("since") LocalDateTime since);

    @Query("SELECT sr.matchingSkills FROM ScreeningResult sr WHERE sr.matchingSkills IS NOT NULL")
    List<String> findAllMatchingSkills();

    void deleteByApplicationId(Long applicationId);
}
