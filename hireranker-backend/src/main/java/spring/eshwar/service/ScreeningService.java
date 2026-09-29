package spring.eshwar.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.eshwar.entity.Application;
import spring.eshwar.entity.ScreeningResult;
import spring.eshwar.exception.ResourceNotFoundException;
import spring.eshwar.repository.ApplicationRepository;
import spring.eshwar.repository.ScreeningResultRepository;

import java.util.Optional;

@Service
public class ScreeningService {

    private final ScreeningResultRepository screeningResultRepository;
    private final ApplicationRepository applicationRepository;

    public ScreeningService(ScreeningResultRepository screeningResultRepository,
                            ApplicationRepository applicationRepository) {
        this.screeningResultRepository = screeningResultRepository;
        this.applicationRepository = applicationRepository;
    }

    @Transactional(readOnly = true)
    public ScreeningResult getScreeningResultById(Long id) {
        return screeningResultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ScreeningResult", "id", id));
    }

    @Transactional(readOnly = true)
    public ScreeningResult getScreeningResultByApplicationId(Long applicationId) {
        return screeningResultRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("ScreeningResult", "applicationId", applicationId));
    }

    @Transactional(readOnly = true)
    public boolean existsByApplicationId(Long applicationId) {
        return screeningResultRepository.existsByApplicationId(applicationId);
    }

    @Transactional
    public ScreeningResult saveScreeningResult(ScreeningResult result) {
        if (result.getApplication() == null || result.getApplication().getId() == null) {
            throw new IllegalArgumentException("ScreeningResult must be associated with an Application.");
        }

        Long applicationId = result.getApplication().getId();
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        validateScore("overallScore", result.getOverallScore());
        validateScore("skillsScore", result.getSkillsScore());
        validateScore("experienceScore", result.getExperienceScore());
        validateScore("educationScore", result.getEducationScore());

        // If a screening result already exists for this application, update the existing record
        Optional<ScreeningResult> existingOpt = screeningResultRepository.findByApplicationId(applicationId);
        if (existingOpt.isPresent()) {
            ScreeningResult existing = existingOpt.get();
            existing.setOverallScore(result.getOverallScore());
            existing.setSkillsScore(result.getSkillsScore());
            existing.setExperienceScore(result.getExperienceScore());
            existing.setEducationScore(result.getEducationScore());
            existing.setMatchingSkills(result.getMatchingSkills());
            existing.setMissingSkills(result.getMissingSkills());
            existing.setRecommendation(result.getRecommendation());
            return screeningResultRepository.save(existing);
        }

        result.setApplication(application);
        return screeningResultRepository.save(result);
    }

    @Transactional
    public void deleteScreeningResult(Long id) {
        if (!screeningResultRepository.existsById(id)) {
            throw new ResourceNotFoundException("ScreeningResult", "id", id);
        }
        screeningResultRepository.deleteById(id);
    }

    private void validateScore(String fieldName, Double score) {
        if (score != null && (score < 0.0 || score > 100.0)) {
            throw new IllegalArgumentException(fieldName + " must be between 0.0 and 100.0. Current value: " + score);
        }
    }
}
