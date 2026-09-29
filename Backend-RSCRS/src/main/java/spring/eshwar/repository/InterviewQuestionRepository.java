package spring.eshwar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import spring.eshwar.entity.InterviewQuestion;
import spring.eshwar.entity.QuestionStatus;

import java.util.List;
import java.util.Optional;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {

    List<InterviewQuestion> findByInterviewIdOrderByQuestionOrderAsc(Long interviewId);

    List<InterviewQuestion> findByInterviewIdAndStatus(Long interviewId, QuestionStatus status);

    Optional<InterviewQuestion> findFirstByInterviewIdAndStatusOrderByQuestionOrderAsc(Long interviewId, QuestionStatus status);

    long countByInterviewId(Long interviewId);

    long countByInterviewIdAndStatus(Long interviewId, QuestionStatus status);

    void deleteByInterviewId(Long interviewId);
}
