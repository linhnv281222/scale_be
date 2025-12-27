package org.facenet.service.report;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.facenet.entity.report.ReportDefinition;
import org.facenet.repository.report.ReportDefinitionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing Report Definitions
 * Central registry of all available enterprise reports
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportDefinitionService {

    private final ReportDefinitionRepository reportDefinitionRepository;

    /**
     * Get report definition by code
     * @throws IllegalArgumentException if report not found
     */
    @Transactional(readOnly = true)
    public ReportDefinition getByCode(String reportCode) {
        log.debug("Fetching report definition for code: {}", reportCode);
        return reportDefinitionRepository.findByReportCode(reportCode)
                .orElseThrow(() -> new IllegalArgumentException("Report definition not found: " + reportCode));
    }

    /**
     * Get report definition by code (optional)
     */
    @Transactional(readOnly = true)
    public Optional<ReportDefinition> findByCode(String reportCode) {
        return reportDefinitionRepository.findByReportCode(reportCode);
    }

    /**
     * Get all executable reports (ACTIVE status)
     */
    @Transactional(readOnly = true)
    public List<ReportDefinition> getExecutableReports() {
        return reportDefinitionRepository.findExecutableReports();
    }

    /**
     * Get reports by category
     */
    @Transactional(readOnly = true)
    public List<ReportDefinition> getReportsByCategory(String category) {
        return reportDefinitionRepository.findByCategoryAndStatusOrderByDisplayOrderAsc(
                category, ReportDefinition.ReportStatus.ACTIVE);
    }

    /**
     * Check if report code exists
     */
    @Transactional(readOnly = true)
    public boolean exists(String reportCode) {
        return reportDefinitionRepository.existsByReportCode(reportCode);
    }

    /**
     * Validate report can be executed
     * @throws IllegalStateException if report is not executable
     */
    public void validateExecutable(String reportCode) {
        ReportDefinition definition = getByCode(reportCode);
        
        if (!definition.isExecutable()) {
            throw new IllegalStateException(
                    String.format("Report %s is not executable. Current status: %s", 
                            reportCode, definition.getStatus()));
        }
        
        log.debug("Report {} validated for execution", reportCode);
    }

    /**
     * Validate report supports format
     * @throws IllegalArgumentException if format not supported
     */
    public void validateFormat(String reportCode, String format) {
        ReportDefinition definition = getByCode(reportCode);
        
        if (!definition.supportsFormat(format)) {
            throw new IllegalArgumentException(
                    String.format("Report %s does not support format %s. Supported formats: %s", 
                            reportCode, format, String.join(", ", definition.getSupportedFormats())));
        }
        
        log.debug("Format {} validated for report {}", format, reportCode);
    }

    /**
     * Create or update report definition
     */
    @Transactional
    public ReportDefinition save(ReportDefinition definition) {
        log.info("Saving report definition: {}", definition.getReportCode());
        return reportDefinitionRepository.save(definition);
    }

    /**
     * Activate report
     */
    @Transactional
    public void activate(String reportCode) {
        ReportDefinition definition = getByCode(reportCode);
        definition.setStatus(ReportDefinition.ReportStatus.ACTIVE);
        reportDefinitionRepository.save(definition);
        log.info("Report {} activated", reportCode);
    }

    /**
     * Deactivate report
     */
    @Transactional
    public void deactivate(String reportCode) {
        ReportDefinition definition = getByCode(reportCode);
        definition.setStatus(ReportDefinition.ReportStatus.INACTIVE);
        reportDefinitionRepository.save(definition);
        log.info("Report {} deactivated", reportCode);
    }

    /**
     * Get all reports (including inactive)
     */
    @Transactional(readOnly = true)
    public List<ReportDefinition> getAllReports() {
        return reportDefinitionRepository.findAll();
    }

    /**
     * Get reports supporting specific format
     */
    @Transactional(readOnly = true)
    public List<ReportDefinition> getReportsByFormat(String format) {
        return reportDefinitionRepository.findByFormat(format);
    }
}
