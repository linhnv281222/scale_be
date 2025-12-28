package org.facenet.service.report;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.facenet.dto.report.ReportData;
import org.facenet.dto.report.ReportExportRequest;
import org.facenet.entity.scale.Scale;
import org.facenet.entity.scale.ScaleConfig;
import org.facenet.repository.scale.ScaleConfigRepository;
import org.facenet.repository.scale.ScaleRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for advanced report aggregation with time intervals
 * Implements smart query strategy: ad-hoc vs pre-aggregated
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportAggregationService {

    private final JdbcTemplate jdbcTemplate;
    private final ScaleRepository scaleRepository;
    private final ScaleConfigRepository scaleConfigRepository;

    /**
     * Fetch aggregated report data with time intervals
     * Uses pre-aggregated data (scale_daily_reports) for WEEK/MONTH/YEAR
     */
    @Transactional(readOnly = true)
    public ReportData fetchAggregatedReportData(ReportExportRequest request) {
        log.info("Fetching aggregated report: interval={}, method={}", 
                request.getTimeInterval(), request.getAggregationMethod());

        // Build and execute SQL query
        String sql = buildAggregatedQuery(request);
        log.debug("Aggregated query: {}", sql);
        
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
        
        // Convert to ReportData
        return buildReportDataFromAggregated(results, request);
    }

    /**
     * Fetch ad-hoc report data with aggregation
     * Queries weighing_logs directly for HOUR/DAY intervals or no interval
     */
    @Transactional(readOnly = true)
    public ReportData fetchAdHocAggregatedData(ReportExportRequest request) {
        log.info("Fetching ad-hoc aggregated report: interval={}, method={}", 
                request.getTimeInterval(), request.getAggregationMethod());

        String sql = buildAdHocQuery(request);
        log.debug("Ad-hoc query: {}", sql);
        
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
        
        return buildReportDataFromAggregated(results, request);
    }

    /**
     * Build SQL query for pre-aggregated reports (scale_daily_reports)
     */
    private String buildAggregatedQuery(ReportExportRequest request) {
        String scaleIdsStr = buildScaleIdsFilter(request);
        String dateTruncParam = getDateTruncParameter(request.getTimeInterval());
        
        List<String> dataFields = getDataFields(request);
        StringBuilder selectColumns = new StringBuilder();
        selectColumns.append("scale_id, ");
        selectColumns.append("DATE_TRUNC('").append(dateTruncParam).append("', date) as time_bucket, ");
        
        for (int i = 0; i < dataFields.size(); i++) {
            String field = dataFields.get(i);
            selectColumns.append(buildAggregationExpression(request.getAggregationMethod(), field))
                        .append(" as ").append(field).append("_agg");
            if (i < dataFields.size() - 1) selectColumns.append(", ");
        }
        
        return String.format("""
                SELECT %s
                FROM scale_daily_reports
                WHERE scale_id IN (%s)
                    AND date BETWEEN '%s' AND '%s'
                GROUP BY scale_id, DATE_TRUNC('%s', date)
                ORDER BY scale_id, time_bucket
                """,
                selectColumns.toString(),
                scaleIdsStr,
                request.getStartTime().toLocalDate(),
                request.getEndTime().toLocalDate(),
                dateTruncParam
        );
    }

    /**
     * Build SQL query for ad-hoc reports (weighing_logs)
     */
    private String buildAdHocQuery(ReportExportRequest request) {
        String scaleIdsStr = buildScaleIdsFilter(request);
        String dateTruncParam = request.getTimeInterval() != null 
                ? getDateTruncParameter(request.getTimeInterval()) 
                : "day";
        
        List<String> dataFields = getDataFields(request);
        StringBuilder selectColumns = new StringBuilder();
        selectColumns.append("scale_id, ");
        
        if (request.getTimeInterval() != null) {
            selectColumns.append("DATE_TRUNC('").append(dateTruncParam).append("', created_at) as time_bucket, ");
        }
        
        for (int i = 0; i < dataFields.size(); i++) {
            String field = dataFields.get(i);
            selectColumns.append(buildAggregationExpression(request.getAggregationMethod(), field))
                        .append(" as ").append(field).append("_agg");
            if (i < dataFields.size() - 1) selectColumns.append(", ");
        }
        
        String groupBy = request.getTimeInterval() != null
                ? "GROUP BY scale_id, DATE_TRUNC('" + dateTruncParam + "', created_at)"
                : "GROUP BY scale_id";
        
        return String.format("""
                SELECT %s
                FROM weighing_logs
                WHERE scale_id IN (%s)
                    AND created_at BETWEEN '%s' AND '%s'
                %s
                ORDER BY scale_id%s
                """,
                selectColumns.toString(),
                scaleIdsStr,
                request.getStartTime(),
                request.getEndTime(),
                groupBy,
                request.getTimeInterval() != null ? ", time_bucket" : ""
        );
    }

    /**
     * Build aggregation expression for SQL
     */
    private String buildAggregationExpression(ReportExportRequest.AggregationMethod method, String dataField) {
        // Ensure dataField is a valid column name (data_1 to data_5)
        String normalizedField = normalizeDataFieldName(dataField);
        
        // Cast JSONB string to NUMERIC, handle invalid values as 0
        // TRIM removes any whitespace, NULLIF converts empty strings to NULL, COALESCE converts NULL to 0
        String castExpression = String.format(
                "COALESCE(NULLIF(TRIM(%s), '')::NUMERIC, 0)",
                normalizedField
        );
        
        return switch (method) {
            case SUM -> String.format("SUM(%s)", castExpression);
            case AVG -> String.format("AVG(%s)", castExpression);
            case MAX -> String.format("MAX(%s)", castExpression);
            case MIN -> String.format("MIN(%s)", castExpression);
            case COUNT -> "COUNT(*)";
        };
    }
    
    /**
     * Normalize data field name to proper column name in database
     * Ensures the field name is one of: data_1, data_2, data_3, data_4, data_5
     */
    private String normalizeDataFieldName(String dataField) {
        if (dataField == null || dataField.trim().isEmpty()) {
            log.warn("Data field is null or empty, defaulting to data_1");
            return "data_1";
        }
        
        String normalized = dataField.toLowerCase().trim();
        
        // Already in correct format: data_1, data_2, etc.
        if (normalized.matches("data_[1-5]")) {
            return normalized;
        }
        
        // Format: data1, data2 -> data_1, data_2
        if (normalized.matches("data[1-5]")) {
            return normalized.replace("data", "data_");
        }
        
        // Just a number: 1, 2, 3, 4, 5 -> data_1, data_2, etc.
        if (normalized.matches("[1-5]")) {
            return "data_" + normalized;
        }
        
        // Invalid format - log warning and default to data_1
        log.warn("Invalid data field '{}', defaulting to data_1", dataField);
        return "data_1";
    }

    /**
     * Build scale IDs filter
     */
    private String buildScaleIdsFilter(ReportExportRequest request) {
        if (request.getScaleIds() != null && !request.getScaleIds().isEmpty()) {
            return request.getScaleIds().toString().replaceAll("[\\[\\]]", "");
        }
        // If no scale IDs specified, get all active scales
        return "(SELECT id FROM scales WHERE is_active = true)";
    }

    /**
     * Get data fields list
     */
    private List<String> getDataFields(ReportExportRequest request) {
        if (request.getDataFields() != null && !request.getDataFields().isEmpty()) {
            return request.getDataFields();
        }
        return List.of("data_1", "data_2", "data_3", "data_4", "data_5");
    }

    /**
     * Get date_trunc parameter based on interval
     */
    private String getDateTruncParameter(ReportExportRequest.TimeInterval interval) {
        if (interval == null) return "day";
        return switch (interval) {
            case HOUR -> "hour";
            case DAY -> "day";
            case WEEK -> "week";
            case MONTH -> "month";
            case YEAR -> "year";
        };
    }

    /**
     * Build ReportData from aggregated query results
     */
    private ReportData buildReportDataFromAggregated(List<Map<String, Object>> results, ReportExportRequest request) {
        // Group by scale_id
        Map<Long, List<Map<String, Object>>> groupedByScale = results.stream()
                .collect(Collectors.groupingBy(row -> ((Number) row.get("scale_id")).longValue()));
        
        // Fetch scale information
        List<Long> scaleIds = new ArrayList<>(groupedByScale.keySet());
        List<Scale> scales = scaleRepository.findAllById(scaleIds);
        Map<Long, Scale> scaleMap = scales.stream()
                .collect(Collectors.toMap(Scale::getId, s -> s));
        
        // Build report rows
        List<ReportData.ReportRow> rows = new ArrayList<>();
        int rowNumber = 1;
        
        for (Map.Entry<Long, List<Map<String, Object>>> entry : groupedByScale.entrySet()) {
            Long scaleId = entry.getKey();
            Scale scale = scaleMap.get(scaleId);
            if (scale == null) continue;
            
            List<Map<String, Object>> scaleData = entry.getValue();
            
            ReportData.ReportRow row = ReportData.ReportRow.builder()
                    .rowNumber(rowNumber++)
                    .scaleId(scaleId)
                    .scaleCode("SCALE-" + scaleId)
                    .scaleName(scale.getName())
                    .location(scale.getLocation() != null ? scale.getLocation().getName() : "N/A")
                    .data1Total(sumAggregatedField(scaleData, "data_1_agg"))
                    .data2Total(sumAggregatedField(scaleData, "data_2_agg"))
                    .data3Total(sumAggregatedField(scaleData, "data_3_agg"))
                    .data4Total(sumAggregatedField(scaleData, "data_4_agg"))
                    .data5Total(sumAggregatedField(scaleData, "data_5_agg"))
                    .recordCount(scaleData.size())
                    .build();
            
            rows.add(row);
        }
        
        rows.sort(Comparator.comparing(ReportData.ReportRow::getScaleId));
        
        // Calculate summary
        ReportData.ReportSummary summary = calculateSummary(rows);
        
        // Get column names
        String[] columnNames = getColumnNamesFromConfig(scaleIds.isEmpty() ? null : scaleIds.get(0));
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("aggregationMethod", request.getAggregationMethod().toString());
        metadata.put("timeInterval", request.getTimeInterval() != null ? request.getTimeInterval().toString() : "NONE");
        metadata.put("totalDataPoints", results.size());
        metadata.put("scaleCount", scaleIds.size());
        
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
     * Sum aggregated field from results
     */
    private Double sumAggregatedField(List<Map<String, Object>> data, String fieldName) {
        return data.stream()
                .map(row -> row.get(fieldName))
                .filter(Objects::nonNull)
                .mapToDouble(val -> ((Number) val).doubleValue())
                .sum();
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

    private Double calculateAverage(List<ReportData.ReportRow> rows, java.util.function.Function<ReportData.ReportRow, Double> extractor) {
        if (rows.isEmpty()) return 0.0;
        return rows.stream().map(extractor).filter(Objects::nonNull).reduce(0.0, Double::sum) / rows.size();
    }

    private Double calculateMax(List<ReportData.ReportRow> rows, java.util.function.Function<ReportData.ReportRow, Double> extractor) {
        return rows.stream().map(extractor).filter(Objects::nonNull).max(Double::compareTo).orElse(0.0);
    }

    /**
     * Get column names from scale config
     */
    private String[] getColumnNamesFromConfig(Long sampleScaleId) {
        String[] defaultNames = {"Khối lượng (kg)", "Nhiệt độ (°C)", "Độ ẩm (%)", "Áp suất (hPa)", "Tốc độ (m/s)"};
        
        if (sampleScaleId == null) {
            return defaultNames;
        }
        
        try {
            Optional<ScaleConfig> configOpt = scaleConfigRepository.findById(sampleScaleId);
            if (configOpt.isEmpty()) {
                return defaultNames;
            }
            
            ScaleConfig config = configOpt.get();
            String[] names = new String[5];
            
            names[0] = extractNameFromDataConfig(config.getData1(), defaultNames[0]);
            names[1] = extractNameFromDataConfig(config.getData2(), defaultNames[1]);
            names[2] = extractNameFromDataConfig(config.getData3(), defaultNames[2]);
            names[3] = extractNameFromDataConfig(config.getData4(), defaultNames[3]);
            names[4] = extractNameFromDataConfig(config.getData5(), defaultNames[4]);
            
            return names;
        } catch (Exception e) {
            log.warn("Failed to get column names from config: {}", e.getMessage());
            return defaultNames;
        }
    }

    private String extractNameFromDataConfig(Map<String, Object> dataConfig, String defaultName) {
        if (dataConfig == null || !dataConfig.containsKey("name")) {
            return defaultName;
        }
        
        Object nameObj = dataConfig.get("name");
        return nameObj != null ? nameObj.toString() : defaultName;
    }
}
