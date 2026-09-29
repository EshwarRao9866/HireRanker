package spring.eshwar.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spring.eshwar.dto.CandidateProfileDto;
import spring.eshwar.dto.candidate.CandidateDashboardResponse;
import spring.eshwar.dto.candidate.CandidateProfileUpdateRequest;
import spring.eshwar.dto.candidate.CandidateRequest;
import spring.eshwar.dto.candidate.CandidateResponse;
import spring.eshwar.entity.Candidate;
import spring.eshwar.service.CandidateDashboardService;
import spring.eshwar.service.CandidateService;

import java.util.List;

@RestController
@RequestMapping("/api/candidates")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201", "http://localhost:4202", "http://127.0.0.1:4200", "http://127.0.0.1:4201"}, allowCredentials = "true")
public class CandidateController {

    private final CandidateService candidateService;
    private final CandidateDashboardService candidateDashboardService;

    public CandidateController(CandidateService candidateService,
                               CandidateDashboardService candidateDashboardService) {
        this.candidateService = candidateService;
        this.candidateDashboardService = candidateDashboardService;
    }

    /**
     * Retrieves the profile of the authenticated candidate.
     * Identity is strictly resolved from JWT authentication.
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<CandidateResponse> getMyProfile(Authentication authentication) {
        return ResponseEntity.ok(candidateService.getCandidateProfileByEmail(authentication.getName()));
    }

    /**
     * Updates the profile of the authenticated candidate.
     * Validates input fields and prevents modifying role or sensitive fields.
     */
    @PutMapping("/me")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<CandidateResponse> updateMyProfile(Authentication authentication,
                                                             @Valid @RequestBody CandidateProfileUpdateRequest request) {
        return ResponseEntity.ok(candidateService.updateCandidateProfile(authentication.getName(), request));
    }

    /**
     * Retrieves the personalized candidate dashboard for the authenticated candidate.
     * Identity is strictly resolved from JWT authentication.
     */
    @GetMapping("/me/dashboard")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<CandidateDashboardResponse> getMyDashboard(Authentication authentication) {
        return ResponseEntity.ok(candidateDashboardService.getCandidateDashboard(authentication.getName()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'ADMIN')")
    public ResponseEntity<CandidateResponse> getCandidateById(@PathVariable Long id, Authentication authentication) {
        validateCandidateAccess(id, authentication);
        return ResponseEntity.ok(candidateService.getCandidateResponseById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'ADMIN')")
    public ResponseEntity<CandidateResponse> updateCandidate(@PathVariable Long id,
                                                             @Valid @RequestBody CandidateRequest request,
                                                             Authentication authentication) {
        validateCandidateAccess(id, authentication);
        Candidate candidate = candidateService.getCandidateById(id);
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            candidate.setFullName(request.getFullName().trim());
            if (candidate.getUser() != null) {
                candidate.getUser().setName(request.getFullName().trim());
            }
        }
        if (request.getPhone() != null) candidate.setPhone(request.getPhone());
        if (request.getLocation() != null) candidate.setLocation(request.getLocation());
        if (request.getSkills() != null) candidate.setSkills(request.getSkills());
        if (request.getExperience() != null) candidate.setExperience(request.getExperience());
        if (request.getEducation() != null) candidate.setEducation(request.getEducation());
        if (request.getGithub() != null) candidate.setGithub(request.getGithub());
        if (request.getLinkedin() != null) candidate.setLinkedin(request.getLinkedin());

        Candidate updated = candidateService.updateCandidate(id, candidate);
        return ResponseEntity.ok(CandidateResponse.fromEntity(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCandidate(@PathVariable Long id) {
        candidateService.deleteCandidate(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/profile/{userId}")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'ADMIN')")
    public ResponseEntity<CandidateProfileDto> getProfile(@PathVariable Long userId, Authentication authentication) {
        validateUserAccess(userId, authentication);
        return candidateService.getProfileByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/profile/{userId}")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'ADMIN')")
    public ResponseEntity<CandidateProfileDto> updateProfile(@PathVariable Long userId,
                                                             @RequestBody CandidateProfileDto dto,
                                                             Authentication authentication) {
        validateUserAccess(userId, authentication);
        CandidateProfileDto updated = candidateService.updateProfile(userId, dto);
        return ResponseEntity.ok(updated);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CandidateResponse>> getAllCandidates() {
        return ResponseEntity.ok(candidateService.getAllCandidateResponses());
    }

    private void validateCandidateAccess(Long candidateId, Authentication authentication) {
        if (authentication == null) {
            throw new AccessDeniedException("Unauthorized");
        }
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            Candidate currentCandidate = candidateService.getCandidateByUserEmail(authentication.getName());
            if (!currentCandidate.getId().equals(candidateId)) {
                throw new AccessDeniedException("Candidates can only access their own profile");
            }
        }
    }

    private void validateUserAccess(Long userId, Authentication authentication) {
        if (authentication == null) {
            throw new AccessDeniedException("Unauthorized");
        }
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            Candidate currentCandidate = candidateService.getCandidateByUserEmail(authentication.getName());
            if (currentCandidate.getUser() == null || !currentCandidate.getUser().getId().equals(userId)) {
                throw new AccessDeniedException("Candidates can only access their own profile");
            }
        }
    }
}
