package spring.eshwar.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.eshwar.dto.CandidateProfileDto;
import spring.eshwar.dto.candidate.CandidateProfileUpdateRequest;
import spring.eshwar.dto.candidate.CandidateResponse;
import spring.eshwar.entity.Application;
import spring.eshwar.entity.Candidate;
import spring.eshwar.entity.Interview;
import spring.eshwar.entity.Resume;
import spring.eshwar.entity.User;
import spring.eshwar.exception.DuplicateResourceException;
import spring.eshwar.exception.ResourceNotFoundException;
import spring.eshwar.repository.ApplicationRepository;
import spring.eshwar.repository.CandidateRepository;
import spring.eshwar.repository.InterviewRepository;
import spring.eshwar.repository.ResumeRepository;
import spring.eshwar.repository.ScreeningResultRepository;
import spring.eshwar.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CandidateService {

    private final CandidateRepository candidateRepository;
    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final ApplicationRepository applicationRepository;
    private final ScreeningResultRepository screeningResultRepository;
    private final InterviewRepository interviewRepository;

    public CandidateService(CandidateRepository candidateRepository,
                            UserRepository userRepository,
                            ResumeRepository resumeRepository,
                            ApplicationRepository applicationRepository,
                            ScreeningResultRepository screeningResultRepository,
                            InterviewRepository interviewRepository) {
        this.candidateRepository = candidateRepository;
        this.userRepository = userRepository;
        this.resumeRepository = resumeRepository;
        this.applicationRepository = applicationRepository;
        this.screeningResultRepository = screeningResultRepository;
        this.interviewRepository = interviewRepository;
    }

    @Transactional(readOnly = true)
    public CandidateResponse getCandidateResponseById(Long id) {
        return CandidateResponse.fromEntity(getCandidateById(id));
    }

    @Transactional(readOnly = true)
    public CandidateResponse getCandidateResponseByUserId(Long userId) {
        return CandidateResponse.fromEntity(getCandidateByUserId(userId));
    }

    @Transactional(readOnly = true)
    public List<CandidateResponse> getAllCandidateResponses() {
        return candidateRepository.findAll().stream()
                .map(CandidateResponse::fromEntity)
                .toList();
    }

    // --- DTO Methods (Used by CandidateController) ---

    @Transactional(readOnly = true)
    public Optional<CandidateProfileDto> getProfileByUserId(Long userId) {
        return candidateRepository.findByUserId(userId)
                .map(this::mapToDto);
    }

    @Transactional
    public CandidateProfileDto updateProfile(Long userId, CandidateProfileDto dto) {
        Candidate candidate = candidateRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
                    Candidate c = new Candidate();
                    c.setUser(user);
                    return c;
                });

        if (dto.getFullName() != null) candidate.setFullName(dto.getFullName());
        if (dto.getPhone() != null) candidate.setPhone(dto.getPhone());
        if (dto.getLocation() != null) candidate.setLocation(dto.getLocation());
        if (dto.getSkills() != null) candidate.setSkills(dto.getSkills());
        if (dto.getExperience() != null) candidate.setExperience(dto.getExperience());
        if (dto.getEducation() != null) candidate.setEducation(dto.getEducation());
        if (dto.getGithub() != null) candidate.setGithub(dto.getGithub());
        if (dto.getLinkedin() != null) candidate.setLinkedin(dto.getLinkedin());

        Candidate saved = candidateRepository.save(candidate);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<CandidateProfileDto> getAllCandidates() {
        return candidateRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // --- Entity-Level Business Methods ---

    @Transactional(readOnly = true)
    public Candidate getCandidateById(Long id) {
        return candidateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate", "id", id));
    }

    @Transactional(readOnly = true)
    public Candidate getCandidateByUserId(Long userId) {
        return candidateRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate", "userId", userId));
    }

    @Transactional(readOnly = true)
    public Candidate getCandidateByUserEmail(String email) {
        return candidateRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate", "email", email));
    }

    @Transactional
    public Candidate getOrCreateCandidateByUserEmail(String email) {
        return candidateRepository.findByUserEmail(email)
                .orElseGet(() -> {
                    User user = userRepository.findByEmail(email)
                            .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
                    Candidate newCandidate = new Candidate();
                    newCandidate.setUser(user);
                    newCandidate.setFullName(user.getName());
                    return candidateRepository.save(newCandidate);
                });
    }

    @Transactional
    public CandidateResponse getCandidateProfileByEmail(String email) {
        Candidate candidate = candidateRepository.findByUserEmail(email)
                .orElseGet(() -> {
                    User user = userRepository.findByEmail(email)
                            .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
                    Candidate newCandidate = new Candidate();
                    newCandidate.setUser(user);
                    newCandidate.setFullName(user.getName());
                    return candidateRepository.save(newCandidate);
                });
        return CandidateResponse.fromEntity(candidate);
    }

    @Transactional
    public CandidateResponse updateCandidateProfile(String email, CandidateProfileUpdateRequest request) {
        Candidate candidate = candidateRepository.findByUserEmail(email)
                .orElseGet(() -> {
                    User user = userRepository.findByEmail(email)
                            .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
                    Candidate newCandidate = new Candidate();
                    newCandidate.setUser(user);
                    newCandidate.setFullName(user.getName());
                    return candidateRepository.save(newCandidate);
                });

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            candidate.setFullName(request.getFullName().trim());
            if (candidate.getUser() != null) {
                candidate.getUser().setName(request.getFullName().trim());
            }
        }
        if (request.getPhone() != null) candidate.setPhone(request.getPhone().trim());
        if (request.getLocation() != null) candidate.setLocation(request.getLocation().trim());
        if (request.getSkills() != null) candidate.setSkills(request.getSkills().trim());
        if (request.getExperience() != null) candidate.setExperience(request.getExperience().trim());
        if (request.getEducation() != null) candidate.setEducation(request.getEducation().trim());
        if (request.getGithub() != null) candidate.setGithub(request.getGithub().trim());
        if (request.getLinkedin() != null) candidate.setLinkedin(request.getLinkedin().trim());

        Candidate saved = candidateRepository.save(candidate);
        return CandidateResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public boolean existsByUserId(Long userId) {
        return candidateRepository.existsByUserId(userId);
    }

    @Transactional
    public Candidate createCandidate(Candidate candidate) {
        if (candidate.getUser() == null || candidate.getUser().getId() == null) {
            throw new IllegalArgumentException("Candidate must be associated with a valid User.");
        }

        Long userId = candidate.getUser().getId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (candidateRepository.existsByUser(user)) {
            throw new DuplicateResourceException("Candidate profile already exists for user id: " + userId);
        }

        candidate.setUser(user);
        if (candidate.getFullName() == null || candidate.getFullName().isBlank()) {
            candidate.setFullName(user.getName());
        }

        return candidateRepository.save(candidate);
    }

    @Transactional
    public Candidate updateCandidate(Long id, Candidate updated) {
        Candidate existing = getCandidateById(id);

        if (updated.getFullName() != null && !updated.getFullName().isBlank()) {
            existing.setFullName(updated.getFullName());
        }
        if (updated.getPhone() != null) existing.setPhone(updated.getPhone());
        if (updated.getLocation() != null) existing.setLocation(updated.getLocation());
        if (updated.getSkills() != null) existing.setSkills(updated.getSkills());
        if (updated.getExperience() != null) existing.setExperience(updated.getExperience());
        if (updated.getEducation() != null) existing.setEducation(updated.getEducation());
        if (updated.getGithub() != null) existing.setGithub(updated.getGithub());
        if (updated.getLinkedin() != null) existing.setLinkedin(updated.getLinkedin());

        return candidateRepository.save(existing);
    }

    @Transactional
    public void deleteCandidate(Long id) {
        Candidate candidate = candidateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate", "id", id));

        List<Application> applications = applicationRepository.findByCandidateId(id);
        for (Application app : applications) {
            screeningResultRepository.findByApplicationId(app.getId())
                    .ifPresent(screeningResultRepository::delete);
            List<Interview> interviews = interviewRepository.findByApplicationId(app.getId());
            interviewRepository.deleteAll(interviews);
            applicationRepository.delete(app);
        }

        List<Resume> resumes = resumeRepository.findAllByCandidateId(id);
        resumeRepository.deleteAll(resumes);

        candidateRepository.delete(candidate);
    }

    // --- Helper Mappers ---

    public CandidateProfileDto mapToDto(Candidate c) {
        CandidateProfileDto dto = new CandidateProfileDto();
        dto.setId(c.getId());
        if (c.getUser() != null) {
            dto.setUserId(c.getUser().getId());
            dto.setEmail(c.getUser().getEmail());
        }
        dto.setFullName(c.getFullName());
        dto.setPhone(c.getPhone());
        dto.setLocation(c.getLocation());
        dto.setSkills(c.getSkills());
        dto.setExperience(c.getExperience());
        dto.setEducation(c.getEducation());
        dto.setGithub(c.getGithub());
        dto.setLinkedin(c.getLinkedin());
        return dto;
    }
}
