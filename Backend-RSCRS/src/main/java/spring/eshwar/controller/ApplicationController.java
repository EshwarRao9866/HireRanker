package spring.eshwar.controller;

import jakarta.validation.Valid;
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
import spring.eshwar.dto.application.ApplicationRequest;
import spring.eshwar.dto.application.ApplicationResponse;
import spring.eshwar.dto.application.ShortlistedCandidateResponse;
import spring.eshwar.entity.Application;
import spring.eshwar.entity.ApplicationStatus;
import spring.eshwar.entity.Candidate;
import spring.eshwar.entity.ScreeningResult;
import spring.eshwar.repository.ScreeningResultRepository;
import spring.eshwar.service.ApplicationService;
import spring.eshwar.service.CandidateService;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201", "http://localhost:4202", "http://127.0.0.1:4200", "http://127.0.0.1:4201"}, allowCredentials = "true")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final CandidateService candidateService;
    private final ScreeningResultRepository screeningResultRepository;

    public ApplicationController(ApplicationService applicationService,
                                 CandidateService candidateService,
                                 ScreeningResultRepository screeningResultRepository) {
        this.applicationService = applicationService;
        this.candidateService = candidateService;
        this.screeningResultRepository = screeningResultRepository;
    }

    @PostMapping("/applications")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'ADMIN')")
    public ResponseEntity<ApplicationResponse> applyToJob(@Valid @RequestBody ApplicationRequest request,
                                                         Authentication authentication) {
        Long candidateId = request.getCandidateId();
        if (authentication != null && !isAdmin(authentication)) {
            Candidate candidate = candidateService.getCandidateByUserEmail(authentication.getName());
            if (candidateId != null && !candidateId.equals(candidate.getId())) {
                throw new AccessDeniedException("Candidates can only apply on behalf of themselves.");
            }
            candidateId = candidate.getId();
        }

        Application application = applicationService.applyToJob(
                candidateId,
                request.getJobId(),
                request.getResumeId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApplicationResponse.fromEntity(application));
    }

    @GetMapping("/applications/shortlisted")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ShortlistedCandidateResponse>> getAllShortlistedCandidates() {
        List<ShortlistedCandidateResponse> list = applicationService.getAllShortlistedCandidates();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/applications/{id}")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'ADMIN')")
    public ResponseEntity<ApplicationResponse> getApplicationById(@PathVariable Long id,
                                                                 Authentication authentication) {
        Application application = applicationService.getApplicationById(id);
        if (authentication != null && !isAdmin(authentication)) {
            Candidate candidate = candidateService.getCandidateByUserEmail(authentication.getName());
            if (application.getCandidate() == null || !application.getCandidate().getId().equals(candidate.getId())) {
                throw new AccessDeniedException("You are not authorized to view another candidate's application.");
            }
        }
        return ResponseEntity.ok(ApplicationResponse.fromEntity(application));
    }

    @GetMapping("/applications/candidate/{candidateId}")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'ADMIN')")
    public ResponseEntity<List<ApplicationResponse>> getApplicationsByCandidate(@PathVariable Long candidateId,
                                                                               Authentication authentication) {
        if (authentication != null && !isAdmin(authentication)) {
            Candidate candidate = candidateService.getCandidateByUserEmail(authentication.getName());
            if (!candidate.getId().equals(candidateId)) {
                throw new AccessDeniedException("You are not authorized to view applications for another candidate.");
            }
        }

        List<ApplicationResponse> list = applicationService.getApplicationsByCandidate(candidateId).stream()
                .map(ApplicationResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/applications/job/{jobId}")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'ADMIN')")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<ApplicationResponse>> getApplicationsByJob(@PathVariable Long jobId) {
        List<ApplicationResponse> list = applicationService.getApplicationsByJob(jobId).stream()
                .map(ApplicationResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/applications")
    @PreAuthorize("hasRole('ADMIN')")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<ApplicationResponse>> getAllApplications() {
        List<ApplicationResponse> list = applicationService.getAllApplications().stream()
                .map(app -> {
                    ScreeningResult sr = screeningResultRepository.findByApplicationId(app.getId()).orElse(null);
                    return ApplicationResponse.fromEntity(app, sr);
                })
                .toList();
        return ResponseEntity.ok(list);
    }

    @PutMapping("/applications/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApplicationResponse> updateStatus(@PathVariable Long id,
                                                            @RequestParam ApplicationStatus status) {
        Application updated = applicationService.updateApplicationStatus(id, status);
        return ResponseEntity.ok(ApplicationResponse.fromEntity(updated));
    }

    @PutMapping("/applications/{applicationId}/shortlist")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApplicationResponse> shortlistApplication(@PathVariable Long applicationId) {
        Application updated = applicationService.shortlistApplication(applicationId);
        return ResponseEntity.ok(ApplicationResponse.fromEntity(updated));
    }

    @GetMapping("/applications/job/{jobId}/shortlisted")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ShortlistedCandidateResponse>> getShortlistedCandidatesByJob(@PathVariable Long jobId) {
        List<ShortlistedCandidateResponse> list = applicationService.getShortlistedCandidatesByJob(jobId);
        return ResponseEntity.ok(list);
    }


    @DeleteMapping("/applications/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteApplication(@PathVariable Long id) {
        applicationService.deleteApplication(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/applications/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAllApplications() {
        applicationService.deleteAllApplications();
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
