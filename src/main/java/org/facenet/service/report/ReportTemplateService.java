package org.facenet.service.report;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.facenet.dto.report.ReportData;
import org.facenet.entity.report.*;
import org.facenet.entity.scale.Scale;
import org.facenet.entity.scale.WeighingLog;
import org.facenet.repository.report.OrganizationSettingsRepository;
import org.facenet.repository.report.ReportTemplateRepository;
import org.facenet.repository.scale.ScaleRepository;
import org.facenet.repository.scale.WeighingLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for loading and processing report templates dynamically
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportTemplateService {

    private final ReportTemplateRepository templateRepository;
    private final OrganizationSettingsRepository organizationRepository;
    private final WeighingLogRepository weighingLogRepository;
    private final ScaleRepository scaleRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Get template with columns by ID
     */
    @Transactional(readOnly = true)
    public ReportTemplate getTemplate(Long templateId) {
        return templateRepository.findByIdWithColumns(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));
    }

    /**
     * Get template by code
     */
    @Transactional(readOnly = true)
    public ReportTemplate getTemplateByCode(String code) {
        return templateRepository.findByCodeWithColumns(code)
                .orElseThrow(() -> new RuntimeException("Template not found: " + code));
    }

    /**
     * Get default template by type
     */
    @Transactional(readOnly = true)
    public ReportTemplate getDefaultTemplate(ReportTemplate.ReportType type) {
        return templateRepository.findByReportTypeAndIsDefaultTrue(type)
                .orElseGet(() -> {
                    // Fallback to any active template
                    List<ReportTemplate> templates = templateRepository.findByReportTypeAndIsActiveTrue(type);
                    if (templates.isEmpty()) {
                        throw new RuntimeException("No active template found for type: " + type);
                    }
                    return templates.get(0);
                });
    }

    /**
     * Get organization settings (with logo)
     */
    @Transactional(readOnly = true)
    public OrganizationSettings getOrganizationSettings() {
        return organizationRepository.findActiveDefault()
                .orElseGet(() -> {
                    log.warn("No default organization found, using fallback");
                    return OrganizationSettings.builder()
                            .companyName("CÔNG TY CỔ PHẦN SCALEHUB IOT")
                            .companyNameEn("ScaleHub IoT Corporation")
                            .watermarkText("HỆ THỐNG SCALEHUB")
                            .build();
                });
    }

    /**
     * Build report data based on template configuration
     */
    @Transactional(readOnly = true)
    public ReportData buildReportData(
            ReportTemplate template,
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            List<Long> scaleIds,
            String preparedBy
    ) {
        log.info("Building report data with template: {} for period {} to {}", 
                template.getCode(), startTime, endTime);

        OrganizationSettings org = getOrganizationSettings();

        // Fetch weighing logs
        List<WeighingLog> logs = weighingLogRepository.findAllInTimeRange(startTime, endTime);

        // Filter by scale IDs if specified
        if (scaleIds != null && !scaleIds.isEmpty()) {
            logs = logs.stream()
                    .filter(log -> scaleIds.contains(log.getScaleId()))
                    .collect(Collectors.toList());
        }

        log.info("Found {} weighing logs", logs.size());

        // Group data by scale
        Map<Long, List<WeighingLog>> logsByScale = logs.stream()
                .collect(Collectors.groupingBy(WeighingLog::getScaleId));

        // Fetch scale information
        List<Long> uniqueScaleIds = new ArrayList<>(logsByScale.keySet());
        List<Scale> scales = scaleRepository.findAllById(uniqueScaleIds);
        Map<Long, Scale> scaleMap = scales.stream()
                .collect(Collectors.toMap(Scale::getId, s -> s));

        // Build report rows based on template columns
        List<ReportData.ReportRow> rows = new ArrayList<>();
        int rowNumber = 1;

        for (Map.Entry<Long, List<WeighingLog>> entry : logsByScale.entrySet()) {
            Long scaleId = entry.getKey();
            List<WeighingLog> scaleLogs = entry.getValue();
            Scale scale = scaleMap.get(scaleId);

            if (scale == null) {
                log.warn("Scale {} not found, skipping", scaleId);
                continue;
            }

            ReportData.ReportRow row = buildRowFromTemplate(
                    template, scale, scaleLogs, rowNumber++);
            rows.add(row);
        }

        // Sort rows by scale ID
        rows.sort(Comparator.comparing(ReportData.ReportRow::getScaleId));

        // Calculate summary based on template
        ReportData.ReportSummary summary = calculateSummary(rows, template);

        // Build title from template
        String title = buildTitle(template, startTime, endTime);

        // Build metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("totalLogs", logs.size());
        metadata.put("dateRange", formatDateRange(startTime, endTime));
        metadata.put("organizationName", org.getCompanyName());
        metadata.put("watermark", org.getWatermarkText());
        metadata.put("template", template);

        return ReportData.builder()
                .reportTitle(title)
                .reportCode(template.getCode() + "-" + System.currentTimeMillis())
                .startTime(startTime)
                .endTime(endTime)
                .exportTime(OffsetDateTime.now())
                .preparedBy(preparedBy != null ? preparedBy : "System")
                .rows(rows)
                .summary(summary)
                .metadata(metadata)
                .build();
    }

    /**
     * Build row from template columns
     */
    private ReportData.ReportRow buildRowFromTemplate(
            ReportTemplate template,
            Scale scale,
            List<WeighingLog> logs,
            int rowNumber
    ) {
        ReportData.ReportRow.ReportRowBuilder builder = ReportData.ReportRow.builder()
                .rowNumber(rowNumber)
                .scaleId(scale.getId())
                .scaleCode("SCALE-" + scale.getId())
                .scaleName(scale.getName())
                .location(scale.getLocation() != null ? scale.getLocation().getName() : "N/A")
                .recordCount(logs.size())
                .lastTime(logs.stream()
                        .map(WeighingLog::getLastTime)
                        .max(OffsetDateTime::compareTo)
                        .orElse(null));

        // Calculate aggregations for data fields based on columns
        for (ReportColumn column : template.getColumns()) {
            if (column.getDataSource() == ReportColumn.DataSource.WEIGHING_DATA) {
                Double value = calculateAggregation(logs, column);
                
                // Map to appropriate field
                switch (column.getDataField()) {
                    case "data_1" -> builder.data1Total(value);
                    case "data_2" -> builder.data2Total(value);
                    case "data_3" -> builder.data3Total(value);
                    case "data_4" -> builder.data4Total(value);
                    case "data_5" -> builder.data5Total(value);
                }
            }
        }

        return builder.build();
    }

    /**
     * Calculate aggregation for a column
     */
    private Double calculateAggregation(List<WeighingLog> logs, ReportColumn column) {
        String field = column.getDataField();
        ReportColumn.AggregationType aggType = column.getAggregationType();

        List<Double> values = logs.stream()
                .map(log -> extractValue(log, field))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (values.isEmpty()) {
            return 0.0;
        }

        return switch (aggType) {
            case SUM -> values.stream().reduce(0.0, Double::sum);
            case AVG -> values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            case MAX -> values.stream().max(Double::compare).orElse(0.0);
            case MIN -> values.stream().min(Double::compare).orElse(0.0);
            case COUNT -> (double) values.size();
            case NONE -> values.isEmpty() ? 0.0 : values.get(0);
        };
    }

    /**
     * Extract value from weighing log by field name
     */
    private Double extractValue(WeighingLog log, String field) {
        String jsonbValue = switch (field) {
            case "data_1" -> log.getData1();
            case "data_2" -> log.getData2();
            case "data_3" -> log.getData3();
            case "data_4" -> log.getData4();
            case "data_5" -> log.getData5();
            default -> null;
        };

        return parseJsonbValue(jsonbValue);
    }

    /**
     * Parse JSONB string to Double
     */
    private Double parseJsonbValue(String jsonbValue) {
        if (jsonbValue == null || jsonbValue.trim().isEmpty()) {
            return 0.0;
        }

        try {
            String cleanValue = jsonbValue.replaceAll("\"", "").trim();
            return Double.parseDouble(cleanValue);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse JSONB value: {}", jsonbValue);
            return 0.0;
        }
    }

    /**
     * Calculate summary
     */
    private ReportData.ReportSummary calculateSummary(List<ReportData.ReportRow> rows, ReportTemplate template) {
        if (rows.isEmpty()) {
            return ReportData.ReportSummary.builder()
                    .totalScales(0)
                    .totalRecords(0)
                    .data1GrandTotal(0.0)
                    .data2GrandTotal(0.0)
                    .data3GrandTotal(0.0)
                    .data4GrandTotal(0.0)
                    .data5GrandTotal(0.0)
                    .data1Average(0.0)
                    .data2Average(0.0)
                    .data3Average(0.0)
                    .data4Average(0.0)
                    .data5Average(0.0)
                    .data1Max(0.0)
                    .data2Max(0.0)
                    .data3Max(0.0)
                    .data4Max(0.0)
                    .data5Max(0.0)
                    .build();
        }

        return ReportData.ReportSummary.builder()
                .totalScales(rows.size())
                .totalRecords(rows.stream().mapToInt(ReportData.ReportRow::getRecordCount).sum())
                .data1GrandTotal(rows.stream().mapToDouble(r -> r.getData1Total() != null ? r.getData1Total() : 0).sum())
                .data2GrandTotal(rows.stream().mapToDouble(r -> r.getData2Total() != null ? r.getData2Total() : 0).sum())
                .data3GrandTotal(rows.stream().mapToDouble(r -> r.getData3Total() != null ? r.getData3Total() : 0).sum())
                .data4GrandTotal(rows.stream().mapToDouble(r -> r.getData4Total() != null ? r.getData4Total() : 0).sum())
                .data5GrandTotal(rows.stream().mapToDouble(r -> r.getData5Total() != null ? r.getData5Total() : 0).sum())
                .data1Average(rows.stream().mapToDouble(r -> r.getData1Total() != null ? r.getData1Total() : 0).average().orElse(0))
                .data2Average(rows.stream().mapToDouble(r -> r.getData2Total() != null ? r.getData2Total() : 0).average().orElse(0))
                .data3Average(rows.stream().mapToDouble(r -> r.getData3Total() != null ? r.getData3Total() : 0).average().orElse(0))
                .data4Average(rows.stream().mapToDouble(r -> r.getData4Total() != null ? r.getData4Total() : 0).average().orElse(0))
                .data5Average(rows.stream().mapToDouble(r -> r.getData5Total() != null ? r.getData5Total() : 0).average().orElse(0))
                .data1Max(rows.stream().mapToDouble(r -> r.getData1Total() != null ? r.getData1Total() : 0).max().orElse(0))
                .data2Max(rows.stream().mapToDouble(r -> r.getData2Total() != null ? r.getData2Total() : 0).max().orElse(0))
                .data3Max(rows.stream().mapToDouble(r -> r.getData3Total() != null ? r.getData3Total() : 0).max().orElse(0))
                .data4Max(rows.stream().mapToDouble(r -> r.getData4Total() != null ? r.getData4Total() : 0).max().orElse(0))
                .data5Max(rows.stream().mapToDouble(r -> r.getData5Total() != null ? r.getData5Total() : 0).max().orElse(0))
                .build();
    }

    /**
     * Build title from template
     */
    private String buildTitle(ReportTemplate template, OffsetDateTime startTime, OffsetDateTime endTime) {
        String titleTemplate = template.getTitleTemplate();
        if (titleTemplate == null) {
            return "BÁO CÁO SẢN LƯỢNG TRẠM CÂN";
        }

        String dateRange = formatDateRange(startTime, endTime);
        return titleTemplate.replace("{{dateRange}}", dateRange)
                .replace("{{startDate}}", DATE_FORMATTER.format(startTime))
                .replace("{{endDate}}", DATE_FORMATTER.format(endTime));
    }

    /**
     * Format date range
     */
    private String formatDateRange(OffsetDateTime start, OffsetDateTime end) {
        return String.format("%s đến %s", 
                DATE_FORMATTER.format(start), 
                DATE_FORMATTER.format(end));
    }

    /**
     * Get all active templates
     */
    @Transactional(readOnly = true)
    public List<ReportTemplate> getAllActiveTemplates() {
        return templateRepository.findByIsActiveTrueOrderByReportTypeAscNameAsc();
    }

    /**
     * Get templates by type
     */
    @Transactional(readOnly = true)
    public List<ReportTemplate> getTemplatesByType(ReportTemplate.ReportType type) {
        return templateRepository.findByReportTypeAndIsActiveTrue(type);
    }
}
