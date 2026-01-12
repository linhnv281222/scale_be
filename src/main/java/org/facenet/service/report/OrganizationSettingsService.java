package org.facenet.service.report;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.facenet.dto.report.OrganizationSettingsDto;
import org.facenet.entity.report.OrganizationSettings;
import org.facenet.repository.report.OrganizationSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing organization settings
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationSettingsService {

    private final OrganizationSettingsRepository repository;
    
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("png", "jpg", "jpeg");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String LOGO_DIRECTORY = "src/main/resources/images/logos/";
    private static final String FAVICON_DIRECTORY = "src/main/resources/images/favicons/";

    @Transactional(readOnly = true)
    public OrganizationSettingsDto.Response getActiveSettings() {
        OrganizationSettings settings = repository.findActiveDefault()
                .orElseThrow(() -> new RuntimeException("No active organization settings found"));
        return toResponse(settings);
    }

    @Transactional
    public OrganizationSettingsDto.Response createSettings(
            OrganizationSettingsDto.CreateRequest request,
            MultipartFile logoFile,
            MultipartFile faviconFile) {
        
        if (request.getCompanyName() == null || request.getCompanyName().trim().isEmpty()) {
            throw new IllegalArgumentException("Company name is required");
        }

        OrganizationSettings settings = OrganizationSettings.builder()
                .companyName(request.getCompanyName())
                .companyNameEn(request.getCompanyNameEn())
                .address(request.getAddress())
                .phone(request.getPhone())
                .email(request.getEmail())
                .website(request.getWebsite())
                .taxCode(request.getTaxCode())
                .watermarkText(request.getWatermarkText())
                .isActive(true)
                .isDefault(Boolean.TRUE.equals(request.getIsDefault()))
                .build();

        // If set as default, unset other defaults
        if (settings.getIsDefault()) {
            repository.clearAllDefaults();
        }

        settings = repository.save(settings);
        
        // Upload logo if provided
        if (logoFile != null && !logoFile.isEmpty()) {
            uploadLogoFile(settings, logoFile);
            settings = repository.save(settings);
        }
        
        // Upload favicon if provided
        if (faviconFile != null && !faviconFile.isEmpty()) {
            uploadFaviconFile(settings, faviconFile);
            settings = repository.save(settings);
        }
        
        log.info("Created organization settings: {}", settings.getId());
        
        return toResponse(settings);
    }

    @Transactional(readOnly = true)
    public OrganizationSettingsDto.Response getById(Long id) {
        OrganizationSettings settings = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Organization settings not found: " + id));
        return toResponse(settings);
    }

    @Transactional
    public OrganizationSettingsDto.Response updateActiveSettings(OrganizationSettingsDto.UpdateRequest request) {
        OrganizationSettings settings = repository.findActiveDefault()
                .orElseThrow(() -> new RuntimeException("No active organization settings found"));
        settings = updateSettings(settings, request);
        return toResponse(settings);
    }

    @Transactional
    public OrganizationSettingsDto.Response updateActiveSettings(
            OrganizationSettingsDto.UpdateRequest request, 
            MultipartFile logoFile,
            Boolean deleteLogo,
            MultipartFile faviconFile,
            Boolean deleteFavicon) {
        
        OrganizationSettings settings = repository.findActiveDefault()
                .orElseThrow(() -> new RuntimeException("No active organization settings found"));
        
        // Update text fields
        settings = updateSettings(settings, request);
        
        // Handle logo deletion
        if (Boolean.TRUE.equals(deleteLogo)) {
            deleteLogoFiles(settings);
            settings.setLogoUrl(null);
            settings.setLogoData(null);
        }
        
        // Handle logo upload
        if (logoFile != null && !logoFile.isEmpty()) {
            uploadLogoFile(settings, logoFile);
        }
        
        // Handle favicon deletion
        if (Boolean.TRUE.equals(deleteFavicon)) {
            deleteFaviconFiles(settings);
            settings.setFaviconUrl(null);
            settings.setFaviconData(null);
        }
        
        // Handle favicon upload
        if (faviconFile != null && !faviconFile.isEmpty()) {
            uploadFaviconFile(settings, faviconFile);
        }
        
        settings = repository.save(settings);
        log.info("Updated organization settings with logo/favicon handling: {}", settings.getId());
        
        return toResponse(settings);
    }

    private OrganizationSettings updateSettings(OrganizationSettings settings, OrganizationSettingsDto.UpdateRequest request) {
        if (request.getCompanyName() != null) {
            settings.setCompanyName(request.getCompanyName());
        }
        if (request.getCompanyNameEn() != null) {
            settings.setCompanyNameEn(request.getCompanyNameEn());
        }
        if (request.getAddress() != null) {
            settings.setAddress(request.getAddress());
        }
        if (request.getPhone() != null) {
            settings.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            settings.setEmail(request.getEmail());
        }
        if (request.getWebsite() != null) {
            settings.setWebsite(request.getWebsite());
        }
        if (request.getTaxCode() != null) {
            settings.setTaxCode(request.getTaxCode());
        }
        if (request.getWatermarkText() != null) {
            settings.setWatermarkText(request.getWatermarkText());
        }
        if (request.getIsActive() != null) {
            settings.setIsActive(request.getIsActive());
        }

        return repository.save(settings);
    }

    @Transactional
    public OrganizationSettingsDto.Response create(OrganizationSettingsDto.CreateRequest request) {
        if (request.getCompanyName() == null || request.getCompanyName().trim().isEmpty()) {
            throw new IllegalArgumentException("Company name is required");
        }

        OrganizationSettings settings = OrganizationSettings.builder()
                .companyName(request.getCompanyName())
                .companyNameEn(request.getCompanyNameEn())
                .address(request.getAddress())
                .phone(request.getPhone())
                .email(request.getEmail())
                .website(request.getWebsite())
                .taxCode(request.getTaxCode())
                .watermarkText(request.getWatermarkText())
                .isActive(true)
                .isDefault(Boolean.TRUE.equals(request.getIsDefault()))
                .build();

        // If set as default, unset other defaults
        if (settings.getIsDefault()) {
            repository.clearAllDefaults();
        }

        settings = repository.save(settings);
        log.info("Created organization settings: {}", settings.getId());
        
        return toResponse(settings);
    }

    @Transactional
    public OrganizationSettingsDto.Response update(Long id, OrganizationSettingsDto.UpdateRequest request) {
        OrganizationSettings settings = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Organization settings not found: " + id));

        if (request.getCompanyName() != null) {
            settings.setCompanyName(request.getCompanyName());
        }
        if (request.getCompanyNameEn() != null) {
            settings.setCompanyNameEn(request.getCompanyNameEn());
        }
        if (request.getAddress() != null) {
            settings.setAddress(request.getAddress());
        }
        if (request.getPhone() != null) {
            settings.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            settings.setEmail(request.getEmail());
        }
        if (request.getWebsite() != null) {
            settings.setWebsite(request.getWebsite());
        }
        if (request.getTaxCode() != null) {
            settings.setTaxCode(request.getTaxCode());
        }
        if (request.getWatermarkText() != null) {
            settings.setWatermarkText(request.getWatermarkText());
        }
        if (request.getIsActive() != null) {
            settings.setIsActive(request.getIsActive());
        }

        settings = repository.save(settings);
        log.info("Updated organization settings: {}", settings.getId());
        
        return toResponse(settings);
    }

    @Transactional
    public OrganizationSettingsDto.Response uploadLogoToActive(MultipartFile file) {
        OrganizationSettings settings = repository.findActiveDefault()
                .orElseThrow(() -> new RuntimeException("No active organization settings found"));
        return uploadLogoToSettings(settings, file);
    }

    @Transactional
    public OrganizationSettingsDto.Response uploadLogo(Long id, MultipartFile file) {
        OrganizationSettings settings = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Organization settings not found: " + id));
        return uploadLogoToSettings(settings, file);
    }

    @Transactional
    public OrganizationSettingsDto.Response uploadFaviconToActive(MultipartFile file) {
        OrganizationSettings settings = repository.findActiveDefault()
                .orElseThrow(() -> new RuntimeException("No active organization settings found"));
        return uploadFaviconToSettings(settings, file);
    }

    @Transactional
    public OrganizationSettingsDto.Response uploadFavicon(Long id, MultipartFile file) {
        OrganizationSettings settings = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Organization settings not found: " + id));
        return uploadFaviconToSettings(settings, file);
    }

    private OrganizationSettingsDto.Response uploadLogoToSettings(OrganizationSettings settings, MultipartFile file) {
        uploadLogoFile(settings, file);
        settings = repository.save(settings);
        return toResponse(settings);
    }

    private OrganizationSettingsDto.Response uploadFaviconToSettings(OrganizationSettings settings, MultipartFile file) {
        uploadFaviconFile(settings, file);
        settings = repository.save(settings);
        return toResponse(settings);
    }

    private void uploadLogoFile(OrganizationSettings settings, MultipartFile file) {
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size (5MB)");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("Invalid filename");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Invalid file type. Allowed: " + String.join(", ", ALLOWED_EXTENSIONS));
        }

        try {
            // Generate unique filename
            String filename = "org-" + settings.getId() + "-" + UUID.randomUUID().toString() + "." + extension;
            
            // Save file to resources
            Path logoPath = Paths.get(LOGO_DIRECTORY);
            Files.createDirectories(logoPath);
            
            Path filePath = logoPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Update settings with logo path (relative to classpath)
            String logoUrl = "images/logos/" + filename;
            settings.setLogoUrl(logoUrl);
            
            // Also store in database for backup
            settings.setLogoData(file.getBytes());
            
            log.info("Uploaded logo for organization {}: {}", settings.getId(), logoUrl);
            
        } catch (IOException e) {
            log.error("Failed to upload logo for organization {}", settings.getId(), e);
            throw new RuntimeException("Failed to upload logo: " + e.getMessage());
        }
    }

    private void uploadFaviconFile(OrganizationSettings settings, MultipartFile file) {
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size (5MB)");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("Invalid filename");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Invalid file type. Allowed: " + String.join(", ", ALLOWED_EXTENSIONS));
        }

        try {
            // Generate unique filename
            String filename = "org-favicon-" + settings.getId() + "-" + UUID.randomUUID().toString() + "." + extension;
            
            // Save file to resources
            Path faviconPath = Paths.get(FAVICON_DIRECTORY);
            Files.createDirectories(faviconPath);
            
            Path filePath = faviconPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Update settings with favicon path (relative to classpath)
            String faviconUrl = "images/favicons/" + filename;
            settings.setFaviconUrl(faviconUrl);
            
            // Also store in database for backup
            settings.setFaviconData(file.getBytes());
            
            log.info("Uploaded favicon for organization {}: {}", settings.getId(), faviconUrl);
            
        } catch (IOException e) {
            log.error("Failed to upload favicon for organization {}", settings.getId(), e);
            throw new RuntimeException("Failed to upload favicon: " + e.getMessage());
        }
    }

    @Transactional
    public OrganizationSettingsDto.Response deleteActiveLogo() {
        OrganizationSettings settings = repository.findActiveDefault()
                .orElseThrow(() -> new RuntimeException("No active organization settings found"));
        return deleteLogoFromSettings(settings);
    }

    @Transactional
    public OrganizationSettingsDto.Response deleteLogo(Long id) {
        OrganizationSettings settings = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Organization settings not found: " + id));
        return deleteLogoFromSettings(settings);
    }

    @Transactional
    public OrganizationSettingsDto.Response deleteActiveFavicon() {
        OrganizationSettings settings = repository.findActiveDefault()
                .orElseThrow(() -> new RuntimeException("No active organization settings found"));
        return deleteFaviconFromSettings(settings);
    }

    @Transactional
    public OrganizationSettingsDto.Response deleteFavicon(Long id) {
        OrganizationSettings settings = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Organization settings not found: " + id));
        return deleteFaviconFromSettings(settings);
    }

    private OrganizationSettingsDto.Response deleteLogoFromSettings(OrganizationSettings settings) {
        deleteLogoFiles(settings);
        settings.setLogoUrl(null);
        settings.setLogoData(null);
        settings = repository.save(settings);
        
        log.info("Deleted logo for organization: {}", settings.getId());
        return toResponse(settings);
    }

    private OrganizationSettingsDto.Response deleteFaviconFromSettings(OrganizationSettings settings) {
        deleteFaviconFiles(settings);
        settings.setFaviconUrl(null);
        settings.setFaviconData(null);
        settings = repository.save(settings);
        
        log.info("Deleted favicon for organization: {}", settings.getId());
        return toResponse(settings);
    }

    private void deleteLogoFiles(OrganizationSettings settings) {
        // Delete file from resources
        if (settings.getLogoUrl() != null && !settings.getLogoUrl().isEmpty()) {
            try {
                String filename = settings.getLogoUrl().substring(settings.getLogoUrl().lastIndexOf('/') + 1);
                Path filePath = Paths.get(LOGO_DIRECTORY, filename);
                Files.deleteIfExists(filePath);
                log.info("Deleted logo file: {}", filePath);
            } catch (IOException e) {
                log.warn("Failed to delete logo file: {}", e.getMessage());
            }
        }
    }

    private void deleteFaviconFiles(OrganizationSettings settings) {
        // Delete file from resources
        if (settings.getFaviconUrl() != null && !settings.getFaviconUrl().isEmpty()) {
            try {
                String filename = settings.getFaviconUrl().substring(settings.getFaviconUrl().lastIndexOf('/') + 1);
                Path filePath = Paths.get(FAVICON_DIRECTORY, filename);
                Files.deleteIfExists(filePath);
                log.info("Deleted favicon file: {}", filePath);
            } catch (IOException e) {
                log.warn("Failed to delete favicon file: {}", e.getMessage());
            }
        }
    }

    @Transactional(readOnly = true)
    public byte[] getActiveLogoData() {
        OrganizationSettings settings = repository.findActiveDefault()
                .orElseThrow(() -> new RuntimeException("No active organization settings found"));
        return getLogoDataFromSettings(settings);
    }

    @Transactional(readOnly = true)
    public byte[] getLogoData(Long id) {
        OrganizationSettings settings = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Organization settings not found: " + id));
        return getLogoDataFromSettings(settings);
    }

    @Transactional(readOnly = true)
    public byte[] getActiveFaviconData() {
        OrganizationSettings settings = repository.findActiveDefault()
                .orElseThrow(() -> new RuntimeException("No active organization settings found"));
        return getFaviconDataFromSettings(settings);
    }

    @Transactional(readOnly = true)
    public byte[] getFaviconData(Long id) {
        OrganizationSettings settings = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Organization settings not found: " + id));
        return getFaviconDataFromSettings(settings);
    }

    private byte[] getLogoDataFromSettings(OrganizationSettings settings) {
        // Try database first
        if (settings.getLogoData() != null && settings.getLogoData().length > 0) {
            return settings.getLogoData();
        }

        // Try loading from file
        if (settings.getLogoUrl() != null && !settings.getLogoUrl().isEmpty()) {
            try {
                String filename = settings.getLogoUrl().substring(settings.getLogoUrl().lastIndexOf('/') + 1);
                Path filePath = Paths.get(LOGO_DIRECTORY, filename);
                if (Files.exists(filePath)) {
                    return Files.readAllBytes(filePath);
                }
            } catch (IOException e) {
                log.error("Failed to read logo file", e);
            }
        }

        return null;
    }

    private byte[] getFaviconDataFromSettings(OrganizationSettings settings) {
        // Try database first
        if (settings.getFaviconData() != null && settings.getFaviconData().length > 0) {
            return settings.getFaviconData();
        }

        // Try loading from file
        if (settings.getFaviconUrl() != null && !settings.getFaviconUrl().isEmpty()) {
            try {
                String filename = settings.getFaviconUrl().substring(settings.getFaviconUrl().lastIndexOf('/') + 1);
                Path filePath = Paths.get(FAVICON_DIRECTORY, filename);
                if (Files.exists(filePath)) {
                    return Files.readAllBytes(filePath);
                }
            } catch (IOException e) {
                log.error("Failed to read favicon file", e);
            }
        }

        return null;
    }

    @Transactional
    public OrganizationSettingsDto.Response setAsDefault(Long id) {
        OrganizationSettings settings = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Organization settings not found: " + id));

        // Clear all other defaults
        repository.clearAllDefaults();

        // Set this as default
        settings.setIsDefault(true);
        settings.setIsActive(true);
        settings = repository.save(settings);

        log.info("Set organization {} as default", id);
        return toResponse(settings);
    }

    private OrganizationSettingsDto.Response toResponse(OrganizationSettings settings) {
        // Get logo data for Base64 encoding
        byte[] logoData = getLogoDataFromSettings(settings);
        String logoBase64 = null;
        
        if (logoData != null && logoData.length > 0) {
            logoBase64 = java.util.Base64.getEncoder().encodeToString(logoData);
        }
        
        // Get favicon data for Base64 encoding
        byte[] faviconData = getFaviconDataFromSettings(settings);
        String faviconBase64 = null;
        
        if (faviconData != null && faviconData.length > 0) {
            faviconBase64 = java.util.Base64.getEncoder().encodeToString(faviconData);
        }
        
        return OrganizationSettingsDto.Response.builder()
                .id(settings.getId())
                .companyName(settings.getCompanyName())
                .companyNameEn(settings.getCompanyNameEn())
                .address(settings.getAddress())
                .phone(settings.getPhone())
                .email(settings.getEmail())
                .website(settings.getWebsite())
                .taxCode(settings.getTaxCode())
                .logoUrl(settings.getLogoUrl())
                .logoBase64(logoBase64)
                .hasLogo(logoData != null && logoData.length > 0)
                .faviconUrl(settings.getFaviconUrl())
                .faviconBase64(faviconBase64)
                .hasFavicon(faviconData != null && faviconData.length > 0)
                .watermarkText(settings.getWatermarkText())
                .isActive(settings.getIsActive())
                .isDefault(settings.getIsDefault())
                .createdAt(settings.getCreatedAt())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }
}
