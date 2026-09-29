package spring.eshwar.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import spring.eshwar.entity.Candidate;
import spring.eshwar.entity.Resume;
import spring.eshwar.exception.ResourceNotFoundException;
import spring.eshwar.repository.CandidateRepository;
import spring.eshwar.repository.ResumeRepository;
import spring.eshwar.service.ai.ResumeScreeningAIService;
import spring.eshwar.service.skill.SkillNormalizationService;
import spring.eshwar.util.FileStorageUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ResumeService {

    private static final Logger log = LoggerFactory.getLogger(ResumeService.class);

    private final ResumeRepository resumeRepository;
    private final CandidateRepository candidateRepository;
    private final FileStorageUtil fileStorageUtil;
    private final ResumeTextExtractionService textExtractionService;
    private final ResumeScreeningAIService screeningAIService;
    private final SkillNormalizationService skillNormalizationService;
    private final spring.eshwar.repository.JobRepository jobRepository;
    private final spring.eshwar.repository.EvaluationCriteriaRepository evaluationCriteriaRepository;

    public ResumeService(ResumeRepository resumeRepository,
                         CandidateRepository candidateRepository,
                         FileStorageUtil fileStorageUtil,
                         ResumeTextExtractionService textExtractionService,
                         ResumeScreeningAIService screeningAIService,
                         SkillNormalizationService skillNormalizationService,
                         spring.eshwar.repository.JobRepository jobRepository,
                         spring.eshwar.repository.EvaluationCriteriaRepository evaluationCriteriaRepository) {
        this.resumeRepository = resumeRepository;
        this.candidateRepository = candidateRepository;
        this.fileStorageUtil = fileStorageUtil;
        this.textExtractionService = textExtractionService;
        this.screeningAIService = screeningAIService;
        this.skillNormalizationService = skillNormalizationService;
        this.jobRepository = jobRepository;
        this.evaluationCriteriaRepository = evaluationCriteriaRepository;
    }

    @Transactional(readOnly = true)
    public Resume getResumeById(Long id) {
        return resumeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", "id", id));
    }

    @Transactional(readOnly = true)
    public Resume getResumeByCandidateId(Long candidateId) {
        return resumeRepository.findByCandidateId(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", "candidateId", candidateId));
    }

    @Transactional(readOnly = true)
    public List<Resume> getAllResumesByCandidateId(Long candidateId) {
        return resumeRepository.findAllByCandidateId(candidateId);
    }

    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return resumeRepository.existsById(id);
    }

    @Transactional(readOnly = true)
    public org.springframework.core.io.Resource getResumeFileResource(Long id) {
        Resume resume = getResumeById(id);
        if (resume.getFilePath() == null || resume.getFilePath().isBlank()) {
            return new org.springframework.core.io.ByteArrayResource(generateFallbackPdfBytes(resume));
        }
        try {
            return fileStorageUtil.loadFileAsResource(resume.getFilePath());
        } catch (Exception e) {
            log.warn("Physical file not found at {} for resume ID {}. Returning generated fallback PDF: {}",
                    resume.getFilePath(), id, e.getMessage());
            return new org.springframework.core.io.ByteArrayResource(generateFallbackPdfBytes(resume));
        }
    }

    private byte[] generateFallbackPdfBytes(Resume resume) {
        String candidateName = (resume != null && resume.getCandidate() != null && resume.getCandidate().getFullName() != null)
                ? resume.getCandidate().getFullName()
                : "Candidate Resume";
        String fileName = (resume != null && resume.getFileName() != null) ? resume.getFileName() : "resume.pdf";
        Long resumeId = (resume != null) ? resume.getId() : 1L;

        String pdfContent = "%PDF-1.4\n" +
                "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n" +
                "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n" +
                "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >> endobj\n" +
                "4 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n" +
                "5 0 obj << /Length 220 >> stream\n" +
                "BT\n" +
                "/F1 18 Tf\n" +
                "50 720 Td\n" +
                "(" + escapePdfText(candidateName) + " - Resume Preview) Tj\n" +
                "/F1 12 Tf\n" +
                "0 -30 Td\n" +
                "(File: " + escapePdfText(fileName) + " | Resume ID: #" + resumeId + ") Tj\n" +
                "0 -25 Td\n" +
                "(Verified ATS Candidate Profile - HireRanker Platform) Tj\n" +
                "ET\n" +
                "endstream\n" +
                "endobj\n" +
                "xref\n" +
                "0 6\n" +
                "0000000000 65535 f \n" +
                "0000000009 00000 n \n" +
                "0000000058 00000 n \n" +
                "0000000115 00000 n \n" +
                "0000000234 00000 n \n" +
                "0000000305 00000 n \n" +
                "trailer << /Size 6 /Root 1 0 R >>\n" +
                "startxref\n" +
                "580\n" +
                "%%EOF\n";
        return pdfContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String escapePdfText(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    @Transactional
    public Resume saveResumeMetadata(Resume resume) {
        if (resume.getCandidate() == null || resume.getCandidate().getId() == null) {
            throw new IllegalArgumentException("Resume must be linked to a valid Candidate.");
        }

        Long candidateId = resume.getCandidate().getId();
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate", "id", candidateId));

        if (resume.getFileName() == null || resume.getFileName().isBlank()) {
            throw new IllegalArgumentException("Resume file name cannot be blank.");
        }
        if (resume.getFilePath() == null || resume.getFilePath().isBlank()) {
            throw new IllegalArgumentException("Resume file path cannot be blank.");
        }

        resume.setCandidate(candidate);
        return resumeRepository.save(resume);
    }

    @Transactional
    public Resume updateExtractedText(Long id, String extractedText) {
        Resume resume = getResumeById(id);
        resume.setExtractedText(extractedText);
        return resumeRepository.save(resume);
    }

    @Transactional(readOnly = true)
    public Resume getMyActiveResume(Candidate candidate) {
        if (candidate == null || candidate.getId() == null) {
            throw new ResourceNotFoundException("Candidate", "id", null);
        }
        List<Resume> list = resumeRepository.findAllByCandidateId(candidate.getId());
        if (list.isEmpty()) {
            throw new ResourceNotFoundException("Resume for candidate", "candidateId", candidate.getId());
        }
        return list.get(list.size() - 1);
    }

    public Resume uploadResume(MultipartFile file, Candidate candidate) {
        return uploadResume(file, candidate, null, null);
    }

    public Resume uploadResume(MultipartFile file, Candidate candidate, String targetRole, Long jobId) {
        if (candidate == null || candidate.getId() == null) {
            throw new IllegalArgumentException("Resume must be associated with a valid candidate.");
        }

        // Phase 1 (DB Tx): Store file, persist Resume entity, and extract text
        Resume savedResume = storeAndPersistResume(file, candidate);

        // Phase 2 (No Tx): Run Unified AI Screening outside active transaction
        try {
            savedResume = screenCandidateResume(savedResume.getId(), targetRole, jobId, candidate);
        } catch (Exception e) {
            log.error("AI resume screening failed for resume id {}: {}. Falling back to standard analysis.", savedResume.getId(), e.getMessage());
            try {
                performAiAnalysis(savedResume);
                savedResume = resumeRepository.findById(savedResume.getId()).orElse(savedResume);
            } catch (Exception ignored) {}
        }

        return savedResume;
    }

    public Resume screenCandidateResume(Long resumeId, String targetRole, Long jobId, Candidate candidate) {
        Resume resume = getResumeById(resumeId);

        // 1. Ensure text is extracted using Apache PDFBox + OCR fallback
        if (resume.getExtractedText() == null || resume.getExtractedText().isBlank() || resume.getExtractedText().startsWith("[EXTRACTION_FAILED")) {
            textExtractionService.extractText(resume);
            resume = getResumeById(resumeId);
        }

        // 2. Resolve target Job & Evaluation Criteria
        spring.eshwar.entity.Job job = null;
        if (jobId != null) {
            job = jobRepository.findById(jobId).orElse(null);
        }
        if (job == null && targetRole != null && !targetRole.isBlank()) {
            String roleClean = targetRole.trim().toLowerCase();
            List<spring.eshwar.entity.Job> allJobs = jobRepository.findAll();
            for (spring.eshwar.entity.Job j : allJobs) {
                if (j.getTitle() != null && j.getTitle().toLowerCase().contains(roleClean)) {
                    job = j;
                    break;
                }
            }
            if (job == null && !allJobs.isEmpty()) {
                job = allJobs.get(0);
            }
        }
        if (job == null) {
            List<spring.eshwar.entity.Job> allJobs = jobRepository.findAll();
            if (!allJobs.isEmpty()) {
                job = allJobs.get(0);
            }
        }

        spring.eshwar.entity.EvaluationCriteria criteria = (job != null)
                ? evaluationCriteriaRepository.findByJobId(job.getId()).orElse(null)
                : null;

        String effectiveTitle = (targetRole != null && !targetRole.isBlank())
                ? targetRole.trim()
                : (job != null ? job.getTitle() : "Full Stack Software Engineer");

        String effectiveDescription = (job != null && job.getDescription() != null)
                ? job.getDescription()
                : "Design, build, and maintain scalable web and enterprise applications, develop microservices, and collaborate on cross-functional engineering teams.";

        String effectiveSkills = (criteria != null && criteria.getRequiredSkills() != null && !criteria.getRequiredSkills().isBlank())
                ? criteria.getRequiredSkills()
                : (job != null && job.getRequiredSkills() != null ? job.getRequiredSkills() : "Java, Spring Boot, Angular, TypeScript, SQL, REST APIs, Microservices, Git, Docker");

        String effectiveExp = (job != null && job.getExperienceRequired() != null)
                ? job.getExperienceRequired()
                : "3-5 years";

        Double minExp = (criteria != null && criteria.getMinimumExperience() != null)
                ? criteria.getMinimumExperience()
                : 3.0;

        String eduRequirements = (criteria != null && criteria.getEducationRequirements() != null)
                ? criteria.getEducationRequirements()
                : "Bachelor's Degree in Computer Science, Software Engineering, or related discipline.";

        Double skillWeight = (criteria != null && criteria.getSkillWeight() != null) ? criteria.getSkillWeight() : 50.0;
        Double expWeight = (criteria != null && criteria.getExperienceWeight() != null) ? criteria.getExperienceWeight() : 30.0;
        Double eduWeight = (criteria != null && criteria.getEducationWeight() != null) ? criteria.getEducationWeight() : 20.0;

        // 3. Assemble ScreeningContext (The EXACT same context used by Admin screening)
        spring.eshwar.service.ai.AIScreeningProvider.ScreeningContext context = new spring.eshwar.service.ai.AIScreeningProvider.ScreeningContext(
                effectiveTitle,
                effectiveDescription,
                effectiveSkills,
                effectiveExp,
                minExp,
                eduRequirements,
                skillWeight,
                expWeight,
                eduWeight,
                resume.getExtractedText()
        );

        // 4. Run AI semantic matching & 8-dimension ATS evaluation
        spring.eshwar.service.ai.AIScreeningProvider.ScreeningEvaluationResult evalResult = screeningAIService.evaluateResume(context);

        // 5. Serialize full structured evaluation into JSON for Resume
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            java.util.List<String> allExtractedSkills = skillNormalizationService.extractSkillsFromResumeText(resume.getExtractedText());
            if (allExtractedSkills.isEmpty() && evalResult.matchingSkills() != null && !evalResult.matchingSkills().isBlank()) {
                allExtractedSkills = java.util.Arrays.stream(evalResult.matchingSkills().split("[,;]"))
                        .map(String::trim).filter(s -> !s.isEmpty()).toList();
            }
            map.put("skills", allExtractedSkills);
            map.put("detectedSkills", allExtractedSkills);
            map.put("matchingSkills", evalResult.matchingSkills());
            map.put("missingSkills", java.util.Arrays.stream(evalResult.missingSkills().split("[,;]"))
                    .map(String::trim).filter(s -> !s.isEmpty()).toList());
            map.put("recommendedSkills", java.util.Arrays.stream(evalResult.recommendedSkills().split("[,;]"))
                    .map(String::trim).filter(s -> !s.isEmpty()).toList());
            map.put("strengths", java.util.Arrays.stream(evalResult.strengths().split(";"))
                    .map(String::trim).filter(s -> !s.isEmpty()).toList());
            map.put("weaknesses", java.util.Arrays.stream(evalResult.weaknesses().split(";"))
                    .map(String::trim).filter(s -> !s.isEmpty()).toList());
            map.put("improvementSuggestions", java.util.Arrays.stream(evalResult.improvementSuggestions().split(";"))
                    .map(String::trim).filter(s -> !s.isEmpty()).toList());
            map.put("resumeSummary", evalResult.resumeSummary());
            map.put("recommendation", evalResult.recommendation());

            // 8 ATS dimensions
            map.put("overallScore", evalResult.overallScore());
            map.put("technicalSkillsScore", evalResult.skillsScore());
            map.put("skillsScore", evalResult.skillsScore());
            map.put("jobSkillScore", evalResult.skillsScore());
            map.put("experienceScore", evalResult.experienceScore());
            map.put("educationScore", evalResult.educationScore());
            map.put("keywordScore", evalResult.keywordScore());
            map.put("jobDescriptionScore", evalResult.keywordScore());
            map.put("projectScore", evalResult.projectScore());
            map.put("formattingScore", evalResult.formattingScore());
            map.put("atsCompatibilityScore", evalResult.formattingScore());
            map.put("certificationScore", evalResult.certificationScore());
            map.put("completenessScore", evalResult.certificationScore());
            map.put("achievementScore", evalResult.achievementScore());
            map.put("status", "COMPLETE");

            String analysisJson = mapper.writeValueAsString(map);
            saveAiAnalysisJson(resume.getId(), analysisJson);

            // 6. Enrich candidate profile skills with normalized skills
            Candidate candToEnrich = candidate;
            if (candToEnrich == null && resume.getCandidate() != null) {
                candToEnrich = candidateRepository.findById(resume.getCandidate().getId()).orElse(null);
            }
            if (candToEnrich != null) {
                enrichCandidateFromAnalysis(candToEnrich, analysisJson);
            }
        } catch (Exception e) {
            log.warn("Could not serialize screening evaluation result to JSON: {}", e.getMessage());
        }

        return getResumeById(resumeId);
    }

    @Transactional
    public Resume storeAndPersistResume(MultipartFile file, Candidate candidate) {
        // 1. Store file using FileStorageUtil (validates format, magic bytes, size, traversal, etc.)
        FileStorageUtil.StoredFile storedFile = fileStorageUtil.storePdfFile(file);

        // 2. Check if candidate already has an existing resume to replace
        List<Resume> existingResumes = resumeRepository.findAllByCandidateId(candidate.getId());
        Resume resume;
        String oldFilePath = null;
        if (!existingResumes.isEmpty()) {
            resume = existingResumes.get(0);
            oldFilePath = resume.getFilePath();
            // Delete extra duplicate records if any exist
            for (int i = 1; i < existingResumes.size(); i++) {
                try {
                    fileStorageUtil.deleteFile(existingResumes.get(i).getFilePath());
                    resumeRepository.delete(existingResumes.get(i));
                } catch (Exception ignored) {}
            }
        } else {
            resume = new Resume();
            resume.setCandidate(candidate);
        }

        resume.setFileName(storedFile.originalFilename());
        resume.setOriginalFileName(storedFile.originalFilename());
        resume.setGeneratedFileName(storedFile.storedFilename());
        resume.setFilePath(storedFile.filePath());
        resume.setFileType(storedFile.contentType());
        resume.setFileSize(storedFile.fileSize());
        resume.setUploadedAt(LocalDateTime.now());
        resume.setExtractedText(null);
        resume.setAiAnalysisJson(null);

        Resume savedResume;
        try {
            savedResume = resumeRepository.save(resume);
            // Clean up old file if path changed
            if (oldFilePath != null && !oldFilePath.equals(storedFile.filePath())) {
                fileStorageUtil.deleteFile(oldFilePath);
            }
        } catch (Exception ex) {
            fileStorageUtil.deleteFile(storedFile.filePath());
            throw ex;
        }

        // 3. Trigger PDF text extraction via Apache PDFBox
        try {
            textExtractionService.extractText(savedResume);
            savedResume = resumeRepository.findById(savedResume.getId()).orElse(savedResume);
        } catch (Exception e) {
            log.error("Failed to extract text from resume id {}: {}", savedResume.getId(), e.getMessage());
        }

        return savedResume;
    }

    @Transactional(readOnly = true)
    public Resume analyzeResume(Long resumeId) {
        Resume resume = getResumeById(resumeId);
        if (resume.getExtractedText() == null || resume.getExtractedText().isBlank()) {
            extractTextForResume(resumeId);
            resume = getResumeById(resumeId);
        }
        performAiAnalysis(resume);
        return getResumeById(resumeId);
    }

    private void performAiAnalysis(Resume resume) {
        if (resume == null || resume.getExtractedText() == null || resume.getExtractedText().isBlank()) {
            return;
        }

        Long candidateId = null;
        try {
            if (resume.getCandidate() != null) {
                candidateId = resume.getCandidate().getId();
            }
        } catch (Exception e) {
            log.warn("Could not retrieve candidate id directly from resume proxy: {}", e.getMessage());
        }

        Candidate candidate = (candidateId != null) ? candidateRepository.findById(candidateId).orElse(null) : null;
        String cSkills = (candidate != null) ? candidate.getSkills() : null;
        String cExp = (candidate != null) ? candidate.getExperience() : null;
        String cEdu = (candidate != null) ? candidate.getEducation() : null;

        String analysisJson = screeningAIService.analyzeResumeText(resume.getExtractedText(), cSkills, cExp, cEdu);
        if (analysisJson != null && !analysisJson.isBlank()) {
            saveAiAnalysisJson(resume.getId(), analysisJson);

            // Dynamically enrich candidate skills from AI resume analysis
            if (candidate != null) {
                enrichCandidateFromAnalysis(candidate, analysisJson);
            }
        }
    }

    private void enrichCandidateFromAnalysis(Candidate candidate, String analysisJson) {
        if (candidate == null || analysisJson == null || analysisJson.isBlank()) {
            return;
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(analysisJson);
            if (root.has("skills") && root.get("skills").isArray()) {
                List<String> detectedSkills = new java.util.ArrayList<>();
                for (com.fasterxml.jackson.databind.JsonNode sn : root.get("skills")) {
                    String s = sn.asText("").trim();
                    if (!s.isEmpty()) {
                        detectedSkills.add(s);
                    }
                }
                if (!detectedSkills.isEmpty()) {
                    List<String> normalizedSkills = skillNormalizationService.decomposeAndNormalizeSkills(String.join(", ", detectedSkills));
                    if (!normalizedSkills.isEmpty()) {
                        candidate.setSkills(String.join(", ", normalizedSkills));
                        candidateRepository.save(candidate);
                        log.info("Updated candidate #{} profile skills with normalized skills from resume analysis: {}", candidate.getId(), candidate.getSkills());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not enrich candidate skills from analysis JSON: {}", e.getMessage());
        }
    }

    @Transactional
    public void saveAiAnalysisJson(Long resumeId, String analysisJson) {
        if (resumeId != null && analysisJson != null) {
            resumeRepository.findById(resumeId).ifPresent(r -> {
                r.setAiAnalysisJson(analysisJson);
                resumeRepository.save(r);
            });
        }
    }

    @Transactional
    public Resume extractTextForResume(Long resumeId) {
        Resume resume = getResumeById(resumeId);
        textExtractionService.extractText(resume);
        return getResumeById(resumeId);
    }

    @Transactional
    public void deleteResume(Long id) {
        Resume resume = getResumeById(id);
        if (resume.getFilePath() != null) {
            fileStorageUtil.deleteFile(resume.getFilePath());
        }
        resumeRepository.delete(resume);
    }
}
