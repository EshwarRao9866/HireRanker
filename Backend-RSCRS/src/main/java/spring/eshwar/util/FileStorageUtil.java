package spring.eshwar.util;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import spring.eshwar.exception.BadRequestException;
import spring.eshwar.exception.FileStorageException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

@Component
public class FileStorageUtil {

    private static final Logger log = LoggerFactory.getLogger(FileStorageUtil.class);

    // Standard PDF Magic Header: %PDF- (hex: 25 50 44 46 2D)
    private static final byte[] PDF_MAGIC_BYTES = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D};
    public static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB

    private final Path uploadLocation;

    public FileStorageUtil(@Value("${app.upload.dir:uploads/resumes}") String uploadDir) {
        this.uploadLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.uploadLocation);
            log.info("Initialized resume upload directory at: {}", this.uploadLocation);
        } catch (IOException e) {
            throw new FileStorageException("Could not initialize storage directory at: " + this.uploadLocation, e);
        }
    }

    /**
     * Validates and securely stores an uploaded PDF resume.
     *
     * @param file the uploaded MultipartFile
     * @return StoredFile metadata containing original filename, stored filename, relative path, and size
     */
    public StoredFile storePdfFile(MultipartFile file) {
        // 1. Check if file is null or empty
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please select a file to upload.");
        }

        // 2. Validate maximum file size (10 MB limit)
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("File size exceeds the maximum allowed limit of 10 MB.");
        }

        // 3. Validate original filename and check path traversal
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), "resume.pdf"));
        if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new BadRequestException("Invalid filename contains illegal characters or path traversal sequence.");
        }

        // 4. Validate extension is .pdf
        if (!originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new BadRequestException("Only PDF files are allowed. Given filename: " + originalFilename);
        }

        // 5. Validate Content-Type (support standard PDF types and fallback octet-stream with strict magic byte validation)
        String contentType = file.getContentType();
        boolean isKnownPdfMime = contentType != null && (
                contentType.equalsIgnoreCase("application/pdf")
                || contentType.equalsIgnoreCase("application/x-pdf")
                || contentType.equalsIgnoreCase("application/acrobat")
                || contentType.equalsIgnoreCase("application/octet-stream")
                || contentType.equalsIgnoreCase("binary/octet-stream")
                || contentType.toLowerCase().contains("pdf")
        );
        if (contentType == null || contentType.isBlank() || !isKnownPdfMime) {
            throw new BadRequestException("Invalid file type: Content-Type must be application/pdf. Provided: " + contentType);
        }

        // 6. Deep inspection: Validate PDF magic bytes (%PDF-)
        validatePdfMagicBytes(file);

        // 7. Generate a unique, unpredictable filename (UUID)
        String uniqueFilename = UUID.randomUUID() + ".pdf";
        Path targetLocation = this.uploadLocation.resolve(uniqueFilename).normalize();

        // 8. Prevent path traversal attack against base upload directory
        if (!targetLocation.startsWith(this.uploadLocation)) {
            throw new BadRequestException("Cannot store file outside current storage directory.");
        }

        // 9. Store the file to disk
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.info("Successfully stored resume PDF: {} as {} ({} bytes)",
                    originalFilename, uniqueFilename, file.getSize());
        } catch (IOException e) {
            throw new FileStorageException("Failed to store file " + originalFilename + " on disk.", e);
        }

        return new StoredFile(
                originalFilename,
                uniqueFilename,
                targetLocation.toString(),
                file.getSize(),
                "application/pdf"
        );
    }

    /**
     * Deep check verifying that the file input stream actually starts with the %PDF- magic bytes header.
     */
    private void validatePdfMagicBytes(MultipartFile file) {
        byte[] header = new byte[5];
        try (InputStream is = file.getInputStream()) {
            int read = is.read(header, 0, 5);
            if (read < 5 || !Arrays.equals(header, PDF_MAGIC_BYTES)) {
                throw new BadRequestException("Invalid file format: file header does not match valid PDF specification.");
            }
        } catch (IOException e) {
            throw new FileStorageException("Failed to inspect file format headers.", e);
        }
    }

    /**
     * Deletes a stored file from disk if entity persistence fails.
     */
    public boolean deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Could not delete stored file at {}: {}", filePath, e.getMessage());
            return false;
        }
    }

    public org.springframework.core.io.Resource loadFileAsResource(String filePath) {
        try {
            Path path = Paths.get(filePath).normalize();
            org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(path.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            Path fallbackPath = this.uploadLocation.resolve(Paths.get(filePath).getFileName()).normalize();
            org.springframework.core.io.Resource fallbackResource = new org.springframework.core.io.UrlResource(fallbackPath.toUri());
            if (fallbackResource.exists() && fallbackResource.isReadable()) {
                return fallbackResource;
            }
            throw new spring.eshwar.exception.ResourceNotFoundException("Resume file", "path", filePath);
        } catch (java.net.MalformedURLException e) {
            throw new spring.eshwar.exception.ResourceNotFoundException("Resume file", "path", filePath);
        }
    }

    public Path getUploadLocation() {
        return uploadLocation;
    }

    /**
     * Value object representing the stored file metadata.
     */
    public record StoredFile(
            String originalFilename,
            String storedFilename,
            String filePath,
            long fileSize,
            String contentType
    ) {}
}
