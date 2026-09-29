package spring.eshwar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import spring.eshwar.entity.Candidate;
import spring.eshwar.entity.Resume;

import java.util.List;
import java.util.Optional;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

    Optional<Resume> findByCandidate(Candidate candidate);

    List<Resume> findAllByCandidateId(Long candidateId);

    Optional<Resume> findByCandidateId(Long candidateId);

    long countByCandidateId(Long candidateId);
}
