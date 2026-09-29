package spring.eshwar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import spring.eshwar.entity.InterviewAnswer;

import java.util.List;
import java.util.Optional;

public interface InterviewAnswerRepository extends JpaRepository<InterviewAnswer, Long> {

    Optional<InterviewAnswer> findByQuestionId(Long questionId);

    List<InterviewAnswer> findByQuestionInterviewIdOrderByAnsweredAtAsc(Long interviewId);

    void deleteByQuestionId(Long questionId);

    void deleteByQuestionInterviewId(Long interviewId);
}
