package org.facenet.service.report;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.facenet.dto.report.ReportData;
import org.facenet.dto.report.ReportExportRequest;
import org.facenet.dto.report.ReportTemplateDto;
import org.facenet.dto.scale.IntervalReportRequestDto;
import org.facenet.dto.scale.IntervalReportResponseDto;
import org.facenet.entity.report.*;
import org.facenet.entity.scale.Scale;
import org.facenet.entity.scale.ScaleConfig;
import org.facenet.entity.scale.WeighingLog;
import org.facenet.repository.report.OrganizationSettingsRepository;
import org.facenet.repository.report.ReportTemplateRepository;
import org.facenet.repository.report.TemplateImportRepository;
import org.facenet.repository.scale.ScaleConfigRepository;
import org.facenet.repository.scale.ScaleRepository;
import org.facenet.repository.scale.WeighingLogRepository;
import org.facenet.service.scale.report.ReportService;
import org.facenet.util.TemplateFileUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    private final TemplateImportRepository templateImportRepository;
    private final OrganizationSettingsRepository organizationRepository;
    private final WeighingLogRepository weighingLogRepository;
    private final ScaleRepository scaleRepository;
    private final ScaleConfigRepository scaleConfigRepository;
    private final ReportService scaleReportService;
    private final TemplateFileUtil templateFileUtil;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public record WordTemplateFile(String filename, byte[] content) {}

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

    // ===== WORD template management APIs =====

    @Transactional(readOnly = true)
    public List<ReportTemplateDto.WordTemplateResponse> listWordTemplates(boolean activeOnly) {
        List<ReportTemplate> templates = activeOnly
                ? templateRepository.findByReportTypeAndIsActiveTrue(ReportTemplate.ReportType.WORD)
                : templateRepository.findAll().stream()
                    .filter(t -> t.getReportType() == ReportTemplate.ReportType.WORD)
                    .toList();

        return templates.stream().map(this::toWordTemplateResponse).toList();
    }

    @Transactional(readOnly = true)
    public ReportTemplateDto.WordTemplateResponse getWordTemplateResponse(Long id) {
        ReportTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found: " + id));
        if (template.getReportType() != ReportTemplate.ReportType.WORD) {
            throw new IllegalArgumentException("Template is not WORD type: " + id);
        }
        return toWordTemplateResponse(template);
    }

    @Transactional
    public ReportTemplateDto.WordTemplateResponse createWordTemplate(ReportTemplateDto.CreateWordTemplateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        String code = request.getCode() != null ? request.getCode().trim() : null;
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("code must not be blank");
        }
        if (templateRepository.findByCode(code).isPresent()) {
            throw new IllegalArgumentException("Template code already exists: " + code);
        }

        ReportTemplate template = ReportTemplate.builder()
                .code(code)
                .name(request.getName())
                .description(request.getDescription())
                .titleTemplate(request.getTitleTemplate())
                .reportType(ReportTemplate.ReportType.WORD)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .build();

        template = templateRepository.save(template);

        if (Boolean.TRUE.equals(template.getIsDefault())) {
            unsetOtherDefaults(template.getId(), ReportTemplate.ReportType.WORD);
        }

        return toWordTemplateResponse(template);
    }

    @Transactional
    public ReportTemplateDto.WordTemplateResponse updateWordTemplate(Long id, ReportTemplateDto.UpdateWordTemplateRequest request) {
        ReportTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found: " + id));
        if (template.getReportType() != ReportTemplate.ReportType.WORD) {
            throw new IllegalArgumentException("Template is not WORD type: " + id);
        }
        if (request.getName() != null) {
            template.setName(request.getName());
        }
        if (request.getDescription() != null) {
            template.setDescription(request.getDescription());
        }
        if (request.getTitleTemplate() != null) {
            template.setTitleTemplate(request.getTitleTemplate());
        }
        if (request.getIsActive() != null) {
            template.setIsActive(request.getIsActive());
        }
        template = templateRepository.save(template);
        return toWordTemplateResponse(template);
    }

    @Transactional
    public ReportTemplateDto.WordTemplateResponse setDefaultWordTemplate(Long id) {
        ReportTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found: " + id));
        if (template.getReportType() != ReportTemplate.ReportType.WORD) {
            throw new IllegalArgumentException("Template is not WORD type: " + id);
        }
        template.setIsDefault(true);
        template = templateRepository.save(template);
        unsetOtherDefaults(template.getId(), ReportTemplate.ReportType.WORD);
        return toWordTemplateResponse(template);
    }

    @Transactional
    public ReportTemplateDto.WordTemplateResponse uploadWordTemplateFile(Long id, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file must not be empty");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && !originalFilename.toLowerCase(Locale.ROOT).endsWith(".docx")) {
            throw new IllegalArgumentException("Only .docx templates are supported");
        }

        ReportTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found: " + id));
        if (template.getReportType() != ReportTemplate.ReportType.WORD) {
            throw new IllegalArgumentException("Template is not WORD type: " + id);
        }

        template.setWordTemplateFilename(originalFilename);
        template.setWordTemplateContent(file.getBytes());
        template = templateRepository.save(template);
        return toWordTemplateResponse(template);
    }

    @Transactional(readOnly = true)
    public WordTemplateFile getWordTemplateFile(Long id) {
        ReportTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found: " + id));
        if (template.getReportType() != ReportTemplate.ReportType.WORD) {
            throw new IllegalArgumentException("Template is not WORD type: " + id);
        }
        byte[] content = template.getWordTemplateContent();
        if (content == null || content.length == 0) {
            throw new IllegalStateException("WORD template file is not configured for template: " + id);
        }
        return new WordTemplateFile(template.getWordTemplateFilename(), content);
    }

    @Transactional
    public void deleteWordTemplate(Long id) {
        ReportTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found: " + id));
        if (template.getReportType() != ReportTemplate.ReportType.WORD) {
            throw new IllegalArgumentException("Template is not WORD type: " + id);
        }
        
        // Soft delete: set is_active to false
        template.setIsActive(false);
        templateRepository.save(template);
    }

    private void unsetOtherDefaults(Long keepTemplateId, ReportTemplate.ReportType type) {
        templateRepository.findByReportTypeAndIsDefaultTrue(type).ifPresent(existing -> {
            if (!Objects.equals(existing.getId(), keepTemplateId)) {
                existing.setIsDefault(false);
                templateRepository.save(existing);
            }
        });
    }

    private ReportTemplateDto.WordTemplateResponse toWordTemplateResponse(ReportTemplate template) {
        boolean hasFile = template.getWordTemplateContent() != null && template.getWordTemplateContent().length > 0;
        return ReportTemplateDto.WordTemplateResponse.builder()
                .id(template.getId())
                .code(template.getCode())
                .name(template.getName())
                .description(template.getDescription())
                .titleTemplate(template.getTitleTemplate())
                .isActive(template.getIsActive())
                .isDefault(template.getIsDefault())
                .wordTemplateFilename(template.getWordTemplateFilename())
                .hasWordTemplateFile(hasFile)
                .build();
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
            ReportExportRequest request
    ) {
        if (template == null) {
            throw new IllegalArgumentException("template must not be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }

        OffsetDateTime startTime = request.getStartTime();
        OffsetDateTime endTime = request.getEndTime();
        List<Long> scaleIds = request.getScaleIds();
        String preparedBy = request.getPreparedBy();

        boolean shouldUseIntervalEngine = Boolean.TRUE.equals(request.getIntervalReport())
            || request.getTimeInterval() == ReportExportRequest.TimeInterval.SHIFT
            || (request.getAggregationByField() != null && !request.getAggregationByField().isEmpty())
            || request.getAggregationMethod() == ReportExportRequest.AggregationMethod.ABS;
        
        log.info("Building report data with template: {} for period {} to {}", 
                template.getCode(), startTime, endTime);
        
        // Validate inputs
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Start time and end time must not be null");
        }
        
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }

        if (shouldUseIntervalEngine) {
            log.info("Using interval report engine for export (intervalReport={}, timeInterval={}, hasPerFieldAgg={})",
                    request.getIntervalReport(), request.getTimeInterval(),
                    request.getAggregationByField() != null && !request.getAggregationByField().isEmpty());
            return buildIntervalEngineReportData(template, request);
        }

        OrganizationSettings org = getOrganizationSettings();

        // Fetch weighing logs
        log.info("Fetching weighing logs from {} to {} for scales: {}", 
                startTime, endTime, scaleIds != null ? scaleIds : "ALL");
        
        // Log timezone info for debugging
        log.info("Timezone info - startTime zone: {}, endTime zone: {}", 
                startTime.getOffset(), endTime.getOffset());
        
        List<WeighingLog> logs = weighingLogRepository.findAllInTimeRange(startTime, endTime);
        if (logs == null) {
            logs = Collections.emptyList();
        }
        
        log.info("Fetched {} weighing logs before filtering", logs.size());

        // Filter by scale IDs if specified
        if (scaleIds != null && !scaleIds.isEmpty()) {
            logs = logs.stream()
                    .filter(log -> scaleIds.contains(log.getScaleId()))
                    .collect(Collectors.toList());
        }

        log.info("Found {} weighing logs for period {} to {}", logs.size(), startTime, endTime);
        
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
        } else {
            log.warn("No weighing logs found in time range {} to {}", startTime, endTime);
        }
        
        // Determine which data fields have actual data (not null/empty)
        boolean[] hasData = determineActiveDataFields(logs);
        log.info("Active data fields: data_1={}, data_2={}, data_3={}, data_4={}, data_5={}", 
                hasData[0], hasData[1], hasData[2], hasData[3], hasData[4]);

        // Fetch scale information first
        List<Long> uniqueScaleIds = logs.stream()
            .map(WeighingLog::getScaleId)
            .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<Scale> scales = scaleRepository.findAllById(uniqueScaleIds);
        Map<Long, Scale> scaleMap = scales.stream()
                .collect(Collectors.toMap(Scale::getId, s -> s));

        // Apply global aggregation method from request to all WEIGHING_DATA columns
        ReportColumn.AggregationType effectiveAggregationType = mapAggregationMethod(request.getAggregationMethod());

        // Build report rows - group by time interval if specified
        List<ReportData.ReportRow> rows = new ArrayList<>();
        int rowNumber = 1;

        if (request.getTimeInterval() != null) {
            // Group by time interval AND scale
            log.info("Grouping data by time interval: {}", request.getTimeInterval());
            
            // Create time-scale groups
            Map<String, Map<Long, List<WeighingLog>>> logsByPeriodAndScale = new TreeMap<>();
            
            for (WeighingLog logEntry : logs) {
                if (logEntry == null || logEntry.getScaleId() == null || logEntry.getCreatedAt() == null) {
                    continue;
                }
                String period = truncateToInterval(logEntry.getCreatedAt(), request.getTimeInterval());
                logsByPeriodAndScale
                        .computeIfAbsent(period, k -> new HashMap<>())
                        .computeIfAbsent(logEntry.getScaleId(), k -> new ArrayList<>())
                        .add(logEntry);
            }
            
            log.info("Created {} time periods with data", logsByPeriodAndScale.size());
            
            // Generate rows for each period-scale combination
            for (Map.Entry<String, Map<Long, List<WeighingLog>>> periodEntry : logsByPeriodAndScale.entrySet()) {
                String period = periodEntry.getKey();
                
                for (Map.Entry<Long, List<WeighingLog>> scaleEntry : periodEntry.getValue().entrySet()) {
                    Long scaleId = scaleEntry.getKey();
                    List<WeighingLog> periodLogs = scaleEntry.getValue();
                    Scale scale = scaleMap.get(scaleId);
                    
                    if (scale == null) {
                        log.warn("Scale {} not found, skipping", scaleId);
                        continue;
                    }
                    
                        ReportData.ReportRow row = buildRowFromTemplate(
                            template, scale, periodLogs, rowNumber++, period, effectiveAggregationType);
                    rows.add(row);
                }
            }
        } else {
            // No time interval - aggregate all data per scale (original behavior)
            log.info("No time interval specified, aggregating all data per scale");
            
            Map<Long, List<WeighingLog>> logsByScale = logs.stream()
                    .collect(Collectors.groupingBy(WeighingLog::getScaleId));
            
            for (Map.Entry<Long, List<WeighingLog>> entry : logsByScale.entrySet()) {
                Long scaleId = entry.getKey();
                List<WeighingLog> scaleLogs = entry.getValue();
                Scale scale = scaleMap.get(scaleId);

                if (scale == null) {
                    log.warn("Scale {} not found, skipping", scaleId);
                    continue;
                }

                ReportData.ReportRow row = buildRowFromTemplate(
                    template, scale, scaleLogs, rowNumber++, null, effectiveAggregationType);
                rows.add(row);
            }
        }

        // Sort rows by scale ID
        rows.sort(Comparator.comparing(ReportData.ReportRow::getScaleId));

        // Calculate summary based on template
        ReportData.ReportSummary summary = calculateSummary(rows, template);

        // Build title from request first, fallback to template
        String title = request.getReportTitle() != null && !request.getReportTitle().trim().isEmpty()
                ? request.getReportTitle()
                : buildTitle(template, startTime, endTime);

        // Build metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("totalLogs", logs.size());
        metadata.put("dateRange", formatDateRange(startTime, endTime));
        metadata.put("organizationName", org.getCompanyName());
        metadata.put("watermark", org.getWatermarkText());
        metadata.put("template", template);
        metadata.put("aggregationMethod", request.getAggregationMethod() != null ? request.getAggregationMethod().name() : null);
        metadata.put("aggregationMethodLabel", formatAggregationMethodLabel(request.getAggregationMethod()));
        
        // Get column names from scale config (only for fields with config)
        String[] columnNames = getColumnNamesFromConfig(uniqueScaleIds.isEmpty() ? null : uniqueScaleIds.get(0));
        for (int i = 0; i < columnNames.length; i++) {
            columnNames[i] = sanitizeDisplayName(columnNames[i]);
        }
        
        // Set column names in metadata for reference (only non-null names)
        metadata.put("activeFields", hasData);
        List<String> activeColumnNames = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            if (columnNames[i] != null) {
                activeColumnNames.add(columnNames[i]);
            }
        }
        metadata.put("columnNames", activeColumnNames);

        // Use report code from request, fallback to auto-generated
        String reportCode = request.getReportCode() != null && !request.getReportCode().trim().isEmpty()
                ? request.getReportCode()
                : template.getCode() + "-" + System.currentTimeMillis();

        return ReportData.builder()
                .reportTitle(title)
                .reportCode(reportCode)
                .startTime(startTime)
                .endTime(endTime)
                .exportTime(OffsetDateTime.now())
                .preparedBy(request.getPreparedBy() != null && !request.getPreparedBy().trim().isEmpty() 
                        ? request.getPreparedBy() 
                        : "System")
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

    private ReportData buildIntervalEngineReportData(ReportTemplate template, ReportExportRequest request) {
        OffsetDateTime startTime = request.getStartTime();
        OffsetDateTime endTime = request.getEndTime();

        if (request.getTimeInterval() == null) {
            throw new IllegalArgumentException("timeInterval is required when using interval report export");
        }

        IntervalReportRequestDto.TimeInterval interval = switch (request.getTimeInterval()) {
            case SHIFT -> IntervalReportRequestDto.TimeInterval.SHIFT;
            case HOUR -> IntervalReportRequestDto.TimeInterval.HOUR;
            case DAY -> IntervalReportRequestDto.TimeInterval.DAY;
            case WEEK -> IntervalReportRequestDto.TimeInterval.WEEK;
            case MONTH, YEAR -> throw new IllegalArgumentException("Interval report export supports only SHIFT/HOUR/DAY/WEEK");
        };

        Map<String, IntervalReportRequestDto.AggregationMethod> aggregationByField = null;
        if (request.getAggregationByField() != null && !request.getAggregationByField().isEmpty()) {
            aggregationByField = new HashMap<>();
            for (Map.Entry<String, ReportExportRequest.AggregationMethod> e : request.getAggregationByField().entrySet()) {
                if (e.getKey() == null || e.getValue() == null) {
                    continue;
                }
                IntervalReportRequestDto.AggregationMethod mapped = switch (e.getValue()) {
                    case SUM -> IntervalReportRequestDto.AggregationMethod.SUM;
                    case AVG -> IntervalReportRequestDto.AggregationMethod.AVG;
                    case MAX -> IntervalReportRequestDto.AggregationMethod.MAX;
                    case MIN -> IntervalReportRequestDto.AggregationMethod.MIN;
                    case COUNT -> IntervalReportRequestDto.AggregationMethod.COUNT;
                    case ABS -> IntervalReportRequestDto.AggregationMethod.ABS;
                };
                aggregationByField.put(e.getKey(), mapped);
            }
            if (aggregationByField.isEmpty()) {
                aggregationByField = null;
            }
        }

        IntervalReportRequestDto intervalRequest = IntervalReportRequestDto.builder()
                .scaleIds(request.getScaleIds())
                .fromTime(startTime)
                .toTime(endTime)
                .interval(interval)
                .aggregationByField(aggregationByField)
                .page(0)
                .size(Integer.MAX_VALUE)  // Get all data for export
                .build();

        org.facenet.common.pagination.PageResponseDto<IntervalReportResponseDto.Row> pagedResponse = 
                scaleReportService.generateIntervalReport(intervalRequest);

        List<ReportData.ReportRow> rows = new ArrayList<>();
        int rowNumber = 1;
        if (pagedResponse.getData() != null) {
            for (IntervalReportResponseDto.Row r : pagedResponse.getData()) {
                IntervalReportResponseDto.ScaleInfo s = r != null ? r.getScale() : null;
                Map<String, IntervalReportResponseDto.DataFieldValue> dv = r != null ? r.getDataValues() : null;

                rows.add(ReportData.ReportRow.builder()
                        .rowNumber(rowNumber++)
                        .scaleId(s != null ? s.getId() : null)
                        .scaleCode(s != null && s.getId() != null ? "SCALE-" + s.getId() : null)
                        .scaleName(s != null ? s.getName() : null)
                        .location(s != null && s.getLocation() != null ? s.getLocation().getName() : "N/A")
                        .period(r != null ? r.getPeriod() : null)
                        .recordCount(r != null ? r.getRecordCount() : 0)
                        .data1Total(parseNullableDouble(dv != null ? dv.get("data_1") : null))
                        .data2Total(parseNullableDouble(dv != null ? dv.get("data_2") : null))
                        .data3Total(parseNullableDouble(dv != null ? dv.get("data_3") : null))
                        .data4Total(parseNullableDouble(dv != null ? dv.get("data_4") : null))
                        .data5Total(parseNullableDouble(dv != null ? dv.get("data_5") : null))
                        .lastTime(null)
                        .build());
            }
        }

        rows.sort(Comparator
                .comparing(ReportData.ReportRow::getPeriod, Comparator.nullsLast(String::compareTo))
                .thenComparing(ReportData.ReportRow::getScaleId, Comparator.nullsLast(Long::compareTo)));

        ReportData.ReportSummary summary = calculateSummary(rows, template);

        OrganizationSettings org = getOrganizationSettings();
        
        // Build title from request first, fallback to template
        String title = request.getReportTitle() != null && !request.getReportTitle().trim().isEmpty()
                ? request.getReportTitle()
                : buildTitle(template, startTime, endTime);

        // Build aggregation metadata from request
        Map<String, String> aggregationByFieldMetadata = new HashMap<>();
        if (aggregationByField != null) {
            for (Map.Entry<String, IntervalReportRequestDto.AggregationMethod> e : aggregationByField.entrySet()) {
                aggregationByFieldMetadata.put(e.getKey(), e.getValue().name());
            }
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("totalLogs", rows.stream().mapToInt(r -> r.getRecordCount() != null ? r.getRecordCount() : 0).sum());
        metadata.put("dateRange", formatDateRange(startTime, endTime));
        metadata.put("organizationName", org.getCompanyName());
        metadata.put("watermark", org.getWatermarkText());
        metadata.put("template", template);
        metadata.put("intervalEngine", true);
        metadata.put("timeInterval", request.getTimeInterval() != null ? request.getTimeInterval().name() : null);
        metadata.put("aggregationByField", aggregationByFieldMetadata);

        // Column names: prefer first row (already applies special "Khối lượng" rename)
        String data1Name = null;
        String data2Name = null;
        String data3Name = null;
        String data4Name = null;
        String data5Name = null;

        if (!rows.isEmpty() && pagedResponse.getData() != null && !pagedResponse.getData().isEmpty()) {
            IntervalReportResponseDto.Row first = pagedResponse.getData().get(0);
            if (first != null && first.getDataValues() != null) {
                data1Name = first.getDataValues().get("data_1") != null ? first.getDataValues().get("data_1").getName() : null;
                data2Name = first.getDataValues().get("data_2") != null ? first.getDataValues().get("data_2").getName() : null;
                data3Name = first.getDataValues().get("data_3") != null ? first.getDataValues().get("data_3").getName() : null;
                data4Name = first.getDataValues().get("data_4") != null ? first.getDataValues().get("data_4").getName() : null;
                data5Name = first.getDataValues().get("data_5") != null ? first.getDataValues().get("data_5").getName() : null;
            }
        }

        // Fallback to default names if not set
        if (data1Name == null) data1Name = "Data 1";
        if (data2Name == null) data2Name = "Data 2";
        if (data3Name == null) data3Name = "Data 3";
        if (data4Name == null) data4Name = "Data 4";
        if (data5Name == null) data5Name = "Data 5";

        data1Name = sanitizeDisplayName(data1Name);
        data2Name = sanitizeDisplayName(data2Name);
        data3Name = sanitizeDisplayName(data3Name);
        data4Name = sanitizeDisplayName(data4Name);
        data5Name = sanitizeDisplayName(data5Name);

        // Use report code from request, fallback to auto-generated
        String reportCode = request.getReportCode() != null && !request.getReportCode().trim().isEmpty()
                ? request.getReportCode()
                : template.getCode() + "-" + System.currentTimeMillis();

        return ReportData.builder()
                .reportTitle(title)
                .reportCode(reportCode)
                .startTime(startTime)
                .endTime(endTime)
                .exportTime(OffsetDateTime.now())
                .preparedBy(request.getPreparedBy() != null ? request.getPreparedBy() : "System")
                .rows(rows)
                .summary(summary)
                .metadata(metadata)
                .data1Name(data1Name)
                .data2Name(data2Name)
                .data3Name(data3Name)
                .data4Name(data4Name)
                .data5Name(data5Name)
                .build();
    }

    private static Double parseNullableDouble(IntervalReportResponseDto.DataFieldValue value) {
        if (value == null || value.getValue() == null) {
            return 0.0;
        }
        String s = value.getValue().trim();
        if (s.isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(s);
        } catch (Exception ignored) {
            return 0.0;
        }
    }

    private static String sanitizeDisplayName(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        // Treat common template/placeholder-looking values as "not configured"
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("report.") || lower.contains("${") || lower.contains("th:")) {
            return null;
        }

        return trimmed;
    }
    
    /**
     * Determine which data fields have actual data across all logs
     * Returns boolean array [data_1, data_2, data_3, data_4, data_5]
     */
    private boolean[] determineActiveDataFields(List<WeighingLog> logs) {
        boolean[] hasData = new boolean[5];
        
        if (logs.isEmpty()) {
            return hasData; // All false
        }
        
        // Check each data field across all logs
        for (WeighingLog log : logs) {
            if (!hasData[0] && isNotEmpty(log.getData1())) hasData[0] = true;
            if (!hasData[1] && isNotEmpty(log.getData2())) hasData[1] = true;
            if (!hasData[2] && isNotEmpty(log.getData3())) hasData[2] = true;
            if (!hasData[3] && isNotEmpty(log.getData4())) hasData[3] = true;
            if (!hasData[4] && isNotEmpty(log.getData5())) hasData[4] = true;
            
            // Early exit if all fields have data
            if (hasData[0] && hasData[1] && hasData[2] && hasData[3] && hasData[4]) {
                break;
            }
        }
        
        return hasData;
    }
    
    /**
     * Check if string value is not empty (not null, not empty, not "null")
     */
    private boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty() && !value.equalsIgnoreCase("null");
    }

    /**
     * Truncate timestamp to time interval for grouping
     */
    private String truncateToInterval(OffsetDateTime timestamp, ReportExportRequest.TimeInterval interval) {
        if (timestamp == null || interval == null) {
            return "Unknown";
        }
        
        DateTimeFormatter formatter;
        OffsetDateTime truncated;
        
        switch (interval) {
            case HOUR -> {
                truncated = timestamp.truncatedTo(java.time.temporal.ChronoUnit.HOURS);
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00");
            }
            case DAY -> {
                truncated = timestamp.truncatedTo(java.time.temporal.ChronoUnit.DAYS);
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            }
            case WEEK -> {
                // Week: Monday 00:00:00
                truncated = timestamp.with(java.time.DayOfWeek.MONDAY)
                        .truncatedTo(java.time.temporal.ChronoUnit.DAYS);
                formatter = DateTimeFormatter.ofPattern("yyyy-'W'ww");
            }
            case MONTH -> {
                truncated = timestamp.withDayOfMonth(1)
                        .truncatedTo(java.time.temporal.ChronoUnit.DAYS);
                formatter = DateTimeFormatter.ofPattern("yyyy-MM");
            }
            case YEAR -> {
                truncated = timestamp.withDayOfYear(1)
                        .truncatedTo(java.time.temporal.ChronoUnit.DAYS);
                formatter = DateTimeFormatter.ofPattern("yyyy");
            }
            default -> {
                return timestamp.toString();
            }
        }
        
        return truncated.format(formatter);
    }

    /**
     * Build row from template columns
     */
    private ReportData.ReportRow buildRowFromTemplate(
            ReportTemplate template,
            Scale scale,
            List<WeighingLog> logs,
            int rowNumber,
            String period,
            ReportColumn.AggregationType aggregationType
    ) {
        ReportData.ReportRow.ReportRowBuilder builder = ReportData.ReportRow.builder()
                .rowNumber(rowNumber)
                .scaleId(scale.getId())
                .scaleCode("SCALE-" + scale.getId())
                .scaleName(scale.getName())
                .location(scale.getLocation() != null ? scale.getLocation().getName() : "N/A")
                .recordCount(logs.size())
                .period(period)  // Add time period
                .lastTime(logs.stream()
                        .map(WeighingLog::getLastTime)
                        .max(OffsetDateTime::compareTo)
                        .orElse(null));

        // Calculate aggregations for data fields based on columns
        for (ReportColumn column : template.getColumns()) {
            if (column.getDataSource() == ReportColumn.DataSource.WEIGHING_DATA) {
                Double value = calculateAggregation(logs, column, aggregationType);
                if (column.getDataField()== null) {
                    log.warn("Skip WEIGHING_DATA column {} due to null dataField", column.getColumnKey());
                    continue;
                }
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
    private Double calculateAggregation(List<WeighingLog> logs, ReportColumn column, ReportColumn.AggregationType overrideAggType) {
        String field = column.getDataField();
        ReportColumn.AggregationType aggType = overrideAggType != null ? overrideAggType : column.getAggregationType();
        if (aggType == null) {
            aggType = ReportColumn.AggregationType.SUM;
        }
        if (!logs.isEmpty()) {
            for (int i = 0; i < Math.min(3, logs.size()); i++) {
                WeighingLog logEntry = logs.get(i);
                if (field == null) continue;
                String rawValue = switch (field) {
                    case "data_1" -> logEntry.getData1();
                    case "data_2" -> logEntry.getData2();
                    case "data_3" -> logEntry.getData3();
                    case "data_4" -> logEntry.getData4();
                    case "data_5" -> logEntry.getData5();
                    default -> null;
                };
                log.debug("  Log #{}: {} = '{}'", i + 1, field, rawValue);
            }
        }
        
        List<Double> values = logs.stream()
                .map(log -> extractValue(log, field))
                .filter(Objects::nonNull)
                .filter(v -> v != 0.0) // Filter out zeros for debugging
                .collect(Collectors.toList());

        if (values.isEmpty()) {
            log.debug("No non-zero values extracted from field {} (all null, invalid, or zero). Returning 0.0", field);
            return 0.0;
        }
        
        log.debug("Extracted {} non-zero values from field {}: {}", values.size(), field,
                values.size() <= 5 ? values : values.subList(0, 5) + "...");

        Double result = switch (aggType) {
            case SUM -> values.stream().reduce(0.0, Double::sum);
            case AVG -> values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            case MAX -> values.stream().max(Double::compare).orElse(0.0);
            case MIN -> values.stream().min(Double::compare).orElse(0.0);
            case COUNT -> (double) values.size();
            case NONE -> values.isEmpty() ? 0.0 : values.get(0);
        };
        
        log.debug("Result for {} on {}: {}", aggType, field, result);
        return result;
    }

    /**
     * Extract value from weighing log by field name
     */
    private Double extractValue(WeighingLog weighingLog, String field) {
        if (field == null) {
            log.warn("Field parameter is null in extractValue");
            return null;
        }
        
        String rawValue = switch (field) {
            case "data_1" -> weighingLog.getData1();
            case "data_2" -> weighingLog.getData2();
            case "data_3" -> weighingLog.getData3();
            case "data_4" -> weighingLog.getData4();
            case "data_5" -> weighingLog.getData5();
            default -> null;
        };

        if (rawValue != null && log.isDebugEnabled()) {
            log.debug("Extracting {} from weighing log: raw value = '{}' (type: {})", 
                    field, rawValue, rawValue.getClass().getSimpleName());
        }
        
        Double result = parseJsonbValue(rawValue);
        
        if (log.isDebugEnabled()) {
            log.debug("Parsed {} from raw '{}' -> {}", field, rawValue, result);
        }
        
        return result;
    }

    /**
     * Parse VARCHAR string to Double
     * Handles multiple formats:
     * - Plain string: "150.5" or "131075"
     * - JSON string: "\"150.5\""
     * - Numeric: 150.5
     * - null, empty, "null" -> 0.0
     * 
     * Data in weighing_log is VARCHAR, so we parse string to number
     */
    private Double parseJsonbValue(String jsonbValue) {
        if (jsonbValue == null) {
            log.debug("Value is null, returning 0.0");
            return 0.0;
        }
        
        String trimmed = jsonbValue.trim();
        if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("null")) {
            log.debug("Value is empty or 'null', returning 0.0");
            return 0.0;
        }

        try {
            String cleanValue = trimmed;
            
            log.debug("Parsing value: original='{}', length={}", cleanValue, cleanValue.length());
            
            // Remove JSON escape quotes: \"123\" -> 123
            if (cleanValue.startsWith("\\\"") && cleanValue.endsWith("\\\"")) {
                cleanValue = cleanValue.substring(2, cleanValue.length() - 2);
                log.debug("Removed escape quotes: '{}'", cleanValue);
            }
            // Remove regular quotes: "123" -> 123
            else if (cleanValue.startsWith("\"") && cleanValue.endsWith("\"")) {
                cleanValue = cleanValue.substring(1, cleanValue.length() - 1);
                log.debug("Removed regular quotes: '{}'", cleanValue);
            }
            
            cleanValue = cleanValue.trim();
            if (cleanValue.isEmpty()) {
                log.debug("Empty value after cleaning, returning 0.0");
                return 0.0;
            }
            
            // Parse VARCHAR string to double
            double result = Double.parseDouble(cleanValue);
            log.debug("Successfully parsed '{}' -> {}", jsonbValue, result);
            return result;
            
        } catch (NumberFormatException e) {
            log.error("Failed to parse VARCHAR value: '{}' (length={}) - Error: {}", 
                    jsonbValue, jsonbValue.length(), e.getMessage());
            return 0.0;
        }
    }

    private static ReportColumn.AggregationType mapAggregationMethod(ReportExportRequest.AggregationMethod method) {
        if (method == null) {
            return ReportColumn.AggregationType.SUM;
        }
        return switch (method) {
            case SUM -> ReportColumn.AggregationType.SUM;
            case AVG -> ReportColumn.AggregationType.AVG;
            case MAX -> ReportColumn.AggregationType.MAX;
            case MIN -> ReportColumn.AggregationType.MIN;
            case COUNT -> ReportColumn.AggregationType.COUNT;
            case ABS -> ReportColumn.AggregationType.SUM;
        };
    }

    private static String formatAggregationMethodLabel(ReportExportRequest.AggregationMethod method) {
        if (method == null) {
            return "Tổng (SUM)";
        }
        return switch (method) {
            case SUM -> "Tổng (SUM)";
            case AVG -> "Trung bình (AVG)";
            case MAX -> "Lớn nhất (MAX)";
            case MIN -> "Nhỏ nhất (MIN)";
            case COUNT -> "Đếm (COUNT)";
            case ABS -> "Độ lệch (ABS)";
        };
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
     * Get column names from scale configs
     * Returns array of 5 names for data_1 to data_5
     * If config is null/empty or is_used=false, returns null for that position
     */
    private String[] getColumnNamesFromConfig(Long sampleScaleId) {
        String[] names = new String[5]; // All null by default
        
        if (sampleScaleId == null) {
            return names;
        }
        
        try {
            Optional<ScaleConfig> configOpt = scaleConfigRepository.findById(sampleScaleId);
            if (configOpt.isEmpty()) {
                log.debug("No config found for scale {}", sampleScaleId);
                return names;
            }
            
            ScaleConfig config = configOpt.get();
            
            // Extract name only if config exists and is_used=true
            names[0] = extractNameFromDataConfig(config.getData1());
            names[1] = extractNameFromDataConfig(config.getData2());
            names[2] = extractNameFromDataConfig(config.getData3());
            names[3] = extractNameFromDataConfig(config.getData4());
            names[4] = extractNameFromDataConfig(config.getData5());

            log.debug("Loaded column names from scale {}: data1={}, data2={}, data3={}, data4={}, data5={}", 
                    sampleScaleId, names[0], names[1], names[2], names[3], names[4]);
            return names;
        } catch (Exception e) {
            log.warn("Failed to get column names from config for scale {}: {}", sampleScaleId, e.getMessage());
            return names;
        }
    }
    
    /**
     * Extract name from data config JSON
     * Config format: {"name": "Weight", "is_used": true, "data_type": "integer", ...}
     * Only return name if config is not empty and is_used = true
     */
    private String extractNameFromDataConfig(Map<String, Object> dataConfig) {
        if (dataConfig == null || dataConfig.isEmpty()) {
            return null;
        }
        // Check if field is used
        Object isUsedObj = dataConfig.get("is_used");
        boolean isUsed = isUsedObj != null && (isUsedObj instanceof Boolean ? (Boolean) isUsedObj : Boolean.parseBoolean(isUsedObj.toString()));

        if (!isUsed) {
            log.debug("Field is not used (is_used=false), returning null");
            return null;
        }

        // Get name field
        Object nameObj = dataConfig.get("name");
        if (nameObj == null) {
            log.debug("Field name is null, returning null");
            return null;
        }

        String name = nameObj.toString().trim();
        if (name.isEmpty()) {
            log.debug("Field name is empty, returning null");
            return null;
        }

        log.debug("Using configured field name: {}", name);
        return name;
    }
    
    /**
     * Extract name from data config JSON
     * Config format: {"name": "Weight", "is_used": true, "data_type": "integer", ...}
     * Only return name if is_used = true, otherwise return default
     */
//    private String extractNameFromDataConfig(Map<String, Object> dataConfig, String defaultName) {
//        if (dataConfig == null) {
//            return defaultName;
//        }
//
//        // Check if field is used
//        Object isUsedObj = dataConfig.get("is_used");
//        boolean isUsed = isUsedObj != null && (isUsedObj instanceof Boolean ? (Boolean) isUsedObj : Boolean.parseBoolean(isUsedObj.toString()));
//
//        if (!isUsed) {
//            log.debug("Field is not used (is_used=false), returning default name");
//            return defaultName;
//        }
//
//        // Get name field
//        Object nameObj = dataConfig.get("name");
//        if (nameObj == null) {
//            log.debug("Field name is null, returning default name");
//            return defaultName;
//        }
//
//        String name = nameObj.toString().trim();
//        if (name.isEmpty()) {
//            log.debug("Field name is empty, returning default name");
//            return defaultName;
//        }
//
//        log.debug("Using configured field name: {}", name);
//        return name;
//    }

    /**
     * Get templates by type
     */
    @Transactional(readOnly = true)
    public List<ReportTemplate> getTemplatesByType(ReportTemplate.ReportType type) {
        return templateRepository.findByReportTypeAndIsActiveTrue(type);
    }

    // ===== TEMPLATE IMPORT OPERATIONS =====

    /**
     * Import template file and save to resources
     * Stores metadata in database
     */
    @Transactional
    public ReportTemplateDto.TemplateImportResponse importTemplateFile(
            ReportTemplateDto.TemplateImportRequest request,
            MultipartFile file) throws IOException {
        
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Template file must not be empty");
        }

        String templateCode = request.getTemplateCode().trim();
        
        // Validate file
        if (!templateFileUtil.isValidTemplateFile(file.getOriginalFilename())) {
            throw new IllegalArgumentException("Invalid template file type. Supported: .docx, .doc, .xlsx, .pdf, .html, .htm");
        }

        // Check if template code already exists
        if (templateRepository.findByCode(templateCode).isPresent()) {
            throw new IllegalArgumentException("Template code already exists: " + templateCode);
        }

        byte[] fileContent = file.getBytes();
        String fileHash = templateFileUtil.calculateFileHash(fileContent);

        // Check for duplicate file (by hash)
        if (templateImportRepository.existsByFileHash(fileHash)) {
            throw new IllegalArgumentException("This template file has already been imported (duplicate detected by hash)");
        }

        try {
            // Save file to resources directory
            String resourcePath = templateFileUtil.saveTemplateToResources(
                    fileContent,
                    templateCode,
                    file.getOriginalFilename()
            );

            // Check if resource path already exists
            if (templateImportRepository.existsByResourcePath(resourcePath)) {
                throw new IllegalArgumentException("Resource path already exists: " + resourcePath);
            }

            // Create report template entity
            ReportTemplate template = ReportTemplate.builder()
                    .code(templateCode)
                    .name(request.getTemplateName())
                    .description(request.getDescription())
                    .titleTemplate(request.getTitleTemplate())
                    .reportType(ReportTemplate.ReportType.WORD)
                    .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                    .isDefault(false)
                    .build();

            template = templateRepository.save(template);

            // Create import record
            TemplateImport templateImport = TemplateImport.builder()
                    .template(template)
                    .templateCode(templateCode)
                    .originalFilename(file.getOriginalFilename())
                    .resourcePath(resourcePath)
                    .filePath(templateFileUtil.getAbsolutePath(resourcePath))
                    .fileSizeBytes(templateFileUtil.getFileSize(fileContent))
                    .fileHash(fileHash)
                    .importStatus(TemplateImport.ImportStatus.ACTIVE)
                    .importDate(OffsetDateTime.now())
                    .importNotes(request.getImportNotes())
                    .isActive(true)
                    .templateType(parseTemplateType(request.getTemplateType()))
                    .build();

            templateImport = templateImportRepository.save(templateImport);

            log.info("Template imported successfully: code={}, resourcePath={}, size={}", 
                    templateCode, resourcePath, fileContent.length);

            return toTemplateImportResponse(templateImport);

        } catch (IOException e) {
            log.error("Error importing template file: {}", templateCode, e);
            throw new RuntimeException("Failed to import template file: " + e.getMessage(), e);
        }
    }

    /**
     * Get all imported templates
     */
    @Transactional(readOnly = true)
    public List<ReportTemplateDto.TemplateImportListResponse> listImportedTemplates() {
        return templateImportRepository.findByIsActiveTrueOrderByImportDateDesc()
                .stream()
                .map(this::toTemplateImportListResponse)
                .toList();
    }

    /**
     * Get all imported templates with optional filter by template type
     */
    @Transactional(readOnly = true)
    public List<ReportTemplateDto.TemplateImportListResponse> listImportedTemplates(String templateType) {
        if (templateType == null || templateType.trim().isEmpty()) {
            // No filter, return all active templates
            return templateImportRepository.findByIsActiveTrueOrderByImportDateDesc()
                    .stream()
                    .map(this::toTemplateImportListResponse)
                    .toList();
        }

        // Parse template type
        TemplateImport.TemplateType type = TemplateImport.TemplateType.fromDisplayName(templateType);
        if (type == null) {
            // Try to parse as enum name
            try {
                type = TemplateImport.TemplateType.valueOf(templateType.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid template type: " + templateType + 
                    ". Valid values: SHIFT_REPORT, WEIGHING_REPORT, Báo cáo ca, Báo cáo cân");
            }
        }

        return templateImportRepository.findByIsActiveTrueAndTemplateTypeOrderByImportDateDesc(type)
                .stream()
                .map(this::toTemplateImportListResponse)
                .toList();
    }

    /**
     * Get imported template details
     */
    @Transactional(readOnly = true)
    public ReportTemplateDto.ImportedTemplateDetailsResponse getImportedTemplateDetails(Long importId) {
        TemplateImport templateImport = templateImportRepository.findById(importId)
                .orElseThrow(() -> new RuntimeException("Template import not found: " + importId));

        return ReportTemplateDto.ImportedTemplateDetailsResponse.builder()
                .importId(templateImport.getId())
                .template(toWordTemplateResponse(templateImport.getTemplate()))
                .originalFilename(templateImport.getOriginalFilename())
                .resourcePath(templateImport.getResourcePath())
                .fileSizeBytes(templateImport.getFileSizeBytes())
                .fileHash(templateImport.getFileHash())
                .importStatus(templateImport.getImportStatus().name())
                .importDate(templateImport.getImportDate())
                .importNotes(templateImport.getImportNotes())
                .build();
    }

    /**
     * Get import by template ID
     */
    @Transactional(readOnly = true)
    public ReportTemplateDto.TemplateImportResponse getImportByTemplateId(Long templateId) {
        TemplateImport templateImport = templateImportRepository.findByTemplateId(templateId)
                .orElseThrow(() -> new RuntimeException("No import found for template ID: " + templateId));

        return toTemplateImportResponse(templateImport);
    }

    /**
     * Get import by template code
     */
    @Transactional(readOnly = true)
    public List<ReportTemplateDto.TemplateImportListResponse> getImportsByTemplateCode(String templateCode) {
        return templateImportRepository.findByTemplateCode(templateCode)
                .stream()
                .map(this::toTemplateImportListResponse)
                .toList();
    }

    /**
     * Download imported template file
     */
    @Transactional(readOnly = true)
    public ReportTemplateService.TemplateFileRecord downloadImportedTemplate(Long importId) throws IOException {
        TemplateImport templateImport = templateImportRepository.findById(importId)
                .orElseThrow(() -> new RuntimeException("Template import not found: " + importId));

        byte[] fileContent = templateFileUtil.readTemplateFile(templateImport.getResourcePath());

        return new TemplateFileRecord(
                templateImport.getOriginalFilename(),
                fileContent,
                templateImport.getFileHash()
        );
    }

    /**
     * Archive imported template
     */
    @Transactional
    public ReportTemplateDto.TemplateImportResponse archiveImportedTemplate(Long importId) {
        TemplateImport templateImport = templateImportRepository.findById(importId)
                .orElseThrow(() -> new RuntimeException("Template import not found: " + importId));

        templateImport.setImportStatus(TemplateImport.ImportStatus.ARCHIVED);
        templateImport.setIsActive(false);
        templateImport = templateImportRepository.save(templateImport);

        log.info("Template import archived: {}", importId);

        return toTemplateImportResponse(templateImport);
    }

    /**
     * Delete imported template (soft delete)
     */
    @Transactional
    public void deleteImportedTemplate(Long importId) {
        TemplateImport templateImport = templateImportRepository.findById(importId)
                .orElseThrow(() -> new RuntimeException("Template import not found: " + importId));

        // Delete physical file from resources
        boolean fileDeleted = templateFileUtil.deleteTemplateFile(templateImport.getResourcePath());
        log.info("Template file deleted: {} (success={})", templateImport.getResourcePath(), fileDeleted);

        // Mark as deleted in database
        templateImport.setImportStatus(TemplateImport.ImportStatus.DELETED);
        templateImport.setIsActive(false);
        templateImportRepository.save(templateImport);

        // Delete template if no other imports reference it
        ReportTemplate template = templateImport.getTemplate();
        templateImportRepository.deleteByTemplateId(template.getId());
        templateRepository.delete(template);

        log.info("Template import deleted: {}", importId);
    }

    /**
     * Get template file from resources
     */
    @Transactional(readOnly = true)
    public byte[] getImportedTemplateFile(Long importId) throws IOException {
        TemplateImport templateImport = templateImportRepository.findById(importId)
                .orElseThrow(() -> new RuntimeException("Template import not found: " + importId));

        if (!templateFileUtil.fileExists(templateImport.getResourcePath())) {
            throw new RuntimeException("Template file not found in resources: " + templateImport.getResourcePath());
        }

        return templateFileUtil.readTemplateFile(templateImport.getResourcePath());
    }

    /**
     * Verify template file integrity
     */
    @Transactional(readOnly = true)
    public boolean verifyTemplateFileIntegrity(Long importId) throws IOException {
        TemplateImport templateImport = templateImportRepository.findById(importId)
                .orElseThrow(() -> new RuntimeException("Template import not found: " + importId));

        byte[] fileContent = templateFileUtil.readTemplateFile(templateImport.getResourcePath());
        String currentHash = templateFileUtil.calculateFileHash(fileContent);

        boolean isValid = currentHash.equals(templateImport.getFileHash());
        log.info("Template file integrity check: importId={}, valid={}", importId, isValid);

        if (!isValid) {
            // Mark as corrupted
            templateImport.setImportStatus(TemplateImport.ImportStatus.CORRUPTED);
            templateImportRepository.save(templateImport);
        }

        return isValid;
    }

    // ===== HELPER METHODS =====

    private ReportTemplateDto.TemplateImportResponse toTemplateImportResponse(TemplateImport templateImport) {
        return ReportTemplateDto.TemplateImportResponse.builder()
                .id(templateImport.getId())
                .templateId(templateImport.getTemplate().getId())
                .templateCode(templateImport.getTemplateCode())
                .originalFilename(templateImport.getOriginalFilename())
                .resourcePath(templateImport.getResourcePath())
                .fileSizeBytes(templateImport.getFileSizeBytes())
                .fileHash(templateImport.getFileHash())
                .importStatus(templateImport.getImportStatus().name())
                .importDate(templateImport.getImportDate())
                .importNotes(templateImport.getImportNotes())
                .isActive(templateImport.getIsActive())
                .templateType(templateImport.getTemplateType() != null ? 
                        templateImport.getTemplateType().getDisplayName() : null)
                .createdBy(templateImport.getCreatedBy())
                .createdAt(templateImport.getCreatedAt())
                .build();
    }

    private ReportTemplateDto.TemplateImportListResponse toTemplateImportListResponse(TemplateImport templateImport) {
        return ReportTemplateDto.TemplateImportListResponse.builder()
                .id(templateImport.getId())
                .templateCode(templateImport.getTemplateCode())
                .originalFilename(templateImport.getOriginalFilename())
                .resourcePath(templateImport.getResourcePath())
                .fileSizeBytes(templateImport.getFileSizeBytes())
                .importStatus(templateImport.getImportStatus().name())
                .importDate(templateImport.getImportDate())
                .isActive(templateImport.getIsActive())
                .templateType(templateImport.getTemplateType() != null ? 
                        templateImport.getTemplateType().getDisplayName() : null)
                .build();
    }
    
    /**
     * Parse template type from display name
     */
    private TemplateImport.TemplateType parseTemplateType(String templateType) {
        if (templateType == null || templateType.trim().isEmpty()) {
            return null;
        }
        TemplateImport.TemplateType type = TemplateImport.TemplateType.fromDisplayName(templateType.trim());
        if (type == null) {
            log.warn("Unknown template type: {}, setting to null", templateType);
        }
        return type;
    }

    public record TemplateFileRecord(String filename, byte[] content, String fileHash) {}
}
