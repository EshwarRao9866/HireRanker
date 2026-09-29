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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import spring.eshwar.dto.screening.ScreeningRequest;
import spring.eshwar.dto.screening.ScreeningResultResponse;
import spring.eshwar.entity.Application;
import spring.eshwar.entity.Candidate;
import spring.eshwar.entity.ScreeningResult;
import spring.eshwar.exception.UnauthorizedException;
import spring.eshwar.service.AIResumeScreeningService;
import spring.eshwar.service.ApplicationService;
import spring.eshwar.service.CandidateService;
import spring.eshwar.service.ScreeningService;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201", "http://localhost:4202", "http://127.0.0.1:4200", "http://127.0.0.1:4201"}, allowCredentials = "true")
public class ScreeningController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ScreeningController.class);

    private final ScreeningService screeningService;
    private final ApplicationService applicationService;
    private final AIResumeScreeningService aiResumeScreeningService;
    private final CandidateService candidateService;

    public ScreeningController(ScreeningService screeningService,
                               ApplicationService applicationService,
                               AIResumeScreeningService aiResumeScreeningService,
                               CandidateService candidateService) {
        this.screeningService = screeningService;
        this.applicationService = applicationService;
        this.aiResumeScreeningService = aiResumeScreeningService;
        this.candidateService = candidateService;
    }

    @PostMapping({"/applications/{applicationId}/screen", "/screenings/{applicationId}", "/screening/{applicationId}"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScreeningResultResponse> screenApplication(
            @PathVariable Long applicationId,
            @RequestParam(value = "force", defaultValue = "false") boolean force) {
        log.info("[ScreeningController] Received screenApplication request for applicationId: {}, force: {}", applicationId, force);
        try {
            ScreeningResultResponse response = aiResumeScreeningService.screenApplication(applicationId, force);
            return ResponseEntity.ok(response);
        } catch (spring.eshwar.exception.ResourceNotFoundException rnfe) {
            log.warn("[ScreeningController] Application {} not found for screening: {}", applicationId, rnfe.getMessage());
            // Return empty 204 or graceful null result so frontend dashboard pipeline doesn't crash
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping({"/applications/{applicationId}/screen", "/screenings/{applicationId}", "/screening/{applicationId}"})
    @PreAuthorize("hasAnyRole('CANDIDATE', 'ADMIN')")
    public ResponseEntity<ScreeningResultResponse> getOrScreenApplication(
            @PathVariable Long applicationId,
            @RequestParam(value = "force", defaultValue = "false") boolean force,
            Authentication authentication) {
        log.info("[ScreeningController] Received GET screening for applicationId: {}, force: {}", applicationId, force);
        try {
            if (force && isAdmin(authentication)) {
                return screenApplication(applicationId, true);
            }
            return getScreeningByApplicationId(applicationId, authentication);
        } catch (Exception e) {
            log.warn("[ScreeningController] Gracefully handling missing application/screening {}: {}", applicationId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping({"/applications/{applicationId}/screening", "/screening/application/{applicationId}", "/screenings/application/{applicationId}"})
    @PreAuthorize("hasAnyRole('CANDIDATE', 'ADMIN')")
    public ResponseEntity<ScreeningResultResponse> getScreeningByApplicationId(@PathVariable Long applicationId,
                                                                              Authentication authentication) {
        Application app = applicationService.getApplicationById(applicationId);
        verifyApplicationAccess(app, authentication);
        ScreeningResult result = screeningService.getScreeningResultByApplicationId(applicationId);
        return ResponseEntity.ok(ScreeningResultResponse.fromEntity(result));
    }

    @GetMapping({"/screening/{id}", "/screenings/{id}"})
    @PreAuthorize("hasAnyRole('CANDIDATE', 'ADMIN')")
    public ResponseEntity<ScreeningResultResponse> getScreeningById(@PathVariable Long id,
                                                                   Authentication authentication) {
        ScreeningResult result = screeningService.getScreeningResultById(id);
        if (result.getApplication() != null) {
            verifyApplicationAccess(result.getApplication(), authentication);
        }
        return ResponseEntity.ok(ScreeningResultResponse.fromEntity(result));
    }

    @PostMapping("/screening")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScreeningResultResponse> saveScreening(@Valid @RequestBody ScreeningRequest request) {
        Application application = applicationService.getApplicationById(request.getApplicationId());

        ScreeningResult result = new ScreeningResult();
        result.setApplication(application);
        result.setOverallScore(request.getOverallScore());
        result.setSkillsScore(request.getSkillsScore());
        result.setExperienceScore(request.getExperienceScore());
        result.setEducationScore(request.getEducationScore());
        result.setMatchingSkills(request.getMatchingSkills());
        result.setMissingSkills(request.getMissingSkills());
        result.setRecommendation(request.getRecommendation());

        ScreeningResult saved = screeningService.saveScreeningResult(result);
        return ResponseEntity.status(HttpStatus.CREATED).body(ScreeningResultResponse.fromEntity(saved));
    }

    @DeleteMapping("/screening/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteScreening(@PathVariable Long id) {
        screeningService.deleteScreeningResult(id);
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private void verifyApplicationAccess(Application app, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Authentication is required.");
        }
        if (!isAdmin(authentication)) {
            Candidate candidate = candidateService.getCandidateByUserEmail(authentication.getName());
            if (candidate == null || app.getCandidate() == null
                    || !app.getCandidate().getId().equals(candidate.getId())) {
                throw new AccessDeniedException("You are not authorized to view screening for another candidate.");
            }
        }
    }
}
