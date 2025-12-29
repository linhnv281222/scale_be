package org.facenet.service.scale.report;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.facenet.dto.scale.IntervalReportRequestDto;
import org.facenet.dto.scale.IntervalReportResponseDto;
import org.facenet.dto.scale.ReportRequestDto;
import org.facenet.dto.scale.ReportResponseDto;
import org.facenet.entity.scale.ScaleDailyReport;
import org.facenet.repository.scale.ScaleConfigRepository;
import org.facenet.repository.scale.ScaleDailyReportRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final ScaleConfigRepository scaleConfigRepository;

    private static final String CUMULATIVE_WEIGHT_NAME = "Khối lượng tích lũy";
    private static final String WEIGHT_OUTPUT_NAME = "Khối lượng";

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

    @Override
    public IntervalReportResponseDto generateIntervalReport(IntervalReportRequestDto request) {
        validateIntervalRequest(request);

        List<Long> scaleIdsUsed = resolveScaleIdsForIntervalReport(request.getScaleIds());
        Map<Long, Map<String, DataFieldMeta>> metaByScaleId = resolveDataFieldMetaByScale(scaleIdsUsed);
        Map<String, Set<Long>> cumulativeScaleIdsByField = resolveCumulativeWeightScaleIdsByField(metaByScaleId);

        Map<Long, IntervalReportResponseDto.ScaleInfo> scaleInfoMap = fetchScaleInfoMap(scaleIdsUsed);

        Long sampleScaleId = !scaleIdsUsed.isEmpty() ? scaleIdsUsed.get(0) : null;
        Map<String, String> sampleNames = sampleScaleId != null
            ? metaToNames(metaByScaleId.get(sampleScaleId))
            : defaultDataFieldNames();

        Map<String, IntervalReportRequestDto.AggregationMethod> effectiveMethods = resolveEffectiveMethods(request, sampleNames);

        String sql = buildIntervalQuery(request, effectiveMethods, cumulativeScaleIdsByField);
        log.debug("[REPORT] Interval query: {}", sql);

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
        List<IntervalReportResponseDto.Row> rows = mapIntervalRows(results, metaByScaleId, scaleInfoMap);

        Map<String, String> methodStrings = new HashMap<>();
        for (Map.Entry<String, IntervalReportRequestDto.AggregationMethod> e : effectiveMethods.entrySet()) {
            methodStrings.put(e.getKey(), e.getValue().name());
        }

        return IntervalReportResponseDto.builder()
                .interval(request.getInterval())
                .fromDate(request.getFromDate().toString())
                .toDate(request.getToDate().toString())
                .dataFieldNames(sampleNames)
                .aggregationByField(methodStrings)
                .rows(rows)
                .build();
    }

    private List<Long> resolveScaleIdsForIntervalReport(List<Long> requestedScaleIds) {
        if (requestedScaleIds != null && !requestedScaleIds.isEmpty()) {
            return requestedScaleIds;
        }

        // Default: all active scales
        return jdbcTemplate.queryForList("SELECT id FROM scales WHERE is_active = true", Long.class);
    }

    private Map<String, String> defaultDataFieldNames() {
        Map<String, String> defaults = new HashMap<>();
        defaults.put("data_1", "Data 1");
        defaults.put("data_2", "Data 2");
        defaults.put("data_3", "Data 3");
        defaults.put("data_4", "Data 4");
        defaults.put("data_5", "Data 5");
        return defaults;
    }

    private record DataFieldMeta(String name, boolean used) {
    }

    private Map<Long, Map<String, DataFieldMeta>> resolveDataFieldMetaByScale(List<Long> scaleIds) {
        Map<Long, Map<String, DataFieldMeta>> result = new HashMap<>();
        if (scaleIds == null || scaleIds.isEmpty()) {
            return result;
        }

        // Pull via JPA so it works across DBs without JSON SQL operators
        scaleConfigRepository.findAllById(scaleIds).forEach(cfg -> {
            Map<String, String> defaults = defaultDataFieldNames();
            Map<String, DataFieldMeta> meta = new HashMap<>();

            meta.put("data_1", toMeta(cfg.getData1(), defaults.get("data_1")));
            meta.put("data_2", toMeta(cfg.getData2(), defaults.get("data_2")));
            meta.put("data_3", toMeta(cfg.getData3(), defaults.get("data_3")));
            meta.put("data_4", toMeta(cfg.getData4(), defaults.get("data_4")));
            meta.put("data_5", toMeta(cfg.getData5(), defaults.get("data_5")));

            result.put(cfg.getScaleId(), meta);
        });

        // Ensure every requested scaleId has at least defaults
        for (Long id : scaleIds) {
            result.computeIfAbsent(id, k -> {
                Map<String, String> defaults = defaultDataFieldNames();
                Map<String, DataFieldMeta> meta = new HashMap<>();
                meta.put("data_1", new DataFieldMeta(defaults.get("data_1"), false));
                meta.put("data_2", new DataFieldMeta(defaults.get("data_2"), false));
                meta.put("data_3", new DataFieldMeta(defaults.get("data_3"), false));
                meta.put("data_4", new DataFieldMeta(defaults.get("data_4"), false));
                meta.put("data_5", new DataFieldMeta(defaults.get("data_5"), false));
                return meta;
            });
        }

        return result;
    }

    private DataFieldMeta toMeta(Map<String, Object> dataConfig, String defaultName) {
        String name = extractNameFromDataConfig(dataConfig, defaultName);
        boolean used = false;
        if (dataConfig != null) {
            Object usedObj = dataConfig.get("is_used");
            if (usedObj instanceof Boolean b) {
                used = b;
            } else if (usedObj != null) {
                used = Boolean.parseBoolean(usedObj.toString());
            }
        }
        return new DataFieldMeta(name, used);
    }

    private Map<String, String> metaToNames(Map<String, DataFieldMeta> meta) {
        Map<String, String> names = new HashMap<>();
        Map<String, String> defaults = defaultDataFieldNames();
        for (String field : List.of("data_1", "data_2", "data_3", "data_4", "data_5")) {
            DataFieldMeta m = meta != null ? meta.get(field) : null;
            names.put(field, m != null ? m.name() : defaults.get(field));
        }
        return names;
    }

    private Map<String, Set<Long>> resolveCumulativeWeightScaleIdsByField(Map<Long, Map<String, DataFieldMeta>> metaByScaleId) {
        Map<String, Set<Long>> map = new HashMap<>();
        for (String field : List.of("data_1", "data_2", "data_3", "data_4", "data_5")) {
            Set<Long> ids = metaByScaleId.entrySet().stream()
                    .filter(e -> {
                        DataFieldMeta meta = e.getValue().get(field);
                        return meta != null && isCumulativeWeightName(meta.name());
                    })
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
            map.put(field, ids);
        }
        return map;
    }

    private Map<Long, IntervalReportResponseDto.ScaleInfo> fetchScaleInfoMap(List<Long> scaleIds) {
        Map<Long, IntervalReportResponseDto.ScaleInfo> map = new HashMap<>();
        if (scaleIds == null || scaleIds.isEmpty()) {
            return map;
        }

        String in = scaleIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        String sql = String.format("""
                SELECT
                    s.id as scale_id,
                    s.name as scale_name,
                    s.model as scale_model,
                    s.type as scale_type,
                    s.is_active as scale_is_active,
                    s.created_at as scale_created_at,
                    s.created_by as scale_created_by,
                    s.updated_at as scale_updated_at,
                    s.updated_by as scale_updated_by,
                    l.id as location_id,
                    l.code as location_code,
                    l.name as location_name,
                    l.description as location_description,
                    l.parent_id as location_parent_id
                FROM scales s
                LEFT JOIN locations l ON s.location_id = l.id
                WHERE s.id IN (%s)
                """, in);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        for (Map<String, Object> r : rows) {
            Long id = r.get("scale_id") != null ? ((Number) r.get("scale_id")).longValue() : null;
            if (id == null) {
                continue;
            }

            IntervalReportResponseDto.LocationInfo location = null;
            if (r.get("location_id") != null) {
                location = IntervalReportResponseDto.LocationInfo.builder()
                        .id(((Number) r.get("location_id")).longValue())
                        .code(Objects.toString(r.get("location_code"), null))
                        .name(Objects.toString(r.get("location_name"), null))
                        .description(Objects.toString(r.get("location_description"), null))
                        .parentId(r.get("location_parent_id") != null ? ((Number) r.get("location_parent_id")).longValue() : null)
                        .build();
            }

            map.put(id, IntervalReportResponseDto.ScaleInfo.builder()
                    .id(id)
                    .name(Objects.toString(r.get("scale_name"), null))
                    .model(Objects.toString(r.get("scale_model"), null))
                    .type(Objects.toString(r.get("scale_type"), null))
                    .isActive((Boolean) r.get("scale_is_active"))
                    .location(location)
                    .createdAt(toOffsetDateTime(r.get("scale_created_at")))
                    .createdBy(Objects.toString(r.get("scale_created_by"), null))
                    .updatedAt(toOffsetDateTime(r.get("scale_updated_at")))
                    .updatedBy(Objects.toString(r.get("scale_updated_by"), null))
                    .build());
        }

        return map;
    }

    private OffsetDateTime toOffsetDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof OffsetDateTime odt) {
            return odt;
        }
        if (value instanceof java.sql.Timestamp ts) {
            // Timestamp has no offset info; treat as UTC to keep API consistent
            return ts.toInstant().atOffset(java.time.ZoneOffset.UTC);
        }
        if (value instanceof java.time.LocalDateTime ldt) {
            return ldt.atOffset(java.time.ZoneOffset.UTC);
        }
        if (value instanceof java.util.Date d) {
            return d.toInstant().atOffset(java.time.ZoneOffset.UTC);
        }
        try {
            return OffsetDateTime.parse(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private void validateIntervalRequest(IntervalReportRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        if (request.getFromDate() == null || request.getToDate() == null) {
            throw new IllegalArgumentException("fromDate and toDate are required");
        }
        if (request.getFromDate().isAfter(request.getToDate())) {
            throw new IllegalArgumentException("fromDate must be <= toDate");
        }
        if (request.getInterval() == null) {
            throw new IllegalArgumentException("interval is required");
        }
    }

    private Map<String, IntervalReportRequestDto.AggregationMethod> resolveEffectiveMethods(
            IntervalReportRequestDto request,
            Map<String, String> dataFieldNames
    ) {
        Map<String, IntervalReportRequestDto.AggregationMethod> effective = new HashMap<>();

        for (String field : List.of("data_1", "data_2", "data_3", "data_4", "data_5")) {
            IntervalReportRequestDto.AggregationMethod requested = null;
            if (request.getAggregationByField() != null) {
                requested = request.getAggregationByField().get(field);
            }
            IntervalReportRequestDto.AggregationMethod resolved = requested != null
                    ? requested
                    : IntervalReportRequestDto.AggregationMethod.ABS;

            effective.put(field, resolved);
        }

        return effective;
    }

    private boolean isCumulativeWeightName(String name) {
        return name != null && CUMULATIVE_WEIGHT_NAME.equalsIgnoreCase(name.trim());
    }

    private String extractNameFromDataConfig(Map<String, Object> dataConfig, String defaultName) {
        if (dataConfig == null) {
            return defaultName;
        }
        Object nameObj = dataConfig.get("name");
        if (nameObj == null) {
            return defaultName;
        }
        String name = nameObj.toString();
        return name.isBlank() ? defaultName : name;
    }

        private String buildIntervalQuery(
            IntervalReportRequestDto request,
            Map<String, IntervalReportRequestDto.AggregationMethod> methods,
            Map<String, Set<Long>> cumulativeScaleIdsByField
        ) {
        String scaleFilter = buildScaleIdsFilter(request.getScaleIds());

        String start;
        String end;
        if (request.getFromTime() != null && request.getToTime() != null) {
            // Use precise request boundaries (preserves timezone offset)
            start = request.getFromTime().toString();
            end = request.getToTime().toString();
        } else {
            // Backward-compatible behavior: interpret date range in UTC day bounds
            start = request.getFromDate() + " 00:00:00+00";
            end = request.getToDate() + " 23:59:59+00";
        }

        String timeSelect;
        String timeGroupBy;
        String timeOrderBy;

        switch (request.getInterval()) {
            case HOUR -> {
                timeSelect = "DATE_TRUNC('hour', wl.created_at) as bucket_time, TO_CHAR(DATE_TRUNC('hour', wl.created_at), 'YYYY-MM-DD HH24:00') as period";
                timeGroupBy = "DATE_TRUNC('hour', wl.created_at)";
                timeOrderBy = "bucket_time";
            }
            case DAY -> {
                timeSelect = "DATE_TRUNC('day', wl.created_at) as bucket_time, TO_CHAR(DATE_TRUNC('day', wl.created_at), 'YYYY-MM-DD') as period";
                timeGroupBy = "DATE_TRUNC('day', wl.created_at)";
                timeOrderBy = "bucket_time";
            }
            case WEEK -> {
                timeSelect = "DATE_TRUNC('week', wl.created_at) as bucket_time, TO_CHAR(DATE_TRUNC('week', wl.created_at), 'YYYY-MM-DD') as period";
                timeGroupBy = "DATE_TRUNC('week', wl.created_at)";
                timeOrderBy = "bucket_time";
            }
            case SHIFT -> {
                timeSelect = "DATE(wl.created_at) as bucket_date, COALESCE(sh.code, 'NO_SHIFT') as shift_code, (TO_CHAR(DATE(wl.created_at), 'YYYY-MM-DD') || ' ' || COALESCE(sh.code, 'NO_SHIFT')) as period";
                timeGroupBy = "DATE(wl.created_at), COALESCE(sh.code, 'NO_SHIFT')";
                timeOrderBy = "bucket_date, shift_code";
            }
            default -> throw new IllegalArgumentException("Unsupported interval: " + request.getInterval());
        }

        String selectAgg = buildPerFieldSelect(methods, cumulativeScaleIdsByField);

        boolean isShift = request.getInterval() == IntervalReportRequestDto.TimeInterval.SHIFT;

        String joinShift = isShift ? "LEFT JOIN shifts sh ON wl.shift_id = sh.id" : "";

        String groupBy = isShift
                ? "wl.scale_id, s.name, " + timeGroupBy
                : "wl.scale_id, s.name, " + timeGroupBy;

        String orderBy = "wl.scale_id, " + timeOrderBy;

        return String.format("""
                SELECT
                    wl.scale_id as scale_id,
                    s.name as scale_name,
                    %s,
                    COUNT(*) as record_count,
                    %s
                FROM weighing_logs wl
                JOIN scales s ON wl.scale_id = s.id
                %s
                WHERE wl.scale_id IN (%s)
                    AND wl.created_at BETWEEN '%s' AND '%s'
                GROUP BY %s
                ORDER BY %s
                """,
                timeSelect,
                selectAgg,
                joinShift,
                scaleFilter,
                start,
                end,
                groupBy,
                orderBy
        );
    }

    private String buildPerFieldSelect(
            Map<String, IntervalReportRequestDto.AggregationMethod> methods,
            Map<String, Set<Long>> cumulativeScaleIdsByField
    ) {
        List<String> parts = new ArrayList<>();

        parts.add(buildAggregationExpression(methods.get("data_1"), "wl.data_1", cumulativeScaleIdsByField.get("data_1")) + " as data_1_val");
        parts.add(buildAggregationExpression(methods.get("data_2"), "wl.data_2", cumulativeScaleIdsByField.get("data_2")) + " as data_2_val");
        parts.add(buildAggregationExpression(methods.get("data_3"), "wl.data_3", cumulativeScaleIdsByField.get("data_3")) + " as data_3_val");
        parts.add(buildAggregationExpression(methods.get("data_4"), "wl.data_4", cumulativeScaleIdsByField.get("data_4")) + " as data_4_val");
        parts.add(buildAggregationExpression(methods.get("data_5"), "wl.data_5", cumulativeScaleIdsByField.get("data_5")) + " as data_5_val");

        return String.join(",\n                    ", parts);
    }

    private String buildAggregationExpression(
            IntervalReportRequestDto.AggregationMethod method,
            String column,
            Set<Long> cumulativeScaleIds
    ) {
        IntervalReportRequestDto.AggregationMethod resolved = method != null
                ? method
                : IntervalReportRequestDto.AggregationMethod.ABS;

        String numeric = buildSafeNumericExpression(column);

        String absExpr = String.format("ABS(COALESCE(MAX(%s), 0) - COALESCE(MIN(%s), 0))", numeric, numeric);

        String methodExpr = switch (resolved) {
            case SUM -> String.format("COALESCE(SUM(%s), 0)", numeric);
            case AVG -> String.format("COALESCE(AVG(%s), 0)", numeric);
            case MAX -> String.format("COALESCE(MAX(%s), 0)", numeric);
            case MIN -> String.format("COALESCE(MIN(%s), 0)", numeric);
            case COUNT -> "COUNT(*)";
            case ABS -> absExpr;
        };

        if (cumulativeScaleIds == null || cumulativeScaleIds.isEmpty()) {
            return methodExpr;
        }

        String inList = cumulativeScaleIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        return String.format("CASE WHEN wl.scale_id IN (%s) THEN %s ELSE %s END", inList, absExpr, methodExpr);
    }

    private String buildSafeNumericExpression(String column) {
        if (isPostgres()) {
            // Postgres: ignore invalid numeric strings using regex, cast to NUMERIC
            return String.format(
                    "CASE WHEN TRIM(%s) ~ '^-?\\d+(\\.\\d+)?$' THEN TRIM(%s)::NUMERIC END",
                    column,
                    column
            );
        }

        // H2 (and other DBs): TRY_CAST is safer than CAST
        return String.format("TRY_CAST(NULLIF(TRIM(%s), '') AS DOUBLE)", column);
    }

    private boolean isPostgres() {
        try {
            DataSource ds = jdbcTemplate.getDataSource();
            if (ds == null) {
                return false;
            }
            try (var conn = ds.getConnection()) {
                String product = conn.getMetaData().getDatabaseProductName();
                return product != null && product.toLowerCase().contains("postgres");
            }
        } catch (Exception e) {
            return false;
        }
    }

    private String buildScaleIdsFilter(List<Long> scaleIds) {
        if (scaleIds != null && !scaleIds.isEmpty()) {
            return scaleIds.toString().replaceAll("[\\[\\]]", "");
        }
        return "(SELECT id FROM scales WHERE is_active = true)";
    }

    private List<IntervalReportResponseDto.Row> mapIntervalRows(
            List<Map<String, Object>> results,
            Map<Long, Map<String, DataFieldMeta>> metaByScaleId,
            Map<Long, IntervalReportResponseDto.ScaleInfo> scaleInfoMap
    ) {
        List<IntervalReportResponseDto.Row> rows = new ArrayList<>();

        for (Map<String, Object> row : results) {
            Long scaleId = row.get("scale_id") != null ? ((Number) row.get("scale_id")).longValue() : null;

            Map<String, DataFieldMeta> meta = scaleId != null
                    ? metaByScaleId.getOrDefault(scaleId, Map.of())
                    : Map.of();

            Map<String, IntervalReportResponseDto.DataFieldValue> dataValues = new HashMap<>();
            dataValues.put("data_1", buildIntervalDataValue(meta.get("data_1"), row.get("data_1_val")));
            dataValues.put("data_2", buildIntervalDataValue(meta.get("data_2"), row.get("data_2_val")));
            dataValues.put("data_3", buildIntervalDataValue(meta.get("data_3"), row.get("data_3_val")));
            dataValues.put("data_4", buildIntervalDataValue(meta.get("data_4"), row.get("data_4_val")));
            dataValues.put("data_5", buildIntervalDataValue(meta.get("data_5"), row.get("data_5_val")));

            IntervalReportResponseDto.ScaleInfo scaleInfo = scaleId != null ? scaleInfoMap.get(scaleId) : null;
            if (scaleInfo == null && scaleId != null) {
                scaleInfo = IntervalReportResponseDto.ScaleInfo.builder()
                        .id(scaleId)
                        .name(Objects.toString(row.get("scale_name"), null))
                        .build();
            }

            rows.add(IntervalReportResponseDto.Row.builder()
                    .scale(scaleInfo)
                    .period(Objects.toString(row.get("period"), null))
                    .recordCount(row.get("record_count") != null ? ((Number) row.get("record_count")).intValue() : null)
                    .dataValues(dataValues)
                    .build());
        }

        return rows;
    }

    private IntervalReportResponseDto.DataFieldValue buildIntervalDataValue(DataFieldMeta meta, Object aggregatedValue) {
        String name = meta != null ? meta.name() : null;
        boolean used = meta != null && meta.used();

        if (isCumulativeWeightName(name)) {
            name = WEIGHT_OUTPUT_NAME;
        }

        return IntervalReportResponseDto.DataFieldValue.builder()
                .value(toValueString(aggregatedValue))
                .name(name)
                .used(used)
                .build();
    }

    private String toValueString(Object value) {
        if (value == null) {
            return "0";
        }
        if (value instanceof Number n) {
            double d = n.doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                return "0";
            }
            if (Math.floor(d) == d) {
                return String.valueOf((long) d);
            }
            return Double.toString(d);
        }
        String s = value.toString();
        return s.isBlank() ? "0" : s;
    }

    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return null;
        }
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
