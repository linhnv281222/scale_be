package org.facenet.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

/**
 * Utility for template file operations
 * 
 * Supports both development and production environments:
 * - Development: Files stored in src/main/resources/
 * - Production (JAR): Files stored relative to working directory or configured path
 * 
 * All paths in database are relative for portability
 */
@Slf4j
@Component
public class TemplateFileUtil {

    @Value("${app.template.upload-dir:templates/reports}")
    private String uploadDir;

    @Value("${app.template.base-dir:./resources}")
    private String baseDir;

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * Save template file to resources directory
     * Uses relative paths for portability (JAR deployment)
     * 
     * @param file File to save
     * @param templateCode Template identifier
     * @param originalFilename Original filename
     * @return Relative path (stored in database)
     */
    public String saveTemplateToResources(byte[] file, String templateCode, String originalFilename) throws IOException {
        if (file == null || file.length == 0) {
            throw new IllegalArgumentException("File content cannot be empty");
        }

        // Create directory if not exists
        Path dirPath = Paths.get(baseDir, uploadDir);
        Files.createDirectories(dirPath);

        // Generate unique filename with timestamp
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String extension = getFileExtension(originalFilename);
        String filename = String.format("%s_%s%s", templateCode, timestamp, extension);

        // Save file using absolute path
        Path filePath = dirPath.resolve(filename);
        Files.write(filePath, file);

        log.info("Template file saved to: {}", filePath.toAbsolutePath());

        // Return RELATIVE path for database (portable for JAR)
        String relativePath = uploadDir + "/" + filename;
        log.debug("Relative path stored in database: {}", relativePath);
        return relativePath;
    }

    /**
     * Calculate SHA-256 hash of file
     */
    public String calculateFileHash(byte[] fileContent) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fileContent);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("Error calculating file hash", e);
            throw new RuntimeException("Failed to calculate file hash", e);
        }
    }

    /**
     * Get absolute path from relative path
     * Works for both development and production environments
     * 
     * @param relativePath Relative path (as stored in database)
     * @return Absolute path for file system operations
     */
    public String getAbsolutePath(String relativePath) {
        return Paths.get(baseDir, relativePath).toAbsolutePath().toString();
    }

    /**
     * Get file size in bytes
     */
    public long getFileSize(byte[] content) {
        return content != null ? content.length : 0;
    }

    /**
     * Verify file exists at relative path
     */
    public boolean fileExists(String relativePath) {
        try {
            Path filePath = Paths.get(baseDir, relativePath);
            return Files.exists(filePath);
        } catch (Exception e) {
            log.warn("Error checking file existence: {}", relativePath, e);
            return false;
        }
    }

    /**
     * Delete file from resources (works with relative paths)
     */
    public boolean deleteTemplateFile(String relativePath) {
        try {
            Path filePath = Paths.get(baseDir, relativePath);
            boolean deleted = Files.deleteIfExists(filePath);
            log.info("Template file {} deleted: {}", relativePath, deleted);
            return deleted;
        } catch (IOException e) {
            log.error("Error deleting template file: {}", relativePath, e);
            return false;
        }
    }

    /**
     * Get file extension including dot
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * Read file content from resources (works with relative paths)
     */
    public byte[] readTemplateFile(String relativePath) throws IOException {
        Path filePath = Paths.get(baseDir, relativePath);
        if (!Files.exists(filePath)) {
            throw new IOException("Template file not found: " + relativePath);
        }
        return Files.readAllBytes(filePath);
    }

    /**
     * Validate file extension
     */
    public boolean isValidTemplateFile(String filename) {
        if (filename == null) {
            return false;
        }
        String lower = filename.toLowerCase();
        return lower.endsWith(".docx") || lower.endsWith(".doc") || 
               lower.endsWith(".xlsx") || lower.endsWith(".pdf");
    }

    /**
     * Get relative path (as stored in database)
     * Useful for logging and debugging
     */
    public String getRelativePath(String absolutePath) {
        try {
            Path absPath = Paths.get(absolutePath);
            Path basePath = Paths.get(baseDir).toAbsolutePath();
            return basePath.relativize(absPath).toString().replace("\\", "/");
        } catch (Exception e) {
            log.warn("Error computing relative path for: {}", absolutePath, e);
            return absolutePath;
        }
    }
}
