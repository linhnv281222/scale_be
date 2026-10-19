package org.facenet.service.report;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.facenet.common.exception.ValidationException;
import org.facenet.dto.report.ReportDesignerDto;
import org.facenet.entity.report.ReportTemplate;
import org.facenet.entity.report.TemplateImport;
import org.facenet.repository.report.ReportTemplateRepository;
import org.facenet.repository.report.TemplateImportRepository;
import org.facenet.util.TemplateFileUtil;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ReportDesignerService {

    public static final String DESIGNER_CONFIG_KEY = "report_designer";
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([^{}]+?)\\s*}}|\\[([^\\[\\]]+)]");
    private static final Set<String> DATA_FIELDS = Set.of("data_1", "data_2", "data_3", "data_4", "data_5");
    private static final Set<String> MODEL_FIELDS = Set.of(
            "organizationName", "companyNameEn", "address", "phone", "email", "taxCode",
            "reportTitle", "reportCode", "exportTime", "startTime", "endTime", "preparedBy",
            "currentDateTime", "scaleNames", "rowNumber", "scaleId", "scaleCode", "scaleName",
            "location", "period", "data1Start", "data2Start", "data3Start", "data4Start",
            "data5Start", "data1End", "data2End", "data3End", "data4End", "data5End",
            "data1Total", "data2Total", "data3Total", "data4Total", "data5Total",
            "recordCount", "lastTime", "direction", "ratio", "data1Name", "data2Name",
            "data3Name", "data4Name", "data5Name");

    private final TemplateImportRepository templateImportRepository;
    private final ReportTemplateRepository reportTemplateRepository;
    private final TemplateFileUtil templateFileUtil;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public ReportDesignerDto.TemplateAnalysis analyzeTemplate(Long importId) throws IOException {
        TemplateImport templateImport = getImport(importId);
        byte[] bytes = readTemplate(templateImport);
        String filename = templateImport.getOriginalFilename().toLowerCase(Locale.ROOT);
        List<ReportDesignerDto.SheetAnalysis> sheets = new ArrayList<>();
        LinkedHashSet<String> placeholders = new LinkedHashSet<>();
        String type;
        if (filename.endsWith(".docx")) {
            type = "WORD";
            analyzeWord(bytes, placeholders);
        } else if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
            type = "EXCEL";
            analyzeExcel(bytes, placeholders, sheets);
        } else {
            throw new ValidationException("Only .docx, .xlsx and .xls templates can be analyzed");
        }
        return ReportDesignerDto.TemplateAnalysis.builder()
                .importId(importId).templateId(templateImport.getTemplate().getId())
                .filename(templateImport.getOriginalFilename()).fileType(type)
                .placeholders(List.copyOf(placeholders)).sheets(sheets)
                .mapping(readMapping(templateImport.getTemplate())).build();
    }

    @Transactional(readOnly = true)
    public ReportDesignerDto.TemplateMapping getMapping(Long importId) {
        return readMapping(getImport(importId).getTemplate());
    }

    @Transactional
    public ReportDesignerDto.TemplateMapping saveMapping(Long importId,
            ReportDesignerDto.TemplateMappingRequest request) {
        TemplateImport templateImport = getImport(importId);
        validateMappings(request.getPlaceholderMappings(), request.getColumnMappings());
        Map<String, String> normalizedPlaceholders = new LinkedHashMap<>();
        copy(request.getPlaceholderMappings()).forEach((placeholder, source) ->
                normalizedPlaceholders.put(normalizePlaceholder(placeholder), source));
        ReportDesignerDto.TemplateMapping mapping = ReportDesignerDto.TemplateMapping.builder()
                .placeholderMappings(normalizedPlaceholders)
                .sheetName(blankToNull(request.getSheetName()))
                .headerRow(request.getHeaderRow())
                .dataStartRow(request.getDataStartRow())
                .rowOffset(defaultInt(request.getRowOffset(), 0))
                .columnOffset(defaultInt(request.getColumnOffset(), 0))
                .columnMappings(copy(request.getColumnMappings())).build();

        ReportTemplate template = templateImport.getTemplate();
        Map<String, Object> config = template.getTableConfig() == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(template.getTableConfig());
        config.put(DESIGNER_CONFIG_KEY, mappingToMap(mapping));
        template.setTableConfig(config);
        reportTemplateRepository.save(template);
        return mapping;
    }

    @Transactional(readOnly = true)
    public ReportDesignerDto.DetailQueryResponse queryDetail(ReportDesignerDto.DetailQueryRequest request) {
        validateRange(request.getFrom(), request.getTo());
        List<String> fields = normalizeFields(request.getFields());
        int step = defaultInt(request.getRowStep(), 1);
        int page = defaultInt(request.getPage(), 0);
        int size = defaultInt(request.getSize(), 100);
        String direction = "ASC".equalsIgnoreCase(request.getSortDirection()) ? "ASC" : "DESC";
        MapSqlParameterSource params = filterParams(request.getScaleIds(), request.getLocationIds(),
                request.getFrom(), request.getTo()).addValue("rowStep", step)
                .addValue("limit", size).addValue("offset", (long) page * size);
        String where = filterWhere(request.getScaleIds(), request.getLocationIds());
        String selectedFields = fields.stream().map(field -> "sampled." + field).reduce((a, b) -> a + ", " + b).orElse("");
        String sql = """
                WITH filtered AS (
                    SELECT wl.scale_id, s.name AS scale_name, s.location_id,
                           l.name AS location_name, wl.last_time, wl.created_at,
                           wl.data_1, wl.data_2, wl.data_3, wl.data_4, wl.data_5,
                           ROW_NUMBER() OVER (ORDER BY wl.last_time, wl.scale_id, wl.created_at) AS sample_no
                    FROM weighing_logs wl
                    JOIN scales s ON s.id = wl.scale_id
                    LEFT JOIN locations l ON l.id = s.location_id
                    WHERE %s
                ), sampled AS (
                    SELECT * FROM filtered WHERE MOD(sample_no - 1, :rowStep) = 0
                )
                SELECT sampled.scale_id, sampled.scale_name, sampled.location_id,
                       sampled.location_name, sampled.last_time, sampled.created_at%s
                FROM sampled
                ORDER BY sampled.last_time %s, sampled.scale_id %s
                LIMIT :limit OFFSET :offset
                """.formatted(where, selectedFields.isBlank() ? "" : ", " + selectedFields, direction, direction);
        String countSql = """
                WITH filtered AS (
                    SELECT ROW_NUMBER() OVER (ORDER BY wl.last_time, wl.scale_id, wl.created_at) AS sample_no
                    FROM weighing_logs wl JOIN scales s ON s.id = wl.scale_id
                    WHERE %s
                )
                SELECT COUNT(*) FROM filtered WHERE MOD(sample_no - 1, :rowStep) = 0
                """.formatted(where);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params);
        Long total = jdbcTemplate.queryForObject(countSql, params, Long.class);
        long totalRows = total == null ? 0 : total;
        return ReportDesignerDto.DetailQueryResponse.builder()
                .from(request.getFrom()).to(request.getTo()).rowStep(step).page(page).size(size)
                .totalRows(totalRows).totalPages((totalRows + size - 1) / size)
                .fields(fields).rows(rows).build();
    }

    @Transactional(readOnly = true)
    public ReportDesignerDto.ChartResponse queryChart(ReportDesignerDto.ChartRequest request) {
        validateRange(request.getFrom(), request.getTo());
        List<String> fields = normalizeFields(request.getFields());
        String bucket = normalizeBucket(request.getBucket(), request.getFrom(), request.getTo());
        String aggregation = normalizeAggregation(request.getAggregation());
        String where = filterWhere(request.getScaleIds(), request.getLocationIds());
        MapSqlParameterSource params = filterParams(request.getScaleIds(), request.getLocationIds(),
                request.getFrom(), request.getTo());
        List<ReportDesignerDto.ChartSeries> series = new ArrayList<>();
        for (String field : fields) {
            String numeric = numericExpression("wl." + field);
            String sql = """
                    SELECT wl.scale_id, s.name AS scale_name,
                           DATE_TRUNC('%s', wl.last_time) AS bucket_time,
                           %s(%s) AS point_value
                    FROM weighing_logs wl JOIN scales s ON s.id = wl.scale_id
                    WHERE %s AND %s IS NOT NULL
                    GROUP BY wl.scale_id, s.name, DATE_TRUNC('%s', wl.last_time)
                    ORDER BY wl.scale_id, bucket_time
                    """.formatted(bucket, aggregation, numeric, where, numeric, bucket);
            Map<String, ReportDesignerDto.ChartSeries> byScale = new LinkedHashMap<>();
            jdbcTemplate.queryForList(sql, params).forEach(row -> {
                Long scaleId = ((Number) row.get("scale_id")).longValue();
                String key = field + ":" + scaleId;
                ReportDesignerDto.ChartSeries item = byScale.computeIfAbsent(key, ignored ->
                        ReportDesignerDto.ChartSeries.builder().scaleId(scaleId)
                                .scaleName((String) row.get("scale_name")).field(field)
                                .points(new ArrayList<>()).build());
                item.getPoints().add(ReportDesignerDto.ChartPoint.builder()
                        .timestamp(toOffsetDateTime(row.get("bucket_time")))
                        .value(decimal(row.get("point_value"))).build());
            });
            series.addAll(byScale.values());
        }
        series.sort(Comparator.comparing(ReportDesignerDto.ChartSeries::getScaleId)
                .thenComparing(ReportDesignerDto.ChartSeries::getField));
        return ReportDesignerDto.ChartResponse.builder().from(request.getFrom()).to(request.getTo())
                .bucket(bucket.toUpperCase(Locale.ROOT)).aggregation(aggregation).series(series).build();
    }

    private void analyzeWord(byte[] bytes, Collection<String> placeholders) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            document.getParagraphs().forEach(p -> addPlaceholders(p.getText(), placeholders));
            for (XWPFTable table : document.getTables()) analyzeWordTable(table, placeholders);
            document.getHeaderList().forEach(header -> {
                header.getParagraphs().forEach(p -> addPlaceholders(p.getText(), placeholders));
                header.getTables().forEach(t -> analyzeWordTable(t, placeholders));
            });
            document.getFooterList().forEach(footer -> {
                footer.getParagraphs().forEach(p -> addPlaceholders(p.getText(), placeholders));
                footer.getTables().forEach(t -> analyzeWordTable(t, placeholders));
            });
        }
    }

    private void analyzeWordTable(XWPFTable table, Collection<String> placeholders) {
        for (XWPFTableRow row : table.getRows()) for (XWPFTableCell cell : row.getTableCells())
            for (XWPFParagraph paragraph : cell.getParagraphs()) addPlaceholders(paragraph.getText(), placeholders);
    }

    private void analyzeExcel(byte[] bytes, Collection<String> placeholders,
                              List<ReportDesignerDto.SheetAnalysis> sheets) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            DataFormatter formatter = new DataFormatter();
            for (Sheet sheet : workbook) {
                List<ReportDesignerDto.PlaceholderPosition> positions = new ArrayList<>();
                int lastColumn = 0;
                for (Row row : sheet) {
                    lastColumn = Math.max(lastColumn, Math.max(0, row.getLastCellNum() - 1));
                    for (Cell cell : row) {
                        String text = formatter.formatCellValue(cell);
                        Matcher matcher = PLACEHOLDER.matcher(text);
                        while (matcher.find()) {
                            String placeholder = matcher.group(1) != null ? matcher.group(1).trim() : matcher.group(2).trim();
                            placeholders.add(placeholder);
                            positions.add(ReportDesignerDto.PlaceholderPosition.builder().placeholder(placeholder)
                                    .row(row.getRowNum()).column(cell.getColumnIndex()).build());
                        }
                    }
                }
                sheets.add(ReportDesignerDto.SheetAnalysis.builder().name(sheet.getSheetName())
                        .lastRow(sheet.getLastRowNum()).lastColumn(lastColumn).placeholders(positions).build());
            }
        }
    }

    private void addPlaceholders(String text, Collection<String> placeholders) {
        if (text == null) return;
        Matcher matcher = PLACEHOLDER.matcher(text);
        while (matcher.find()) placeholders.add((matcher.group(1) != null ? matcher.group(1) : matcher.group(2)).trim());
    }

    private void validateMappings(Map<String, String> placeholders, Map<String, String> columns) {
        List<String> sources = new ArrayList<>();
        if (placeholders != null) sources.addAll(placeholders.values());
        if (columns != null) {
            columns.forEach((index, source) -> {
                try { if (Integer.parseInt(index) < 0) throw new NumberFormatException(); }
                catch (NumberFormatException e) { throw new ValidationException("column_mappings keys must be zero-based non-negative column indexes"); }
                sources.add(source);
            });
        }
        for (String source : sources) {
            if (source == null || (!MODEL_FIELDS.contains(source) && !source.startsWith("summary.")))
                throw new ValidationException("Unsupported report mapping source: " + source);
        }
    }

    @SuppressWarnings("unchecked")
    public static ReportDesignerDto.TemplateMapping readMapping(ReportTemplate template) {
        if (template == null || template.getTableConfig() == null) return emptyMapping();
        Object raw = template.getTableConfig().get(DESIGNER_CONFIG_KEY);
        if (!(raw instanceof Map<?, ?> map)) return emptyMapping();
        return ReportDesignerDto.TemplateMapping.builder()
                .placeholderMappings(stringMap(map.get("placeholderMappings")))
                .sheetName(value(map.get("sheetName")))
                .headerRow(integer(map.get("headerRow")))
                .dataStartRow(integer(map.get("dataStartRow")))
                .rowOffset(defaultInt(integer(map.get("rowOffset")), 0))
                .columnOffset(defaultInt(integer(map.get("columnOffset")), 0))
                .columnMappings(stringMap(map.get("columnMappings"))).build();
    }

    private static ReportDesignerDto.TemplateMapping emptyMapping() {
        return ReportDesignerDto.TemplateMapping.builder().placeholderMappings(Map.of())
                .rowOffset(0).columnOffset(0).columnMappings(Map.of()).build();
    }

    private Map<String, Object> mappingToMap(ReportDesignerDto.TemplateMapping mapping) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("placeholderMappings", mapping.getPlaceholderMappings());
        value.put("sheetName", mapping.getSheetName());
        value.put("headerRow", mapping.getHeaderRow());
        value.put("dataStartRow", mapping.getDataStartRow());
        value.put("rowOffset", mapping.getRowOffset());
        value.put("columnOffset", mapping.getColumnOffset());
        value.put("columnMappings", mapping.getColumnMappings());
        return value;
    }

    private TemplateImport getImport(Long importId) {
        return templateImportRepository.findById(importId)
                .orElseThrow(() -> new ValidationException("Template import not found: " + importId));
    }

    private byte[] readTemplate(TemplateImport value) throws IOException {
        try { return templateFileUtil.readTemplateFile(value.getResourcePath()); }
        catch (IOException first) { return java.nio.file.Files.readAllBytes(java.nio.file.Path.of(value.getFilePath())); }
    }

    private void validateRange(OffsetDateTime from, OffsetDateTime to) {
        if (from == null || to == null || !from.isBefore(to)) throw new ValidationException("from must be before to");
        if (Duration.between(from, to).toDays() > 366) throw new ValidationException("Report range must not exceed 366 days");
    }

    private List<String> normalizeFields(List<String> fields) {
        List<String> result = fields == null || fields.isEmpty() ? List.of("data_1", "data_2")
                : fields.stream().map(value -> value.toLowerCase(Locale.ROOT)).distinct().toList();
        if (!DATA_FIELDS.containsAll(result)) throw new ValidationException("fields only support data_1 through data_5");
        return result;
    }

    private String filterWhere(List<Long> scaleIds, List<Long> locationIds) {
        StringBuilder where = new StringBuilder("wl.last_time >= :fromTime AND wl.last_time < :toTime");
        if (scaleIds != null && !scaleIds.isEmpty()) where.append(" AND wl.scale_id IN (:scaleIds)");
        if (locationIds != null && !locationIds.isEmpty()) where.append(" AND s.location_id IN (:locationIds)");
        return where.toString();
    }

    private MapSqlParameterSource filterParams(List<Long> scaleIds, List<Long> locationIds,
                                                OffsetDateTime from, OffsetDateTime to) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("fromTime", from).addValue("toTime", to);
        if (scaleIds != null && !scaleIds.isEmpty()) params.addValue("scaleIds", scaleIds);
        if (locationIds != null && !locationIds.isEmpty()) params.addValue("locationIds", locationIds);
        return params;
    }

    private String normalizeBucket(String value, OffsetDateTime from, OffsetDateTime to) {
        if (value != null && !value.isBlank() && !"AUTO".equalsIgnoreCase(value)) {
            String result = value.toLowerCase(Locale.ROOT);
            if (!Set.of("minute", "hour", "day", "week", "month").contains(result))
                throw new ValidationException("bucket must be AUTO, MINUTE, HOUR, DAY, WEEK or MONTH");
            return result;
        }
        Duration duration = Duration.between(from, to);
        if (duration.toHours() <= 6) return "minute";
        if (duration.toDays() <= 7) return "hour";
        if (duration.toDays() <= 90) return "day";
        return "week";
    }

    private String normalizeAggregation(String value) {
        String result = value == null ? "AVG" : value.toUpperCase(Locale.ROOT);
        if (!Set.of("AVG", "MIN", "MAX", "SUM").contains(result))
            throw new ValidationException("aggregation must be AVG, MIN, MAX or SUM");
        return result;
    }

    private String numericExpression(String column) {
        return "CASE WHEN TRIM(" + column + ") ~ '^[+-]?([0-9]+([.][0-9]*)?|[.][0-9]+)$' "
                + "THEN TRIM(" + column + ")::NUMERIC END";
    }

    private OffsetDateTime toOffsetDateTime(Object value) {
        if (value instanceof OffsetDateTime offset) return offset;
        if (value instanceof Timestamp timestamp) return timestamp.toInstant().atOffset(ZoneOffset.UTC);
        if (value instanceof java.time.LocalDateTime local) return local.atOffset(ZoneOffset.UTC);
        return null;
    }

    private BigDecimal decimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal decimal) return decimal;
        return new BigDecimal(value.toString());
    }

    private static int defaultInt(Integer value, int fallback) { return value == null ? fallback : value; }
    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private static String normalizePlaceholder(String value) {
        String result = value == null ? "" : value.trim();
        if (result.startsWith("{{") && result.endsWith("}}")) result = result.substring(2, result.length() - 2);
        else if (result.startsWith("[") && result.endsWith("]")) result = result.substring(1, result.length() - 1);
        if (result.isBlank()) throw new ValidationException("Placeholder name must not be blank");
        return result.trim();
    }
    private static String value(Object value) { return value == null ? null : value.toString(); }
    private static Integer integer(Object value) { return value instanceof Number n ? n.intValue() : value == null ? null : Integer.valueOf(value.toString()); }
    private static Map<String, String> copy(Map<String, String> value) { return value == null ? Map.of() : new LinkedHashMap<>(value); }
    private static Map<String, String> stringMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) return Map.of();
        Map<String, String> result = new LinkedHashMap<>();
        map.forEach((key, item) -> result.put(String.valueOf(key), item == null ? null : String.valueOf(item)));
        return result;
    }
}
