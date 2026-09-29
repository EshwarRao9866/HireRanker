package spring.eshwar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "resumes")
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Candidate is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", referencedColumnName = "id", nullable = false)
    private Candidate candidate;

    @NotBlank(message = "File name is required")
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "original_file_name", length = 255)
    private String originalFileName;

    @Column(name = "generated_file_name", length = 255)
    private String generatedFileName;

    @Column(name = "file_type", length = 50)
    private String fileType;

    @NotBlank(message = "File path is required")
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Lob
    @Column(name = "extracted_text", columnDefinition = "LONGTEXT")
    private String extractedText;

    @Lob
    @Column(name = "ai_analysis_json", columnDefinition = "LONGTEXT")
    private String aiAnalysisJson;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    public Resume() {
    }

    public Resume(Candidate candidate, String fileName, String fileType, String filePath, Long fileSize) {
        this.candidate = candidate;
        this.fileName = fileName;
        this.originalFileName = fileName;
        this.fileType = fileType;
        this.filePath = filePath;
        this.fileSize = fileSize;
    }

    public Resume(Candidate candidate, String fileName, String originalFileName, String generatedFileName,
                  String fileType, String filePath, Long fileSize) {
        this.candidate = candidate;
        this.fileName = fileName;
        this.originalFileName = originalFileName;
        this.generatedFileName = generatedFileName;
        this.fileType = fileType;
        this.filePath = filePath;
        this.fileSize = fileSize;
    }

    public Resume(Candidate candidate, String fileName, String fileType, String filePath, Long fileSize, String extractedText) {
        this.candidate = candidate;
        this.fileName = fileName;
        this.fileType = fileType;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.extractedText = extractedText;
    }

    public Resume(Long id, Candidate candidate, String fileName, String fileType, String filePath,
                  Long fileSize, String extractedText, LocalDateTime uploadedAt) {
        this.id = id;
        this.candidate = candidate;
        this.fileName = fileName;
        this.fileType = fileType;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.extractedText = extractedText;
        this.uploadedAt = uploadedAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.uploadedAt == null) {
            this.uploadedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Candidate getCandidate() {
        return candidate;
    }

    public void setCandidate(Candidate candidate) {
        this.candidate = candidate;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getOriginalFileName() {
        return originalFileName != null ? originalFileName : fileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getGeneratedFileName() {
        return generatedFileName;
    }

    public void setGeneratedFileName(String generatedFileName) {
        this.generatedFileName = generatedFileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }

    public String getAiAnalysisJson() {
        return aiAnalysisJson;
    }

    public void setAiAnalysisJson(String aiAnalysisJson) {
        this.aiAnalysisJson = aiAnalysisJson;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    @Override
    public String toString() {
        return "Resume{" +
                "id=" + id +
                ", candidateId=" + (candidate != null ? candidate.getId() : null) +
                ", fileName='" + fileName + '\'' +
                ", fileType='" + fileType + '\'' +
                ", filePath='" + filePath + '\'' +
                ", fileSize=" + fileSize +
                ", uploadedAt=" + uploadedAt +
                '}';
    }
}
