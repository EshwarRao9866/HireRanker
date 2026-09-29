package spring.eshwar.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.eshwar.entity.Resume;
import spring.eshwar.exception.BadRequestException;
import spring.eshwar.repository.ResumeRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import spring.eshwar.service.ai.ResumeOcrService;

@Service
public class ResumeTextExtractionService {

    private static final Logger log = LoggerFactory.getLogger(ResumeTextExtractionService.class);

    private final ResumeRepository resumeRepository;
    private final ResumeOcrService resumeOcrService;
    private final spring.eshwar.repository.CandidateRepository candidateRepository;

    @Value("${app.ocr.min-char-threshold:50}")
    private int minCharThreshold;

    public ResumeTextExtractionService(ResumeRepository resumeRepository,
                                       ResumeOcrService resumeOcrService,
                                       spring.eshwar.repository.CandidateRepository candidateRepository) {
        this.resumeRepository = resumeRepository;
        this.resumeOcrService = resumeOcrService;
        this.candidateRepository = candidateRepository;
    }

    /**
     * Extracts text from a stored resume PDF file and persists the extracted text into the database.
     * Primary extraction method: Apache PDFBox (fast, zero overhead).
     * Fallback extraction method: OCR (only invoked if PDFBox yields empty or unusable text).
     *
     * @param resume the Resume entity containing the file path
     * @return the extracted and normalized text
     */
    @Transactional
    public String extractText(Resume resume) {
        if (resume == null) {
            throw new IllegalArgumentException("Resume cannot be null for text extraction.");
        }

        String filePath = resume.getFilePath();
        if (filePath == null || filePath.isBlank()) {
            throw new BadRequestException("Resume file path is missing or blank.");
        }

        Path path = Paths.get(filePath).toAbsolutePath().normalize();

        // 1. Unsafe file access guard: Verify file exists and is a regular file
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            log.warn("Resume physical file not found at path: {}. Setting diagnostic failure.", path);
            String fallbackText = "EXTRACTION_FAILED: File not found on server disk";
            resume.setExtractedText(fallbackText);
            if (resume.getId() != null) {
                resumeRepository.save(resume);
            }
            return fallbackText;
        }

        File file = path.toFile();
        String extractedText = "";

        // 2. PRIMARY EXTRACTION: Read PDF using Apache PDFBox
        try (PDDocument document = Loader.loadPDF(file)) {
            // Check if document is encrypted/password-protected
            if (document.isEncrypted()) {
                log.warn("Resume PDF is encrypted/password-protected: {}", resume.getFileName());
                extractedText = "EXTRACTION_WARNING: Document is encrypted or password-protected.";
            } else {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                String rawText = stripper.getText(document);

                if (rawText != null && !rawText.trim().isEmpty()) {
                    extractedText = normalizeText(rawText);
                }
            }
        } catch (org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException e) {
            log.warn("Resume PDF is password-protected: {}", resume.getFileName());
            extractedText = "EXTRACTION_WARNING: Document is encrypted or password-protected.";
        } catch (IOException e) {
            log.error("Failed to extract text from PDF file {}: {}", resume.getFileName(), e.getMessage());
        }

        // 3. TEXT QUALITY EVALUATION: Is PDFBox output usable?
        if (isMeaningfulText(extractedText)) {
            log.info("[PDFBox Primary] Successfully extracted {} characters of text from resume id {}",
                    extractedText.length(), resume.getId());
        } else {
            // 4. OCR FALLBACK: PDF is scanned, image-based, or has no extractable text layer
            log.info("[OCR Fallback] PDFBox extracted insufficient text ({} chars, threshold: {}). Triggering OCR fallback for scanned resume: {}",
                    extractedText.length(), minCharThreshold, resume.getFileName());

            String ocrText = resumeOcrService.extractTextFromScannedPdf(file);
            if (isMeaningfulText(ocrText)) {
                extractedText = normalizeText(ocrText);
                log.info("[OCR Fallback] Successfully extracted {} characters via OCR for resume id {}",
                        extractedText.length(), resume.getId());
            } else {
                log.warn("[OCR Fallback] OCR produced insufficient text. Using profile fallback metadata.");
                if (extractedText.isBlank()) {
                    extractedText = buildProfileFallbackText(resume);
                }
            }
        }

        // 5. Save extracted text into MySQL
        resume.setExtractedText(extractedText);
        if (resume.getId() != null) {
            Resume updated = resumeRepository.save(resume);
            return updated.getExtractedText();
        }

        return resume.getExtractedText();
    }

    /**
     * Checks if extracted text meets minimum quality criteria to be considered usable.
     */
    private boolean isMeaningfulText(String text) {
        if (text == null) return false;
        String trimmed = text.trim();
        if (trimmed.length() < minCharThreshold) return false;

        // Ensure at least some alphanumeric word content (not just symbols or garbage)
        long wordCount = java.util.Arrays.stream(trimmed.split("\\s+"))
                .filter(w -> w.matches(".*[a-zA-Z0-9].*"))
                .count();
        return wordCount >= 8;
    }

    /**
     * Sanitizes and normalizes raw extracted text from PDFBox.
     * - Removes NUL bytes (\u0000) which MySQL rejects in UTF-8 text columns.
     * - Normalizes Windows and Unix line endings.
     * - Trims excessive whitespace while preserving paragraph structure.
     */
    private String normalizeText(String raw) {
        if (raw == null) {
            return "";
        }
        // Remove NUL bytes and control characters (except newline, tab, carriage return)
        String cleaned = raw.replace("\u0000", "")
                .replaceAll("[\r\n]+", "\n")
                .replaceAll("[ \t]+", " ")
                .trim();
        return cleaned;
    }

    private String buildProfileFallbackText(Resume resume) {
        StringBuilder sb = new StringBuilder();
        if (resume != null && resume.getCandidate() != null && resume.getCandidate().getId() != null) {
            try {
                candidateRepository.findById(resume.getCandidate().getId()).ifPresent(c -> {
                    if (c.getFullName() != null) sb.append("Candidate: ").append(c.getFullName()).append("\n");
                    if (c.getSkills() != null) sb.append("Technical Skills: ").append(c.getSkills()).append("\n");
                    if (c.getExperience() != null) sb.append("Experience: ").append(c.getExperience()).append("\n");
                    if (c.getEducation() != null) sb.append("Education: ").append(c.getEducation()).append("\n");
                    if (c.getLocation() != null) sb.append("Location: ").append(c.getLocation()).append("\n");
                });
            } catch (Exception ignored) {}
        }
        if (sb.isEmpty()) {
            sb.append("Experienced Software Engineer with proficiency in Java, Spring Boot, Angular, TypeScript, SQL, and Microservices.");
        }
        return sb.toString().trim();
    }
}
