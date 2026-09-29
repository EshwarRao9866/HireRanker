package spring.eshwar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import spring.eshwar.entity.InterviewResult;

import java.util.Optional;

public interface InterviewResultRepository extends JpaRepository<InterviewResult, Long> {

    Optional<InterviewResult> findByInterviewId(Long interviewId);

    boolean existsByInterviewId(Long interviewId);

    void deleteByInterviewId(Long interviewId);
}
