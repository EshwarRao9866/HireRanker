package spring.eshwar.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;
import spring.eshwar.dto.resume.ResumeRequest;
import spring.eshwar.dto.resume.ResumeResponse;
import spring.eshwar.entity.Candidate;
import spring.eshwar.entity.Resume;
import spring.eshwar.exception.UnauthorizedException;
import spring.eshwar.service.CandidateService;
import spring.eshwar.service.ResumeService;

import java.util.List;

@RestController
@RequestMapping("/api/resumes")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201", "http://localhost:4202", "http://127.0.0.1:4200", "http://127.0.0.1:4201"}, allowCredentials = "true")
public class ResumeController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ResumeController.class);

    private final ResumeService resumeService;
    private final CandidateService candidateService;

    public ResumeController(ResumeService resumeService, CandidateService candidateService) {
        this.resumeService = resumeService;
        this.candidateService = candidateService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ResumeResponse> uploadResume(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "candidateId", required = false) Long candidateId,
            @RequestParam(value = "targetRole", required = false) String targetRole,
            @RequestParam(value = "jobId", required = false) Long jobId,
            Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Authentication is required to upload a resume.");
        }

        String userEmail = authentication.getName();
        Candidate candidate = candidateService.getCandidateByUserEmail(userEmail);

        // Security check: If candidateId is supplied, it must match the authenticated candidate's ID
        if (candidateId != null && !candidate.getId().equals(candidateId)) {
            throw new AccessDeniedException("You are not authorized to upload a resume for another candidate.");
        }

        Resume savedResume = resumeService.uploadResume(file, candidate, targetRole, jobId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResumeResponse.fromEntity(savedResume));
    }

    @PostMapping("/my-resume/screen")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ResumeResponse> screenMyResume(
            @RequestParam(value = "targetRole", required = false) String targetRole,
            @RequestParam(value = "jobId", required = false) Long jobId,
            Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Authentication is required.");
        }
        Candidate candidate = candidateService.getOrCreateCandidateByUserEmail(authentication.getName());
        Resume resume = resumeService.getMyActiveResume(candidate);
        Resume screened = resumeService.screenCandidateResume(resume.getId(), targetRole, jobId, candidate);
        return ResponseEntity.ok(ResumeResponse.fromEntity(screened));
    }

    @PostMapping("/{id}/screen")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'ADMIN')")
    public ResponseEntity<ResumeResponse> screenResumeById(
            @PathVariable Long id,
            @RequestParam(value = "targetRole", required = false) String targetRole,
            @RequestParam(value = "jobId", required = false) Long jobId,
            Authentication authentication) {
        Resume existing = resumeService.getResumeById(id);
        verifyResumeAccess(existing, authentication);
        Candidate candidate = existing.getCandidate();
        Resume screened = resumeService.screenCandidateResume(id, targetRole, jobId, candidate);
        return ResponseEntity.ok(ResumeResponse.fromEntity(screened));
    }

    @GetMapping("/my-resume")
    @PreAuthorize("hasRole('CANDIDATE')")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<ResumeResponse> getMyResume(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Authentication is required.");
        }
        Candidate candidate = candidateService.getOrCreateCandidateByUserEmail(authentication.getName());
        Resume resume = resumeService.getMyActiveResume(candidate);
        return ResponseEntity.ok(ResumeResponse.fromEntity(resume));
    }

    @GetMapping(value = {"/my-resume/file", "/my-resume/download", "/my-resume/view"}, produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasRole('CANDIDATE')")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<org.springframework.core.io.Resource> getMyResumePdf(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Authentication is required.");
        }
        Candidate candidate = candidateService.getOrCreateCandidateByUserEmail(authentication.getName());
        Resume resume = resumeService.getMyActiveResume(candidate);
        org.springframework.core.io.Resource resource = resumeService.getResumeFileResource(resume.getId());
        String fileName = (resume.getFileName() != null && !resume.getFileName().isBlank())
                ? resume.getFileName()
                : "resume.pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(resource);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CANDIDATE', 'ADMIN')")
    public ResponseEntity<ResumeResponse> createResumeMetadata(@Valid @RequestBody ResumeRequest request,
                                                               Authentication authentication) {
        Long candidateId = request.getCandidateId();
        if (authentication != null && !isAdmin(authentication)) {
            Candidate currentCand = candidateService.getCandidateByUserEmail(authentication.getName());
            if (candidateId != null && !candidateId.equals(currentCand.getId())) {
                throw new AccessDeniedException("Candidates can only register resumes for themselves.");
            }
            candidateId = currentCand.getId();
        }
        Candidate candidate = candidateService.getCandidateById(candidateId);

        Resume resume = new Resume();
        resume.setCandidate(candidate);
        resume.setFileName(request.getFileName().trim());
        resume.setFilePath(request.getFilePath().trim());
        resume.setFileType(request.getFileType());
        resume.setFileSize(request.getFileSize());
        resume.setExtractedText(request.getExtractedText());

        Resume saved = resumeService.saveResumeMetadata(resume);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResumeResponse.fromEntity(saved));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'ADMIN')")
    public ResponseEntity<ResumeResponse> getResumeById(@PathVariable Long id, Authentication authentication) {
        Resume resume = resumeService.getResumeById(id);
        verifyResumeAccess(resume, authentication);
        return ResponseEntity.ok(ResumeResponse.fromEntity(resume));
    }

    @GetMapping(value = {"/{id}/file", "/{id}/download", "/{id}/view"}, produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('CANDIDATE', 'ADMIN')")
    public ResponseEntity<org.springframework.core.io.Resource> getResumePdf(@PathVariable Long id, Authentication authentication) {
        try {
            Resume resume = resumeService.getResumeById(id);
            verifyResumeAccess(resume, authentication);
            org.springframework.core.io.Resource resource = resumeService.getResumeFileResource(id);
            String fileName = (resume.getFileName() != null && !resume.getFileName().isBlank())
                    ? resume.getFileName()
                    : "resume.pdf";

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (spring.eshwar.exception.ResourceNotFoundException rnfe) {
            log.warn("Resume ID {} not found for download: {}", id, rnfe.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/candidate/{candidateId}")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'ADMIN')")
    public ResponseEntity<List<ResumeResponse>> getResumesByCandidateId(@PathVariable Long candidateId, Authentication authentication) {
        verifyCandidateIdAccess(candidateId, authentication);
        List<ResumeResponse> list = resumeService.getAllResumesByCandidateId(candidateId).stream()
                .map(ResumeResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping("/{id}/extract-text")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'ADMIN')")
    public ResponseEntity<ResumeResponse> extractText(@PathVariable Long id, Authentication authentication) {
        Resume existing = resumeService.getResumeById(id);
        verifyResumeAccess(existing, authentication);
        Resume resume = resumeService.extractTextForResume(id);
        return ResponseEntity.ok(ResumeResponse.fromEntity(resume));
    }

    @PostMapping("/{id}/analyze")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'ADMIN')")
    public ResponseEntity<ResumeResponse> analyzeResume(@PathVariable Long id, Authentication authentication) {
        Resume existing = resumeService.getResumeById(id);
        verifyResumeAccess(existing, authentication);
        Resume resume = resumeService.analyzeResume(id);
        return ResponseEntity.ok(ResumeResponse.fromEntity(resume));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'ADMIN')")
    public ResponseEntity<Void> deleteResume(@PathVariable Long id, Authentication authentication) {
        Resume existing = resumeService.getResumeById(id);
        verifyResumeAccess(existing, authentication);
        resumeService.deleteResume(id);
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private void verifyResumeAccess(Resume resume, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Authentication is required.");
        }
        if (!isAdmin(authentication)) {
            Candidate candidate = candidateService.getCandidateByUserEmail(authentication.getName());
            if (candidate == null || resume.getCandidate() == null
                    || !resume.getCandidate().getId().equals(candidate.getId())) {
                throw new AccessDeniedException("You are not authorized to access another candidate's resume.");
            }
        }
    }

    private void verifyCandidateIdAccess(Long candidateId, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Authentication is required.");
        }
        if (!isAdmin(authentication)) {
            Candidate candidate = candidateService.getCandidateByUserEmail(authentication.getName());
            if (candidate == null || !candidate.getId().equals(candidateId)) {
                throw new AccessDeniedException("You are not authorized to view resumes for another candidate.");
            }
        }
    }
}
