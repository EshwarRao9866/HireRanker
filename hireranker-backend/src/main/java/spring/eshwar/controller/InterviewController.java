package spring.eshwar.controller;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import spring.eshwar.dto.interview.InterviewRequest;
import spring.eshwar.dto.interview.InterviewResponse;
import spring.eshwar.entity.Application;
import spring.eshwar.entity.Candidate;
import spring.eshwar.entity.Interview;
import spring.eshwar.entity.InterviewStatus;
import spring.eshwar.exception.BadRequestException;
import spring.eshwar.exception.UnauthorizedException;
import spring.eshwar.service.ApplicationService;
import spring.eshwar.service.CandidateService;
import spring.eshwar.service.InterviewService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/interviews")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201", "http://localhost:4202", "http://127.0.0.1:4200", "http://127.0.0.1:4201"}, allowCredentials = "true")
public class InterviewController {

    private final InterviewService interviewService;
    private final ApplicationService applicationService;
    private final CandidateService candidateService;

    public InterviewController(InterviewService interviewService,
                               ApplicationService applicationService,
                               CandidateService candidateService) {
        this.interviewService = interviewService;
        this.applicationService = applicationService;
        this.candidateService = candidateService;
    }

    /**
     * Schedules an interview for a candidate application.
     * Restricted strictly to ADMIN users.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InterviewResponse> scheduleInterview(@Valid @RequestBody InterviewRequest request) {
        Application application = applicationService.getApplicationById(request.getApplicationId());

        Interview interview = new Interview();
        interview.setApplication(application);
        interview.setScheduledDateTime(request.getScheduledDateTime());
        interview.setInterviewType(request.getInterviewType());
        interview.setMeetingLink(request.getMeetingLink());
        interview.setNotes(request.getNotes());
        if (request.getStatus() != null) {
            interview.setStatus(request.getStatus());
        }

        Interview saved = interviewService.scheduleInterview(interview);
        return ResponseEntity.status(HttpStatus.CREATED).body(InterviewResponse.fromEntity(saved));
    }

    /**
     * Retrieves an interview by its ID.
     * Candidates can only access their own interviews.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'ADMIN')")
    public ResponseEntity<InterviewResponse> getInterviewById(@PathVariable Long id, Authentication authentication) {
        Interview interview = interviewService.getInterviewById(id);
        verifyCandidateOwnership(interview, authentication);
        return ResponseEntity.ok(InterviewResponse.fromEntity(interview));
    }

    /**
     * Retrieves all interviews scheduled for a candidate.
     * Candidates can only view their own interviews.
     */
    @GetMapping("/candidate/{candidateId}")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'ADMIN')")
    public ResponseEntity<List<InterviewResponse>> getInterviewsByCandidateId(@PathVariable Long candidateId,
                                                                             Authentication authentication) {
        verifyCandidateIdAccess(candidateId, authentication);
        List<InterviewResponse> list = interviewService.getInterviewsByCandidateId(candidateId).stream()
                .map(InterviewResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(list);
    }

    /**
     * Retrieves all interviews for an application.
     */
    @GetMapping("/application/{applicationId}")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'ADMIN')")
    public ResponseEntity<List<InterviewResponse>> getInterviewsByApplicationId(@PathVariable Long applicationId,
                                                                               Authentication authentication) {
        Application app = applicationService.getApplicationById(applicationId);
        verifyApplicationAccess(app, authentication);

        List<InterviewResponse> list = interviewService.getInterviewsByApplicationId(applicationId).stream()
                .map(InterviewResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(list);
    }

    /**
     * Retrieves all interviews ordered by scheduled date (Admin only).
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<InterviewResponse>> getAllInterviews() {
        List<InterviewResponse> list = interviewService.getAllInterviewsSortedByDate().stream()
                .map(InterviewResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(list);
    }

    /**
     * Updates interview details.
     * Restricted strictly to ADMIN users.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InterviewResponse> updateInterview(@PathVariable Long id,
                                                             @RequestBody InterviewRequest request) {
        Interview update = new Interview();
        update.setScheduledDateTime(request.getScheduledDateTime());
        update.setInterviewType(request.getInterviewType());
        update.setMeetingLink(request.getMeetingLink());
        update.setNotes(request.getNotes());
        if (request.getStatus() != null) {
            update.setStatus(request.getStatus());
        }

        Interview updated = interviewService.updateInterview(id, update);
        return ResponseEntity.ok(InterviewResponse.fromEntity(updated));
    }

    /**
     * Cancels an interview.
     * Restricted strictly to ADMIN users.
     */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InterviewResponse> cancelInterview(@PathVariable Long id,
                                                             @RequestParam(value = "reason", required = false) String reason,
                                                             @RequestBody(required = false) Map<String, String> body) {
        String cancelReason = reason;
        if ((cancelReason == null || cancelReason.isBlank()) && body != null && body.containsKey("reason")) {
            cancelReason = body.get("reason");
        }

        Interview cancelled = interviewService.cancelInterview(id, cancelReason);
        return ResponseEntity.ok(InterviewResponse.fromEntity(cancelled));
    }

    /**
     * Reschedules an interview to a new date/time.
     * Restricted strictly to ADMIN users.
     */
    @PutMapping("/{id}/reschedule")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InterviewResponse> rescheduleInterview(@PathVariable Long id,
                                                                 @RequestBody(required = false) InterviewRequest request,
                                                                 @RequestParam(value = "newDateTime", required = false)
                                                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime newDateTimeParam) {
        LocalDateTime scheduledTime = (request != null && request.getScheduledDateTime() != null)
                ? request.getScheduledDateTime() : newDateTimeParam;
        if (scheduledTime == null) {
            throw new BadRequestException("New scheduled date and time is required.");
        }

        String meetingLink = (request != null) ? request.getMeetingLink() : null;
        String notes = (request != null) ? request.getNotes() : null;

        Interview rescheduled = interviewService.rescheduleInterview(id, scheduledTime, meetingLink, notes);
        return ResponseEntity.ok(InterviewResponse.fromEntity(rescheduled));
    }

    /**
     * Marks an interview as completed.
     * Restricted strictly to ADMIN users.
     */
    @PutMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InterviewResponse> completeInterview(@PathVariable Long id) {
        Interview completed = interviewService.updateInterviewStatus(id, InterviewStatus.COMPLETED);
        return ResponseEntity.ok(InterviewResponse.fromEntity(completed));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteInterview(@PathVariable Long id) {
        interviewService.deleteInterview(id);
        return ResponseEntity.noContent().build();
    }

    // --- Helper Security Checks ---

    private void verifyCandidateOwnership(Interview interview, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Authentication is required.");
        }
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            Candidate currentCandidate = candidateService.getCandidateByUserEmail(authentication.getName());
            if (currentCandidate == null || interview.getApplication() == null
                    || interview.getApplication().getCandidate() == null
                    || !interview.getApplication().getCandidate().getId().equals(currentCandidate.getId())) {
                throw new AccessDeniedException("You are not authorized to view or modify another candidate's interview.");
            }
        }
    }

    private void verifyCandidateIdAccess(Long candidateId, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Authentication is required.");
        }
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            Candidate currentCandidate = candidateService.getCandidateByUserEmail(authentication.getName());
            if (currentCandidate == null || !currentCandidate.getId().equals(candidateId)) {
                throw new AccessDeniedException("You are not authorized to view another candidate's interviews.");
            }
        }
    }

    private void verifyApplicationAccess(Application app, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Authentication is required.");
        }
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            Candidate currentCandidate = candidateService.getCandidateByUserEmail(authentication.getName());
            if (currentCandidate == null || app.getCandidate() == null
                    || !app.getCandidate().getId().equals(currentCandidate.getId())) {
                throw new AccessDeniedException("You are not authorized to access interviews for this application.");
            }
        }
    }
}
