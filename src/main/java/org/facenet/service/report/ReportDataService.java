package org.facenet.service.report;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.facenet.dto.report.ReportData;
import org.facenet.dto.report.ReportExportRequest;
import org.facenet.entity.scale.Scale;
import org.facenet.entity.scale.ScaleConfig;
import org.facenet.entity.scale.WeighingLog;
import org.facenet.repository.scale.ScaleConfigRepository;
import org.facenet.repository.scale.ScaleRepository;
import org.facenet.repository.scale.WeighingLogRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for processing report data
 * Handles data type conversion from JSONB strings to numbers
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportDataService {

    private final WeighingLogRepository weighingLogRepository;
    private final ScaleRepository scaleRepository;
    private final ScaleConfigRepository scaleConfigRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ReportAggregationService aggregationService;

    /**
     * Fetch and process report data based on request
     * ENHANCED: Supports aggregation, time intervals, and flexible filtering
     */
    @Transactional(readOnly = true)
    public ReportData fetchReportData(ReportExportRequest request) {
        log.info("Fetching report data: period={} to {}, aggregation={}, interval={}", 
                request.getStartTime(), request.getEndTime(), 
                request.getAggregationMethod(), request.getTimeInterval());

        // If aggregation method or time interval is specified, use aggregation service
        if (request.getAggregationMethod() != ReportExportRequest.AggregationMethod.SUM || 
            request.getTimeInterval() != null) {
            
            log.info("Using aggregation service for advanced filtering");
            
            // Choose strategy based on interval
            if (request.getTimeInterval() != null && 
                (request.getTimeInterval() == ReportExportRequest.TimeInterval.WEEK ||
                 request.getTimeInterval() == ReportExportRequest.TimeInterval.MONTH ||
                 request.getTimeInterval() == ReportExportRequest.TimeInterval.YEAR)) {
                // Use pre-aggregated data
                return aggregationService.fetchAggregatedReportData(request);
            } else {
                // Use ad-hoc aggregation
                return aggregationService.fetchAdHocAggregatedData(request);
            }
        }
        
        // LEGACY PATH: Simple SUM without intervals (backward compatible)
        log.info("Using legacy simple SUM path for backward compatibility");
        return fetchSimpleReportData(request);
    }
    
    /**
     * LEGACY: Fetch simple report data (for backward compatibility)
     * Used when no special aggregation or interval is requested
     */
    @Transactional(readOnly = true)
    private ReportData fetchSimpleReportData(ReportExportRequest request) {
        log.info("Fetching simple report data for period {} to {}", request.getStartTime(), request.getEndTime());

        // Fetch weighing logs
        List<WeighingLog> logs = weighingLogRepository.findAllInTimeRange(
                request.getStartTime(),
                request.getEndTime()
        );

        // Filter by scale IDs if specified
        if (request.getScaleIds() != null && !request.getScaleIds().isEmpty()) {
            logs = logs.stream()
                    .filter(log -> request.getScaleIds().contains(log.getScaleId()))
                    .collect(Collectors.toList());
        }

        log.info("Found {} weighing logs", logs.size());
        
        // Log sample data for debugging
        if (!logs.isEmpty()) {
            WeighingLog sample = logs.get(0);
            log.info("Sample weighing log: scaleId={}, data1='{}', data2='{}', data3='{}', data4='{}', data5='{}'",
                    sample.getScaleId(), 
                    sample.getData1(), 
                    sample.getData2(), 
                    sample.getData3(), 
                    sample.getData4(), 
                    sample.getData5());
        }

        // Group data by scale
        Map<Long, List<WeighingLog>> logsByScale = logs.stream()
                .collect(Collectors.groupingBy(WeighingLog::getScaleId));

        // Fetch scale information
        List<Long> scaleIds = new ArrayList<>(logsByScale.keySet());
        List<Scale> scales = scaleRepository.findAllById(scaleIds);
        Map<Long, Scale> scaleMap = scales.stream()
                .collect(Collectors.toMap(Scale::getId, s -> s));

        // Build report rows
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

            // Calculate aggregations
            ReportData.ReportRow row = ReportData.ReportRow.builder()
                    .rowNumber(rowNumber++)
                    .scaleId(scaleId)
                    .scaleCode("SCALE-" + scaleId)
                    .scaleName(scale.getName())
                    .location(scale.getLocation() != null ? scale.getLocation().getName() : "N/A")
                    .data1Total(calculateSum(scaleLogs, WeighingLog::getData1))
                    .data2Total(calculateSum(scaleLogs, WeighingLog::getData2))
                    .data3Total(calculateSum(scaleLogs, WeighingLog::getData3))
                    .data4Total(calculateSum(scaleLogs, WeighingLog::getData4))
                    .data5Total(calculateSum(scaleLogs, WeighingLog::getData5))
                    .recordCount(scaleLogs.size())
                    .lastTime(scaleLogs.stream()
                            .map(WeighingLog::getLastTime)
                            .max(OffsetDateTime::compareTo)
                            .orElse(null))
                    .build();

            rows.add(row);
        }

        // Sort rows by scale ID
        rows.sort(Comparator.comparing(ReportData.ReportRow::getScaleId));

        // Calculate summary
        ReportData.ReportSummary summary = calculateSummary(rows);

        // Build report metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("totalLogs", logs.size());
        metadata.put("dateRange", formatDateRange(request.getStartTime(), request.getEndTime()));
        
        // Get column names from first scale config (assuming all scales have same config structure)
        String[] columnNames = getColumnNamesFromConfig(scaleIds.isEmpty() ? null : scaleIds.get(0));

        return ReportData.builder()
                .reportTitle(request.getReportTitle() != null ? request.getReportTitle() : "BÁO CÁO SẢN LƯỢNG TRẠM CÂN")
                .reportCode(request.getReportCode() != null ? request.getReportCode() : "BCSL-" + System.currentTimeMillis())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .exportTime(OffsetDateTime.now())
                .preparedBy(request.getPreparedBy() != null ? request.getPreparedBy() : "System")
                .rows(rows)
                .summary(summary)
                .metadata(metadata)
                .data1Name(columnNames[0])
                .data2Name(columnNames[1])
                .data3Name(columnNames[2])
                .data4Name(columnNames[3])
                .data5Name(columnNames[4])
                .build();
    }

    /**
     * Convert JSONB string to Double
     * Handles multiple formats:
     * - Plain string: "150.5" (most common)
     * - JSON string: "\"150.5\"" (quoted)
     * - Numeric: 150.5 (direct number)
     * Also handles null, empty, and non-numeric values
     */
    private Double parseJsonbValue(String jsonbValue) {
        if (jsonbValue == null || jsonbValue.trim().isEmpty() || jsonbValue.equalsIgnoreCase("null")) {
            return 0.0;
        }

        try {
            String cleanValue = jsonbValue.trim();
            
            // Remove JSON quotes if present: \"123\" -> 123
            if (cleanValue.startsWith("\\\"") && cleanValue.endsWith("\\\"")) {
                cleanValue = cleanValue.substring(2, cleanValue.length() - 2);
            }
            // Remove regular quotes: "123" -> 123
            else if (cleanValue.startsWith("\"") && cleanValue.endsWith("\"")) {
                cleanValue = cleanValue.substring(1, cleanValue.length() - 1);
            }
            
            cleanValue = cleanValue.trim();
            if (cleanValue.isEmpty()) {
                return 0.0;
            }
            
            double result = Double.parseDouble(cleanValue);
            if (log.isTraceEnabled()) {
                log.trace("Parsed '{}' -> {}", jsonbValue, result);
            }
            return result;
        } catch (NumberFormatException e) {
            log.warn("Failed to parse JSONB value: '{}' - returning 0.0", jsonbValue);
            return 0.0;
        }
    }

    /**
     * Calculate sum of data field across logs
     */
    private Double calculateSum(List<WeighingLog> logs, java.util.function.Function<WeighingLog, String> dataExtractor) {
        double sum = logs.stream()
                .map(dataExtractor)
                .peek(value -> {
                    if (log.isDebugEnabled() || true) {
                        log.debug("Raw data value from weighing_log: '{}'", value);
                    }
                })
                .map(this::parseJsonbValue)
                .peek(value -> {
                    if (log.isDebugEnabled()) {
                        log.debug("Parsed to Double: {}", value);
                    }
                })
                .reduce(0.0, Double::sum);
        
        log.info("Sum calculation result: {} from {} logs", sum, logs.size());
        return sum;
    }

    /**
     * Calculate average of data field
     */
    private Double calculateAverage(List<ReportData.ReportRow> rows, java.util.function.Function<ReportData.ReportRow, Double> dataExtractor) {
        if (rows.isEmpty()) {
            return 0.0;
        }
        return rows.stream()
                .map(dataExtractor)
                .filter(Objects::nonNull)
                .reduce(0.0, Double::sum) / rows.size();
    }

    /**
     * Calculate maximum of data field
     */
    private Double calculateMax(List<ReportData.ReportRow> rows, java.util.function.Function<ReportData.ReportRow, Double> dataExtractor) {
        return rows.stream()
                .map(dataExtractor)
                .filter(Objects::nonNull)
                .max(Double::compareTo)
                .orElse(0.0);
    }

    /**
     * Calculate report summary
     */
    private ReportData.ReportSummary calculateSummary(List<ReportData.ReportRow> rows) {
        return ReportData.ReportSummary.builder()
                .totalScales(rows.size())
                .totalRecords(rows.stream().mapToInt(ReportData.ReportRow::getRecordCount).sum())
                .data1GrandTotal(rows.stream().map(ReportData.ReportRow::getData1Total).reduce(0.0, Double::sum))
                .data2GrandTotal(rows.stream().map(ReportData.ReportRow::getData2Total).reduce(0.0, Double::sum))
                .data3GrandTotal(rows.stream().map(ReportData.ReportRow::getData3Total).reduce(0.0, Double::sum))
                .data4GrandTotal(rows.stream().map(ReportData.ReportRow::getData4Total).reduce(0.0, Double::sum))
                .data5GrandTotal(rows.stream().map(ReportData.ReportRow::getData5Total).reduce(0.0, Double::sum))
                .data1Average(calculateAverage(rows, ReportData.ReportRow::getData1Total))
                .data2Average(calculateAverage(rows, ReportData.ReportRow::getData2Total))
                .data3Average(calculateAverage(rows, ReportData.ReportRow::getData3Total))
                .data4Average(calculateAverage(rows, ReportData.ReportRow::getData4Total))
                .data5Average(calculateAverage(rows, ReportData.ReportRow::getData5Total))
                .data1Max(calculateMax(rows, ReportData.ReportRow::getData1Total))
                .data2Max(calculateMax(rows, ReportData.ReportRow::getData2Total))
                .data3Max(calculateMax(rows, ReportData.ReportRow::getData3Total))
                .data4Max(calculateMax(rows, ReportData.ReportRow::getData4Total))
                .data5Max(calculateMax(rows, ReportData.ReportRow::getData5Total))
                .build();
    }

    /**
     * Format date range for display
     */
    private String formatDateRange(OffsetDateTime start, OffsetDateTime end) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return formatter.format(start) + " - " + formatter.format(end);
    }
    
    /**
     * Get column names from scale configs
     * Tries to get from first scale with config, falls back to defaults
     * Returns array of 5 names for data_1 to data_5
     */
    private String[] getColumnNamesFromConfig(Long sampleScaleId) {
        String[] defaultNames = {"Khối lượng (kg)", "Nhiệt độ (°C)", "Độ ẩm (%)", "Áp suất (hPa)", "Tốc độ (m/s)"};
        
        if (sampleScaleId == null) {
            return defaultNames;
        }
        
        try {
            // Try to get config from the provided scale
            Optional<ScaleConfig> configOpt = scaleConfigRepository.findById(sampleScaleId);
            if (configOpt.isEmpty()) {
                log.debug("No config found for scale {}, using defaults", sampleScaleId);
                return defaultNames;
            }
            
            ScaleConfig config = configOpt.get();
            String[] names = new String[5];
            
            // Extract name from each data_n JSON: {"name": "Khối lượng", "data_type": "FLOAT"}
            names[0] = extractNameFromDataConfig(config.getData1(), defaultNames[0]);
            names[1] = extractNameFromDataConfig(config.getData2(), defaultNames[1]);
            names[2] = extractNameFromDataConfig(config.getData3(), defaultNames[2]);
            names[3] = extractNameFromDataConfig(config.getData4(), defaultNames[3]);
            names[4] = extractNameFromDataConfig(config.getData5(), defaultNames[4]);
            
            log.debug("Loaded column names from scale {}: {}", sampleScaleId, String.join(", ", names));
            return names;
        } catch (Exception e) {
            log.warn("Failed to get column names from config for scale {}: {}", sampleScaleId, e.getMessage());
            return defaultNames;
        }
    }
    
    /**
     * Extract name from data config JSON
     */
    private String extractNameFromDataConfig(Map<String, Object> dataConfig, String defaultName) {
        if (dataConfig == null || !dataConfig.containsKey("name")) {
            return defaultName;
        }
        
        Object nameObj = dataConfig.get("name");
        return nameObj != null ? nameObj.toString() : defaultName;
    }

    /**
     * Round to specified decimal places
     */
    public static Double round(Double value, int places) {
        if (value == null) {
            return 0.0;
        }
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }
}
