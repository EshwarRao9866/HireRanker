package spring.eshwar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring.eshwar.entity.InterviewIntegrityEvent;

import java.util.List;

@Repository
public interface InterviewIntegrityEventRepository extends JpaRepository<InterviewIntegrityEvent, Long> {
    List<InterviewIntegrityEvent> findByInterviewIdOrderByCreatedAtAsc(Long interviewId);
    long countByInterviewId(Long interviewId);
    long countByInterviewIdAndEventType(Long interviewId, String eventType);
    void deleteByInterviewId(Long interviewId);
}
