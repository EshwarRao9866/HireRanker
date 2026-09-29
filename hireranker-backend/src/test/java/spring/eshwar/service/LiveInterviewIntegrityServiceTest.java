package spring.eshwar.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import spring.eshwar.dto.interview.RecordIntegrityEventsRequest;
import spring.eshwar.entity.*;
import spring.eshwar.repository.*;
import spring.eshwar.service.AiInterviewService.GeneratedInterviewResultDto;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LiveInterviewIntegrityServiceTest {

    private ApplicationRepository applicationRepository;
    private InterviewRepository interviewRepository;
    private InterviewQuestionRepository interviewQuestionRepository;
    private InterviewAnswerRepository interviewAnswerRepository;
    private InterviewResultRepository interviewResultRepository;
    private InterviewIntegrityEventRepository interviewIntegrityEventRepository;
    private AiInterviewService aiInterviewService;

    private LiveInterviewService liveInterviewService;

    @BeforeEach
    void setUp() {
        applicationRepository = mock(ApplicationRepository.class);
        interviewRepository = mock(InterviewRepository.class);
        interviewQuestionRepository = mock(InterviewQuestionRepository.class);
        interviewAnswerRepository = mock(InterviewAnswerRepository.class);
        interviewResultRepository = mock(InterviewResultRepository.class);
        interviewIntegrityEventRepository = mock(InterviewIntegrityEventRepository.class);
        aiInterviewService = mock(AiInterviewService.class);

        liveInterviewService = new LiveInterviewService(
                applicationRepository,
                interviewRepository,
                interviewQuestionRepository,
                interviewAnswerRepository,
                interviewResultRepository,
                interviewIntegrityEventRepository,
                aiInterviewService
        );
    }

    @Test
    @DisplayName("Integrity Test: Batch record integrity events saves events correctly")
    void testRecordIntegrityEvents() {
        Interview interview = new Interview();
        interview.setId(10L);

        when(interviewRepository.findById(10L)).thenReturn(Optional.of(interview));
        when(interviewIntegrityEventRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        RecordIntegrityEventsRequest.IntegrityEventItem item1 = new RecordIntegrityEventsRequest.IntegrityEventItem();
        item1.setEventType("MULTIPLE_PERSON");
        item1.setSeverity("HIGH");
        item1.setDurationSeconds(4.5);
        item1.setMessage("2 faces detected in frame");

        RecordIntegrityEventsRequest.IntegrityEventItem item2 = new RecordIntegrityEventsRequest.IntegrityEventItem();
        item2.setEventType("TAB_SWITCH");
        item2.setSeverity("LOW");
        item2.setDurationSeconds(2.0);
        item2.setMessage("Window lost focus");

        RecordIntegrityEventsRequest request = new RecordIntegrityEventsRequest(List.of(item1, item2));

        var saved = liveInterviewService.recordIntegrityEvents(10L, request);

        assertEquals(2, saved.size());
        assertEquals("MULTIPLE_PERSON", saved.get(0).getEventType());
        assertEquals("HIGH", saved.get(0).getSeverity());
        assertEquals("TAB_SWITCH", saved.get(1).getEventType());
    }

    @Test
    @DisplayName("Integrity Test: Complete interview aggregates integrity counts without altering technical score")
    void testCompleteInterviewWithIntegrityAggregation() {
        Interview interview = new Interview();
        interview.setId(10L);
        interview.setStatus(InterviewStatus.IN_PROGRESS);

        Application application = new Application();
        application.setId(5L);
        interview.setApplication(application);

        when(interviewRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(interview));
        when(interviewQuestionRepository.findByInterviewIdOrderByQuestionOrderAsc(10L)).thenReturn(List.of());
        when(interviewAnswerRepository.findByQuestionInterviewIdOrderByAnsweredAtAsc(10L)).thenReturn(List.of());

        // Mock synthesis result
        GeneratedInterviewResultDto synthesis = new GeneratedInterviewResultDto(
                85.0, 80.0, 82.0, 85.0, 80.0, 82.5,
                "Strong Core Java", "Improve concurrency", "Lock-free structures",
                "STRONG_HIRE", "Well-rounded performance"
        );
        when(aiInterviewService.synthesizeFinalResult(any(), any(), any(), any(), anyDouble())).thenReturn(synthesis);

        // Mock integrity events containing 1 MULTIPLE_PERSON
        InterviewIntegrityEvent ev1 = new InterviewIntegrityEvent(interview, "MULTIPLE_PERSON", "HIGH", 1000L, 5000L, 4.0, "2 faces");
        when(interviewIntegrityEventRepository.findByInterviewIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(ev1));

        when(interviewResultRepository.save(any(InterviewResult.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = liveInterviewService.completeInterview(10L);

        // Technical score must remain untouched
        assertEquals(85.0, response.getTechnicalScore());
        assertEquals(82.5, response.getOverallScore());

        // Integrity status must reflect REVIEW_REQUIRED due to MULTIPLE_PERSON
        assertEquals("REVIEW_REQUIRED", response.getIntegrityStatus());
        assertEquals(1, response.getTotalIntegrityEvents());
        assertEquals(1, response.getMultiplePersonEvents());
        assertEquals(0, response.getTabSwitchEvents());
    }
}
