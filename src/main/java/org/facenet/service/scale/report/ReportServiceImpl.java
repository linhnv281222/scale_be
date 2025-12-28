package org.facenet.service.scale.report;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.facenet.dto.scale.ReportRequestDto;
import org.facenet.dto.scale.ReportResponseDto;
import org.facenet.entity.scale.ScaleDailyReport;
import org.facenet.repository.scale.ScaleDailyReportRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implementation of ReportService
 * Handles both ad-hoc and pre-aggregated reports
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final JdbcTemplate jdbcTemplate;
    private final ScaleDailyReportRepository dailyReportRepository;

    @Override
    public ReportResponseDto generateReport(ReportRequestDto request) {
        log.info("[REPORT] Generating report: method={}, interval={}, field={}, scaleIds={}",
                request.getMethod(), request.getInterval(), request.getDataField(), request.getScaleIds());

        // Choose report flow based on interval
        List<ReportResponseDto.DataPoint> dataPoints = switch (request.getInterval()) {
            case HOUR, DAY -> generateAdHocReport(request);
            case WEEK, MONTH, YEAR -> generatePreAggregatedReport(request);
        };

        return ReportResponseDto.builder()
                .reportName(buildReportName(request))
                .method(request.getMethod().name())
                .dataField(request.getDataField())
                .interval(request.getInterval().name())
                .dataPoints(dataPoints)
                .build();
    }

    /**
     * Generate ad-hoc report by querying weighing_logs directly
     * Used for HOUR and DAY intervals
     */
    private List<ReportResponseDto.DataPoint> generateAdHocReport(ReportRequestDto request) {
        String sql = buildAdHocQuery(request);
        log.debug("[REPORT] Ad-hoc query: {}", sql);

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                ReportResponseDto.DataPoint.builder()
                        .time(rs.getString("time_bucket"))
                        .value(rs.getDouble("aggregated_value"))
                        .build()
        );
    }

    /**
     * Generate pre-aggregated report by querying scale_daily_reports
     * Used for WEEK, MONTH, and YEAR intervals
     */
    private List<ReportResponseDto.DataPoint> generatePreAggregatedReport(ReportRequestDto request) {
        String sql = buildPreAggregatedQuery(request);
        log.debug("[REPORT] Pre-aggregated query: {}", sql);

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                ReportResponseDto.DataPoint.builder()
                        .time(rs.getString("time_bucket"))
                        .value(rs.getDouble("aggregated_value"))
                        .build()
        );
    }

    /**
     * Build SQL query for ad-hoc reports
     * Query weighing_logs directly and cast string data to numeric
     */
    private String buildAdHocQuery(ReportRequestDto request) {
        String scaleIdsStr = request.getScaleIds().toString().replaceAll("[\\[\\]]", "");
        String dateTruncParam = getDateTruncParameter(request.getInterval());
        String aggregationExpression = buildAggregationExpression(request.getMethod(), request.getDataField());

        return String.format("""
                SELECT
                    TO_CHAR(DATE_TRUNC('%s', created_at), 'YYYY-MM-DD HH24:MI') as time_bucket,
                    %s as aggregated_value
                FROM weighing_logs
                WHERE scale_id IN (%s)
                    AND created_at BETWEEN '%s 00:00:00+00' AND '%s 23:59:59+00'
                GROUP BY DATE_TRUNC('%s', created_at)
                ORDER BY time_bucket
                """,
                dateTruncParam,
                aggregationExpression,
                scaleIdsStr,
                request.getFromDate(),
                request.getToDate(),
                dateTruncParam
        );
    }

    /**
     * Build pre-aggregated report query
     * Query scale_daily_reports which has already aggregated data
     */
    private String buildPreAggregatedQuery(ReportRequestDto request) {
        String scaleIdsStr = request.getScaleIds().toString().replaceAll("[\\[\\]]", "");
        String dateTruncParam = getDateTruncParameter(request.getInterval());
        String aggregationExpression = buildAggregationExpression(request.getMethod(), request.getDataField());

        return String.format("""
                SELECT
                    TO_CHAR(DATE_TRUNC('%s', date), 'YYYY-MM-DD') as time_bucket,
                    %s as aggregated_value
                FROM scale_daily_reports
                WHERE scale_id IN (%s)
                    AND date BETWEEN '%s' AND '%s'
                GROUP BY DATE_TRUNC('%s', date)
                ORDER BY time_bucket
                """,
                dateTruncParam,
                aggregationExpression,
                scaleIdsStr,
                request.getFromDate(),
                request.getToDate(),
                dateTruncParam
        );
    }

    /**
     * Build aggregation expression - cast string to numeric
     * Data in weighing_logs is stored as string, need to cast to NUMERIC for aggregation
     */
    private String buildAggregationExpression(ReportRequestDto.AggregationMethod method, String dataField) {
        // Validate and normalize dataField to proper column name
        String columnName = normalizeDataFieldName(dataField);
        
        // Cast string data to NUMERIC, handle invalid values as 0
        String castExpression = String.format(
                "COALESCE(NULLIF(TRIM(%s), '')::NUMERIC, 0)",
                columnName
        );

        return switch (method) {
            case SUM -> String.format("SUM(%s)", castExpression);
            case AVG -> String.format("AVG(%s)", castExpression);
            case MAX -> String.format("MAX(%s)", castExpression);
        };
    }
    
    /**
     * Normalize data field name to proper column name
     * Ensures the field name matches the actual column in database
     */
    private String normalizeDataFieldName(String dataField) {
        if (dataField == null || dataField.trim().isEmpty()) {
            throw new IllegalArgumentException("Data field cannot be null or empty");
        }
        
        // Convert to lowercase and remove any whitespace
        String normalized = dataField.toLowerCase().trim();
        
        // If it's already in correct format (data_1, data_2, etc.), return it
        if (normalized.matches("data_[1-5]")) {
            return normalized;
        }
        
        // If it's in format like "data1", "data2", convert to "data_1", "data_2"
        if (normalized.matches("data[1-5]")) {
            return normalized.replace("data", "data_");
        }
        
        // If it's just a number (1-5), convert to "data_N"
        if (normalized.matches("[1-5]")) {
            return "data_" + normalized;
        }
        
        // Default fallback - assume it's data_1 if invalid
        log.warn("Invalid data field '{}', defaulting to data_1", dataField);
        return "data_1";
    }

    /**
     * Get date_trunc parameter based on interval
     */
    private String getDateTruncParameter(ReportRequestDto.TimeInterval interval) {
        return switch (interval) {
            case HOUR -> "hour";
            case DAY -> "day";
            case WEEK -> "week";
            case MONTH -> "month";
            case YEAR -> "year";
        };
    }

    /**
     * Build report name from request parameters
     */
    private String buildReportName(ReportRequestDto request) {
        String methodName = switch (request.getMethod()) {
            case SUM -> "Tổng";
            case AVG -> "Trung bình";
            case MAX -> "Lớn nhất";
        };

        String intervalName = switch (request.getInterval()) {
            case HOUR -> "theo giờ";
            case DAY -> "theo ngày";
            case WEEK -> "theo tuần";
            case MONTH -> "theo tháng";
            case YEAR -> "theo năm";
        };

        return String.format("%s %s %s", methodName, request.getDataField(), intervalName);
    }

    @Override
    @Transactional
    public void aggregateDailyData() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("[REPORT] Starting daily aggregation for date: {}", yesterday);

        try {
            // Query to aggregate data from weighing_logs
            String sql = """
                    SELECT
                        scale_id,
                        MAX(last_time) as last_time,
                        data_1,
                        data_2,
                        data_3,
                        data_4,
                        data_5
                    FROM weighing_logs
                    WHERE DATE(created_at) = ?
                    GROUP BY scale_id, data_1, data_2, data_3, data_4, data_5
                    """;

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, yesterday);

            int savedCount = 0;
            for (Map<String, Object> row : results) {
                ScaleDailyReport report = ScaleDailyReport.builder()
                        .date(yesterday)
                        .scaleId((Long) row.get("scale_id"))
                        .lastTime(((OffsetDateTime) row.get("last_time")))
                        .data1((String) row.get("data_1"))
                        .data2((String) row.get("data_2"))
                        .data3((String) row.get("data_3"))
                        .data4((String) row.get("data_4"))
                        .data5((String) row.get("data_5"))
                        .build();

                // Set audit fields manually since builder doesn't include them
                report.setCreatedBy("system");
                report.setUpdatedBy("system");

                dailyReportRepository.save(report);
                savedCount++;
            }

            log.info("[REPORT] Daily aggregation completed: {} records saved", savedCount);
        } catch (Exception e) {
            log.error("[REPORT] Error during daily aggregation: {}", e.getMessage(), e);
            throw e;
        }
    }
}
