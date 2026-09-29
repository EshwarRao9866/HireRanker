package spring.eshwar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import spring.eshwar.entity.Candidate;
import spring.eshwar.entity.User;

import java.util.List;
import java.util.Optional;

public interface CandidateRepository extends JpaRepository<Candidate, Long> {

    Optional<Candidate> findByUser(User user);

    Optional<Candidate> findByUserId(Long userId);

    boolean existsByUser(User user);

    boolean existsByUserId(Long userId);

    Optional<Candidate> findByUserEmail(String email);

    @Query("SELECT c.skills FROM Candidate c WHERE c.skills IS NOT NULL")
    List<String> findAllCandidateSkills();
}
