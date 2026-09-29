package spring.eshwar.dto.resume;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ResumeRequest {

    @NotNull(message = "Candidate ID is required")
    private Long candidateId;

    @NotBlank(message = "File name is required")
    private String fileName;

    @NotBlank(message = "File path is required")
    private String filePath;

    private String fileType;
    private Long fileSize;
    private String extractedText;

    public ResumeRequest() {
    }

    public ResumeRequest(Long candidateId, String fileName, String filePath,
                         String fileType, Long fileSize, String extractedText) {
        this.candidateId = candidateId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.extractedText = extractedText;
    }

    public Long getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(Long candidateId) {
        this.candidateId = candidateId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
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
}
