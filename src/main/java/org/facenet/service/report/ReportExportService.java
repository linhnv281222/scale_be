package org.facenet.service.report;

import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.config.Configure;
import com.deepoove.poi.plugin.table.LoopRowTableRenderPolicy;
import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.facenet.dto.report.ReportData;
import org.facenet.dto.report.ReportExportRequest;
import org.facenet.entity.report.ReportTemplate;
import org.facenet.entity.report.TemplateImport;
import org.facenet.repository.report.TemplateImportRepository;
import org.facenet.util.TemplateFileUtil;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Main service for report export orchestration (with dynamic template support)
 * Coordinates data fetching and export to different formats
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportExportService {

    private final ReportDataService reportDataService;
    private final ReportTemplateService reportTemplateService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final PdfExportService pdfExportService;
    private final TemplateImportRepository templateImportRepository;
    private final TemplateFileUtil templateFileUtil;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    
    // Number format for Vietnamese locale
    private static final DecimalFormat NUMBER_FORMAT;
    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        NUMBER_FORMAT = new DecimalFormat("#,##0.00", symbols);
    }

    /**
     * Export report using imported template (NEW APPROACH)
     * Directly uses template file from template_imports table
     * Fills data from query results into the template
     */
    public byte[] exportReportWithImportedTemplate(ReportExportRequest request, Long importId) 
            throws IOException, DocumentException {
        log.info("Exporting report with imported template: importId={}, startTime={}, endTime={}", 
                importId, request.getStartTime(), request.getEndTime());

        // 1. Get template import record
        TemplateImport templateImport = templateImportRepository.findById(importId)
                .orElseThrow(() -> new RuntimeException("Template import not found for importId: " + importId));

        log.info("Found template import: id={}, resourcePath={}, filePath={}", 
                templateImport.getId(), 
                templateImport.getResourcePath(), 
                templateImport.getFilePath());

        // 2. Load template file
        byte[] templateFile;
        try {
            templateFile = templateFileUtil.readTemplateFile(templateImport.getResourcePath());
            log.info("Loaded template file from resourcePath: {} ({} bytes)", 
                    templateImport.getOriginalFilename(), templateFile.length);
        } catch (IOException e) {
            log.error("Failed to load template from resourcePath: {}, trying absolute filePath: {}", 
                    templateImport.getResourcePath(), templateImport.getFilePath());
            
            // Try to read from absolute file path as fallback
            try {
                java.nio.file.Path absolutePath = java.nio.file.Paths.get(templateImport.getFilePath());
                if (java.nio.file.Files.exists(absolutePath)) {
                    templateFile = java.nio.file.Files.readAllBytes(absolutePath);
                    log.info("Loaded template file from absolute path: {} ({} bytes)", 
                            absolutePath, templateFile.length);
                } else {
                    throw new RuntimeException("Template file not found at both resourcePath and filePath: " 
                            + templateImport.getResourcePath() + " and " + templateImport.getFilePath());
                }
            } catch (IOException ex) {
                throw new RuntimeException("Failed to load template file: " + e.getMessage() 
                        + ", absolute path also failed: " + ex.getMessage(), ex);
            }
        }

        // 3. Query data with filters (using the ReportTemplate to get configuration)
        ReportTemplate template = templateImport.getTemplate();
        ReportData reportData = reportTemplateService.buildReportData(template, request);
        log.info("Queried data: {} rows", reportData.getRows().size());

        // 4. Prepare data model for template
        Map<String, Object> dataModel = prepareDataModelForTemplate(reportData);
        
        // Log detailed data model info for debugging
        log.info("Data model prepared - rows count: {}", 
                dataModel.get("rows") instanceof List ? ((List<?>) dataModel.get("rows")).size() : "NOT A LIST");
        log.info("Data model keys: {}", dataModel.keySet());
        
        // Log first row sample if available
        if (dataModel.get("rows") instanceof List) {
            List<?> rowsList = (List<?>) dataModel.get("rows");
            if (!rowsList.isEmpty()) {
                log.info("First row sample: {}", rowsList.get(0));
            } else {
                log.warn("Rows list is EMPTY - no data to export!");
            }
        }

        // 5. Fill data into template - NEW APPROACH: Direct table manipulation
        // Instead of relying on POI-TL loop tags, we directly insert rows into the table
        log.info("Using direct table insertion method...");
        
        try {
            byte[] result = renderTemplateWithDirectTableInsertion(templateFile, dataModel);
            log.info("Report exported successfully with direct table insertion: {} bytes", result.length);
            return result;
        } catch (Exception e) {
            log.error("Direct table insertion failed: {}", e.getMessage(), e);
            throw new RuntimeException("Template rendering failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Render template with DIRECT table row insertion
     * This method finds the table in Word, then directly inserts data rows
     * Template only needs: Header row + one template row (will be copied and removed)
     */
    private byte[] renderTemplateWithDirectTableInsertion(byte[] templateFile, Map<String, Object> dataModel) throws Exception {
        log.info("Starting direct table insertion method...");
        
        try (ByteArrayInputStream templateStream = new ByteArrayInputStream(templateFile);
             org.apache.poi.xwpf.usermodel.XWPFDocument document = 
                     new org.apache.poi.xwpf.usermodel.XWPFDocument(templateStream)) {
            
            // First: Replace simple placeholders (non-table fields)
            replaceSimplePlaceholders(document, dataModel);
            
            // Second: Insert data rows into table
            List<?> rowsList = dataModel.get("rows") instanceof List ? (List<?>) dataModel.get("rows") : null;
            if (rowsList != null && !rowsList.isEmpty()) {
                insertDataIntoTable(document, rowsList);
            } else {
                log.warn("No rows data to insert into table");
            }
            
            // Write output
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.write(outputStream);
            
            return outputStream.toByteArray();
        }
    }
    
    /**
     * Replace simple placeholders in document (headers, footers, non-table fields)
     */
    private void replaceSimplePlaceholders(org.apache.poi.xwpf.usermodel.XWPFDocument document, 
                                           Map<String, Object> dataModel) {
        // Replace in paragraphs
        for (org.apache.poi.xwpf.usermodel.XWPFParagraph paragraph : document.getParagraphs()) {
            replacePlaceholdersInParagraph(paragraph, dataModel);
        }
        
        // Replace in tables (but not data rows - those are handled separately)
        for (org.apache.poi.xwpf.usermodel.XWPFTable table : document.getTables()) {
            for (org.apache.poi.xwpf.usermodel.XWPFTableRow row : table.getRows()) {
                for (org.apache.poi.xwpf.usermodel.XWPFTableCell cell : row.getTableCells()) {
                    for (org.apache.poi.xwpf.usermodel.XWPFParagraph paragraph : cell.getParagraphs()) {
                        replacePlaceholdersInParagraph(paragraph, dataModel);
                    }
                }
            }
        }
    }
    
    /**
     * Replace placeholders in a single paragraph
     */
    private void replacePlaceholdersInParagraph(org.apache.poi.xwpf.usermodel.XWPFParagraph paragraph, 
                                                 Map<String, Object> dataModel) {
        String text = paragraph.getText();
        if (text == null || !text.contains("{{")) return;
        
        // Replace placeholders
        for (Map.Entry<String, Object> entry : dataModel.entrySet()) {
            String key = entry.getKey();
            if ("rows".equals(key)) continue; // Skip rows - handled separately
            
            String placeholder = "{{" + key + "}}";
            if (text.contains(placeholder)) {
                Object value = entry.getValue();
                String replacement = value != null ? value.toString() : "";
                text = text.replace(placeholder, replacement);
            }
        }
        
        // Handle nested placeholders (e.g., {{summary.totalRecords}})
        if (text.contains("{{summary.")) {
            Object summaryObj = dataModel.get("summary");
            if (summaryObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> summary = (Map<String, Object>) summaryObj;
                for (Map.Entry<String, Object> entry : summary.entrySet()) {
                    String placeholder = "{{summary." + entry.getKey() + "}}";
                    if (text.contains(placeholder)) {
                        String replacement = entry.getValue() != null ? entry.getValue().toString() : "";
                        text = text.replace(placeholder, replacement);
                    }
                }
            }
        }
        
        // Clear and rewrite paragraph
        if (!text.equals(paragraph.getText())) {
            while (paragraph.getRuns().size() > 0) {
                paragraph.removeRun(0);
            }
            org.apache.poi.xwpf.usermodel.XWPFRun run = paragraph.createRun();
            run.setText(text);
        }
    }
    
    /**
     * Insert data rows into Word table
     * Assumes: Row 0 = Header, Row 1 = Template row (to be copied and removed)
     * NEW: Automatically detects placeholders in template row to map data correctly
     */
    private void insertDataIntoTable(org.apache.poi.xwpf.usermodel.XWPFDocument document, List<?> rowsData) {
        log.info("Inserting {} rows into table", rowsData.size());
        
        // Find the first table in document
        List<org.apache.poi.xwpf.usermodel.XWPFTable> tables = document.getTables();
        if (tables.isEmpty()) {
            log.warn("No tables found in document");
            return;
        }
        
        org.apache.poi.xwpf.usermodel.XWPFTable table = tables.get(0);
        log.info("Found table with {} rows", table.getNumberOfRows());
        
        // Need at least 2 rows: header + template
        if (table.getNumberOfRows() < 2) {
            log.error("Table must have at least 2 rows (header + template row)");
            return;
        }
        
        org.apache.poi.xwpf.usermodel.XWPFTableRow templateRow = table.getRow(1);
        int numColumns = templateRow.getTableCells().size();
        log.info("Template row has {} columns", numColumns);
        
        // Detect placeholders in template row to determine column mapping
        String[] columnKeys = detectColumnKeysFromTemplate(templateRow);
        log.info("Detected column mapping: {}", String.join(", ", columnKeys));
        
        // Insert data rows
        for (int i = 0; i < rowsData.size(); i++) {
            Object rowObj = rowsData.get(i);
            if (!(rowObj instanceof Map)) continue;
            
            @SuppressWarnings("unchecked")
            Map<String, Object> rowData = (Map<String, Object>) rowObj;
            
            // Insert new row at position (1 + i)
            org.apache.poi.xwpf.usermodel.XWPFTableRow newRow = table.insertNewTableRow(1 + i);
            
            // Create cells matching template
            for (int j = 0; j < numColumns; j++) {
                newRow.createCell();
            }
            
            // Fill cells with data using detected column mapping
            fillRowWithData(newRow, rowData, columnKeys);
        }
        
        // Remove template row (now at position 1 + rowsData.size())
        table.removeRow(1 + rowsData.size());
        log.info("Successfully inserted {} data rows and removed template row", rowsData.size());
    }
    
    /**
     * Detect column keys from placeholders in template row
     * Reads text from each cell and extracts {{key}} placeholders
     */
    private String[] detectColumnKeysFromTemplate(org.apache.poi.xwpf.usermodel.XWPFTableRow templateRow) {
        List<org.apache.poi.xwpf.usermodel.XWPFTableCell> cells = templateRow.getTableCells();
        String[] keys = new String[cells.size()];
        
        for (int i = 0; i < cells.size(); i++) {
            String cellText = getCellText(cells.get(i));
            
            // Extract placeholder: {{key}}
            String key = extractPlaceholder(cellText);
            
            if (key != null) {
                keys[i] = key;
                log.debug("Column {} → key: {}", i, key);
            } else {
                // No placeholder found - use default mapping
                String[] defaultKeys = {"rowNumber", "scaleCode", "scaleName", "location", 
                                       "data1Total", "data2Total", "data3Total", "data4Total", "data5Total", 
                                       "recordCount", "period", "lastTime"};
                keys[i] = i < defaultKeys.length ? defaultKeys[i] : "unknown_" + i;
                log.debug("Column {} → default key: {} (no placeholder found)", i, keys[i]);
            }
        }
        
        return keys;
    }
    
    /**
     * Get all text content from a cell
     */
    private String getCellText(org.apache.poi.xwpf.usermodel.XWPFTableCell cell) {
        StringBuilder text = new StringBuilder();
        for (org.apache.poi.xwpf.usermodel.XWPFParagraph paragraph : cell.getParagraphs()) {
            text.append(paragraph.getText());
        }
        return text.toString();
    }
    
    /**
     * Extract placeholder key from text like "{{key}}" or "Some text {{key}} more"
     * Returns the first placeholder found
     */
    private String extractPlaceholder(String text) {
        if (text == null || !text.contains("{{")) {
            return null;
        }
        
        int start = text.indexOf("{{");
        int end = text.indexOf("}}", start);
        
        if (start != -1 && end != -1 && end > start + 2) {
            return text.substring(start + 2, end).trim();
        }
        
        return null;
    }
    
    /**
     * Fill row cells with data using detected column mapping
     */
    private void fillRowWithData(org.apache.poi.xwpf.usermodel.XWPFTableRow row, 
                                  Map<String, Object> rowData, 
                                  String[] columnKeys) {
        List<org.apache.poi.xwpf.usermodel.XWPFTableCell> cells = row.getTableCells();
        
        for (int i = 0; i < cells.size() && i < columnKeys.length; i++) {
            org.apache.poi.xwpf.usermodel.XWPFTableCell cell = cells.get(i);
            String key = columnKeys[i];
            Object value = rowData.get(key);
            String text = value != null ? value.toString() : "";
            
            // Set cell text
            if (cell.getParagraphs().isEmpty()) {
                cell.addParagraph();
            }
            org.apache.poi.xwpf.usermodel.XWPFParagraph paragraph = cell.getParagraphs().get(0);
            
            // Clear existing content
            while (paragraph.getRuns().size() > 0) {
                paragraph.removeRun(0);
            }
            
            // Add new text
            org.apache.poi.xwpf.usermodel.XWPFRun run = paragraph.createRun();
            run.setText(text);
            
            log.trace("Cell[{}]: {} = {}", i, key, text);
        }
    }

    /**
     * Prepare data model for POI-TL template rendering
     */
    private Map<String, Object> prepareDataModelForTemplate(ReportData reportData) {
        Map<String, Object> model = new HashMap<>();

        // Organization information
        org.facenet.entity.report.OrganizationSettings org = reportTemplateService.getOrganizationSettings();
        if (org != null) {
            model.put("organizationName", org.getCompanyName() != null ? org.getCompanyName() : "");
            model.put("companyNameEn", org.getCompanyNameEn() != null ? org.getCompanyNameEn() : "");
            model.put("address", org.getAddress() != null ? org.getAddress() : "");
            model.put("phone", org.getPhone() != null ? org.getPhone() : "");
            model.put("email", org.getEmail() != null ? org.getEmail() : "");
            model.put("taxCode", org.getTaxCode() != null ? org.getTaxCode() : "");
        }

        // Header information
        model.put("reportTitle", reportData.getReportTitle());
        model.put("reportCode", reportData.getReportCode());
        model.put("exportTime", DATE_TIME_FORMATTER.format(reportData.getExportTime()));
        model.put("startTime", DATE_FORMATTER.format(reportData.getStartTime()));
        model.put("endTime", DATE_FORMATTER.format(reportData.getEndTime()));
        model.put("preparedBy", reportData.getPreparedBy());
        model.put("currentDateTime", DATE_TIME_FORMATTER.format(java.time.OffsetDateTime.now()));

        // Scale information
        String scaleNames = reportData.getRows().stream()
                .map(ReportData.ReportRow::getScaleName)
                .filter(name -> name != null && !name.isEmpty())
                .distinct()
                .collect(Collectors.joining(", "));
        model.put("scaleNames", scaleNames.isEmpty() ? "Tất cả" : scaleNames);

        // Column names from scale configs
        model.put("data1Name", reportData.getData1Name() != null ? reportData.getData1Name() : "Data 1");
        model.put("data2Name", reportData.getData2Name() != null ? reportData.getData2Name() : "Data 2");
        model.put("data3Name", reportData.getData3Name() != null ? reportData.getData3Name() : "Data 3");
        model.put("data4Name", reportData.getData4Name() != null ? reportData.getData4Name() : "Data 4");
        model.put("data5Name", reportData.getData5Name() != null ? reportData.getData5Name() : "Data 5");

        // Data rows for table
        List<Map<String, Object>> rows = reportData.getRows().stream()
                .map(this::convertRowToMap)
                .collect(Collectors.toList());
        model.put("rows", rows);

        // Summary statistics (nested object for template)
        Map<String, Object> summary = new HashMap<>();
        if (reportData.getSummary() != null) {
            summary.put("totalScales", reportData.getSummary().getTotalScales());
            summary.put("totalRecords", reportData.getSummary().getTotalRecords());
            summary.put("data1GrandTotal", NUMBER_FORMAT.format(reportData.getSummary().getData1GrandTotal() != null ? reportData.getSummary().getData1GrandTotal() : 0));
            summary.put("data2GrandTotal", NUMBER_FORMAT.format(reportData.getSummary().getData2GrandTotal() != null ? reportData.getSummary().getData2GrandTotal() : 0));
            summary.put("data3GrandTotal", NUMBER_FORMAT.format(reportData.getSummary().getData3GrandTotal() != null ? reportData.getSummary().getData3GrandTotal() : 0));
            summary.put("data4GrandTotal", NUMBER_FORMAT.format(reportData.getSummary().getData4GrandTotal() != null ? reportData.getSummary().getData4GrandTotal() : 0));
            summary.put("data5GrandTotal", NUMBER_FORMAT.format(reportData.getSummary().getData5GrandTotal() != null ? reportData.getSummary().getData5GrandTotal() : 0));
            summary.put("data1Average", NUMBER_FORMAT.format(reportData.getSummary().getData1Average() != null ? reportData.getSummary().getData1Average() : 0));
            summary.put("data2Average", NUMBER_FORMAT.format(reportData.getSummary().getData2Average() != null ? reportData.getSummary().getData2Average() : 0));
            summary.put("data3Average", NUMBER_FORMAT.format(reportData.getSummary().getData3Average() != null ? reportData.getSummary().getData3Average() : 0));
            summary.put("data4Average", NUMBER_FORMAT.format(reportData.getSummary().getData4Average() != null ? reportData.getSummary().getData4Average() : 0));
            summary.put("data5Average", NUMBER_FORMAT.format(reportData.getSummary().getData5Average() != null ? reportData.getSummary().getData5Average() : 0));
            summary.put("data1Max", NUMBER_FORMAT.format(reportData.getSummary().getData1Max() != null ? reportData.getSummary().getData1Max() : 0));
            summary.put("data2Max", NUMBER_FORMAT.format(reportData.getSummary().getData2Max() != null ? reportData.getSummary().getData2Max() : 0));
            summary.put("data3Max", NUMBER_FORMAT.format(reportData.getSummary().getData3Max() != null ? reportData.getSummary().getData3Max() : 0));
            summary.put("data4Max", NUMBER_FORMAT.format(reportData.getSummary().getData4Max() != null ? reportData.getSummary().getData4Max() : 0));
            summary.put("data5Max", NUMBER_FORMAT.format(reportData.getSummary().getData5Max() != null ? reportData.getSummary().getData5Max() : 0));
            
            log.debug("Prepared data model: {} rows, {} total records", 
                    rows.size(), reportData.getSummary().getTotalRecords());
        } else {
            summary.put("totalScales", 0);
            summary.put("totalRecords", rows.size());
            summary.put("data1GrandTotal", "0");
            log.debug("Prepared data model: {} rows (no summary)", rows.size());
        }
        model.put("summary", summary);
        
        // Additional metadata
        model.put("totalLogs", rows.size());
        model.put("watermark", "Generated by ScaleHub IoT");
        model.put("aggregationMethodLabel", reportData.getMetadata() != null && reportData.getMetadata().containsKey("aggregationMethod") 
                ? reportData.getMetadata().get("aggregationMethod").toString() : "Standard");

        return model;
    }

    /**
     * Convert ReportRow to Map for template rendering
     */
    private Map<String, Object> convertRowToMap(ReportData.ReportRow row) {
        Map<String, Object> map = new HashMap<>();
        
        // Row number
        map.put("rowNumber", row.getRowNumber());
        
        // Scale information
        map.put("scaleId", row.getScaleId());
        map.put("scaleCode", row.getScaleCode());
        map.put("scaleName", row.getScaleName());
        map.put("location", row.getLocation());
        
        // Period (for time-based grouping)
        map.put("period", row.getPeriod());
        
        // Data totals
        map.put("data1Total", row.getData1Total() != null ? NUMBER_FORMAT.format(row.getData1Total()) : "0");
        map.put("data2Total", row.getData2Total() != null ? NUMBER_FORMAT.format(row.getData2Total()) : "0");
        map.put("data3Total", row.getData3Total() != null ? NUMBER_FORMAT.format(row.getData3Total()) : "0");
        map.put("data4Total", row.getData4Total() != null ? NUMBER_FORMAT.format(row.getData4Total()) : "0");
        map.put("data5Total", row.getData5Total() != null ? NUMBER_FORMAT.format(row.getData5Total()) : "0");
        
        // Record count
        map.put("recordCount", row.getRecordCount());
        
        // Last time
        if (row.getLastTime() != null) {
            map.put("lastTime", DATE_TIME_FORMATTER.format(row.getLastTime()));
        }

        return map;
    }

    /**
     * Export report based on request (with optional template)
     */
    public byte[] exportReport(ReportExportRequest request, Long templateId) throws IOException, DocumentException {
        log.info("Exporting report: type={}, templateId={}, startTime={}, endTime={}", 
                request.getType(), templateId, request.getStartTime(), request.getEndTime());

        // Get template
        ReportTemplate template;
        if (templateId != null) {
            template = reportTemplateService.getTemplate(templateId);
        } else {
            // Use default template for export type
            template = reportTemplateService.getDefaultTemplate(
                    switch (request.getType()) {
                        case EXCEL -> ReportTemplate.ReportType.EXCEL;
                        case WORD -> ReportTemplate.ReportType.WORD;
                        case PDF -> ReportTemplate.ReportType.PDF;
                    }
            );
        }

        // Fetch and process data using template
        ReportData reportData = reportTemplateService.buildReportData(
                template,
                request
        );

        // Export based on type
        return switch (request.getType()) {
            case EXCEL -> excelExportService.exportToExcel(reportData, template);
            case WORD -> wordExportService.exportToWord(reportData);
            case PDF -> pdfExportService.exportToPdf(reportData);
        };
    }
    
    /**
     * Backward compatibility method (uses default template)
     */
    public byte[] exportReport(ReportExportRequest request) throws IOException, DocumentException {
        return exportReport(request, null);
    }

    /**
     * Generate filename for exported report
     */
    public String generateFilename(ReportExportRequest request) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String dateStr = formatter.format(request.getStartTime());
        
        String code = request.getReportCode() != null ? request.getReportCode() : "BCSL";
        String scaleInfo = request.getScaleIds() != null && !request.getScaleIds().isEmpty() 
                ? "SCALE" + request.getScaleIds().get(0) 
                : "ALL";
        
        return String.format("%s_%s_%s%s", code, scaleInfo, dateStr, request.getType().getExtension());
    }
}
