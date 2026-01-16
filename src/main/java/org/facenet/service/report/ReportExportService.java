package org.facenet.service.report;

import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.config.Configure;
import com.deepoove.poi.plugin.table.LoopRowTableRenderPolicy;
import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.facenet.common.pagination.PageResponseDto;
import org.facenet.dto.report.IntervalReportExportRequestV2;
import org.facenet.dto.report.ReportData;
import org.facenet.dto.report.ReportExportRequest;
import org.facenet.dto.scale.IntervalReportRequestDtoV2;
import org.facenet.dto.scale.IntervalReportResponseDtoV2;
import org.facenet.entity.report.ReportTemplate;
import org.facenet.entity.report.TemplateImport;
import org.facenet.repository.report.TemplateImportRepository;
import org.facenet.service.scale.report.ReportService;
import org.facenet.util.TemplateFileUtil;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
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
    private final ReportService intervalReportService;

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
     * Export report using imported template (ENHANCED - supports Word, PDF, Excel)
     * Directly uses template file from template_imports table
     * Fills data from query results into the template
     * 
     * Supported formats:
     * - .docx: Word template with direct table insertion
     * - .html: PDF template using Thymeleaf
     * - .xlsx: Excel template (future implementation)
     */
    public byte[] exportReportWithImportedTemplate(ReportExportRequest request, Long importId) 
            throws IOException, DocumentException {
        log.info("Exporting report with imported template: importId={}, startTime={}, endTime={}", 
                importId, request.getStartTime(), request.getEndTime());

        // 1. Get template import record
        TemplateImport templateImport = templateImportRepository.findById(importId)
                .orElseThrow(() -> new RuntimeException("Template import not found for importId: " + importId));

        log.info("Found template import: id={}, resourcePath={}, filePath={}, filename={}", 
                templateImport.getId(), 
                templateImport.getResourcePath(), 
                templateImport.getFilePath(),
                templateImport.getOriginalFilename());

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

        // 4. Determine template type from file extension
        String filename = templateImport.getOriginalFilename().toLowerCase();
        TemplateFileType fileType = detectTemplateFileType(filename);
        log.info("Detected template file type: {} for file: {}", fileType, filename);

        // 5. Route to appropriate export method based on file type
        return switch (fileType) {
            case WORD -> exportWordTemplate(templateFile, reportData);
            case PDF_HTML -> exportPdfTemplate(templateFile, reportData);
            case EXCEL -> exportExcelTemplate(templateFile, reportData, template);
            default -> throw new RuntimeException("Unsupported template file type: " + filename);
        };
    }

    /**
     * Detect template file type from filename extension
     */
    private TemplateFileType detectTemplateFileType(String filename) {
        if (filename.endsWith(".docx")) {
            return TemplateFileType.WORD;
        } else if (filename.endsWith(".html") || filename.endsWith(".htm")) {
            return TemplateFileType.PDF_HTML;
        } else if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
            return TemplateFileType.EXCEL;
        }
        return TemplateFileType.UNKNOWN;
    }

    /**
     * Export using Word template (.docx)
     */
    private byte[] exportWordTemplate(byte[] templateFile, ReportData reportData) throws IOException {
        log.info("Exporting Word template with direct table insertion");
        
        // Prepare data model for template
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

        try {
            byte[] result = renderTemplateWithDirectTableInsertion(templateFile, dataModel);
            log.info("Word template exported successfully: {} bytes", result.length);
            return result;
        } catch (Exception e) {
            log.error("Word template export failed: {}", e.getMessage(), e);
            throw new RuntimeException("Word template rendering failed: " + e.getMessage(), e);
        }
    }

    /**
     * Export using PDF HTML template (.html)
     * Uses Thymeleaf to process HTML template, then converts to PDF
     * Supports Vietnamese characters with embedded fonts
     */
    private byte[] exportPdfTemplate(byte[] templateFile, ReportData reportData) throws IOException, DocumentException {
        log.info("Exporting PDF from HTML template with Vietnamese font support");
        
        try {
            // Convert template bytes to string with UTF-8 encoding
            String htmlTemplate = new String(templateFile, java.nio.charset.StandardCharsets.UTF_8);
            
            // Process HTML template with Thymeleaf
            org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
            context.setVariable("report", reportData);
            context.setVariable("dateTimeFormatter", DATE_TIME_FORMATTER);
            
            // Prepare data model
            Map<String, Object> dataModel = prepareDataModelForTemplate(reportData);
            
            // Convert logo to Base64 for HTML img tag if available
            if (dataModel.containsKey("logoData") && dataModel.get("logoData") != null) {
                byte[] logoBytes = (byte[]) dataModel.get("logoData");
                String logoBase64 = java.util.Base64.getEncoder().encodeToString(logoBytes);
                dataModel.put("logoBase64", "data:image/png;base64," + logoBase64);
                log.debug("Logo converted to Base64 for HTML template: {} bytes", logoBytes.length);
            }
            
            dataModel.forEach(context::setVariable);
            
            // Create a temporary Thymeleaf engine for inline template processing
            org.thymeleaf.templateresolver.StringTemplateResolver resolver = 
                    new org.thymeleaf.templateresolver.StringTemplateResolver();
            resolver.setTemplateMode(org.thymeleaf.templatemode.TemplateMode.HTML);
            
            org.thymeleaf.spring6.SpringTemplateEngine engine = new org.thymeleaf.spring6.SpringTemplateEngine();
            engine.setTemplateResolver(resolver);
            
            // Process template
            String processedHtml = engine.process(htmlTemplate, context);
            
            // Ensure HTML has proper meta charset declaration
            if (!processedHtml.contains("charset") && !processedHtml.contains("UTF-8")) {
                processedHtml = processedHtml.replaceFirst("<head>", 
                    "<head><meta charset=\"UTF-8\"/>");
            }
            
            // Convert HTML to PDF using Flying Saucer with Vietnamese font support
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            org.xhtmlrenderer.pdf.ITextRenderer renderer = new org.xhtmlrenderer.pdf.ITextRenderer();
            
            try {
                // Set document with proper encoding
                renderer.setDocumentFromString(processedHtml);
                
                // Get font resolver and add system fonts for Vietnamese support
                org.xhtmlrenderer.pdf.ITextFontResolver fontResolver = renderer.getFontResolver();
                
                // Try to add common Vietnamese fonts from system
                try {
                    // Windows fonts
                    addFontIfExists(fontResolver, "C:/Windows/Fonts/arial.ttf", "Arial");
                    addFontIfExists(fontResolver, "C:/Windows/Fonts/times.ttf", "Times New Roman");
                    addFontIfExists(fontResolver, "C:/Windows/Fonts/calibri.ttf", "Calibri");
                    
                    // Linux fonts
                    addFontIfExists(fontResolver, "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", "DejaVu Sans");
                    addFontIfExists(fontResolver, "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf", "Liberation Sans");
                    
                    log.info("Vietnamese fonts loaded successfully");
                } catch (Exception fontEx) {
                    log.warn("Could not load some system fonts, Vietnamese characters may not display correctly: {}", 
                            fontEx.getMessage());
                }
                
                renderer.layout();
                renderer.createPDF(outputStream);
            } finally {
                renderer.finishPDF();
            }
            
            log.info("PDF template exported successfully: {} bytes", outputStream.size());
            return outputStream.toByteArray();
            
        } catch (Exception e) {
            log.error("PDF template export failed: {}", e.getMessage(), e);
            throw new RuntimeException("PDF template rendering failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Helper method to add font if file exists
     */
    private void addFontIfExists(org.xhtmlrenderer.pdf.ITextFontResolver fontResolver, 
                                  String fontPath, 
                                  String fontName) {
        try {
            java.io.File fontFile = new java.io.File(fontPath);
            if (fontFile.exists()) {
                fontResolver.addFont(fontPath, 
                                    com.lowagie.text.pdf.BaseFont.IDENTITY_H, 
                                    com.lowagie.text.pdf.BaseFont.EMBEDDED);
                log.debug("Added font: {} from {}", fontName, fontPath);
            }
        } catch (Exception e) {
            log.trace("Could not add font {} from {}: {}", fontName, fontPath, e.getMessage());
        }
    }

    /**
     * Load organization logo from resources or database
     * Priority: 1) Database logoData, 2) Resources path from logoUrl, 3) Default logo
     */
    private byte[] loadOrganizationLogo(org.facenet.entity.report.OrganizationSettings org) {
        if (org == null) {
            return loadDefaultLogo();
        }
        
        // Priority 1: Logo data stored in database
        if (org.getLogoData() != null && org.getLogoData().length > 0) {
            log.debug("Loading logo from database: {} bytes", org.getLogoData().length);
            return org.getLogoData();
        }
        
        // Priority 2: Logo path in resources
        if (org.getLogoUrl() != null && !org.getLogoUrl().isEmpty()) {
            try {
                // Try to load from classpath resources
                org.springframework.core.io.Resource resource = 
                    new org.springframework.core.io.ClassPathResource(org.getLogoUrl());
                if (resource.exists()) {
                    byte[] logoBytes = resource.getInputStream().readAllBytes();
                    log.debug("Loading logo from resources: {} -> {} bytes", org.getLogoUrl(), logoBytes.length);
                    return logoBytes;
                } else {
                    log.warn("Logo file not found in resources: {}", org.getLogoUrl());
                }
            } catch (Exception e) {
                log.error("Failed to load logo from resources: {}", org.getLogoUrl(), e);
            }
        }
        
        // Priority 3: Default logo
        return loadDefaultLogo();
    }

    /**
     * Load default logo from resources
     */
    private byte[] loadDefaultLogo() {
        try {
            org.springframework.core.io.Resource resource = 
                new org.springframework.core.io.ClassPathResource("images/default-logo.png");
            if (resource.exists()) {
                byte[] logoBytes = resource.getInputStream().readAllBytes();
                log.debug("Loading default logo: {} bytes", logoBytes.length);
                return logoBytes;
            }
        } catch (Exception e) {
            log.trace("No default logo available: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Export using Excel template (.xlsx)
     * Reads template, fills data into predefined table structure
     */
    private byte[] exportExcelTemplate(byte[] templateFile, ReportData reportData, ReportTemplate template) 
            throws IOException {
        log.info("Exporting Excel template with data insertion");
        
        try (ByteArrayInputStream templateStream = new ByteArrayInputStream(templateFile);
             org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = 
                     new org.apache.poi.xssf.usermodel.XSSFWorkbook(templateStream)) {
            
            // Get first sheet (assuming template uses first sheet)
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            
            // Prepare data model
            Map<String, Object> dataModel = prepareDataModelForTemplate(reportData);
            log.info("Data model prepared with keys: {}", dataModel.keySet());
            log.info("Sample values - organizationName: {}", dataModel.get("organizationName"));
            log.info("Sample values - reportTitle: {}", dataModel.get("reportTitle"));
            log.info("Sample values - startTime: {}", dataModel.get("startTime"));
            log.info("Sample values - data1Name: {}", dataModel.get("data1Name"));
            
            // Insert data rows into table FIRST (before replacing placeholders)
            // This is important because shiftRows can destroy placeholder replacements
            List<?> rowsList = dataModel.get("rows") instanceof List ? (List<?>) dataModel.get("rows") : null;
            if (rowsList != null && !rowsList.isEmpty()) {
                insertDataIntoExcelTable(sheet, rowsList);
            } else {
                log.warn("No rows data to insert into Excel table");
            }
            
            // Replace simple placeholders AFTER data insertion
            // This ensures placeholders are replaced in the final structure
            replaceExcelPlaceholders(sheet, dataModel);
            
            // Write output - try-with-resources will auto-close workbook
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            
            byte[] result = outputStream.toByteArray();
            log.info("Excel template exported successfully: {} bytes", result.length);
            return result;
            
        } catch (Exception e) {
            log.error("Excel template export failed: {}", e.getMessage(), e);
            throw new RuntimeException("Excel template rendering failed: " + e.getMessage(), e);
        }
    }

    /**
     * Replace placeholders in Excel template (SIMPLIFIED - no merged cells, no formulas)
     */
    private void replaceExcelPlaceholders(org.apache.poi.ss.usermodel.Sheet sheet, 
                                          Map<String, Object> dataModel) {
        log.info("Replacing Excel placeholders in {} rows", sheet.getLastRowNum() + 1);
        log.info("Available data model keys: {}", dataModel.keySet());
        
        int replacedCount = 0;
        
        // Iterate through all rows
        for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            org.apache.poi.ss.usermodel.Row row = sheet.getRow(rowIndex);
            if (row == null) continue;
            
            // Iterate through all cells in the row
            int lastCellNum = row.getLastCellNum();
            if (lastCellNum == -1) continue;
            
            for (int cellIndex = 0; cellIndex < lastCellNum; cellIndex++) {
                org.apache.poi.ss.usermodel.Cell cell = row.getCell(cellIndex);
                if (cell == null) continue;
                
                // Only process STRING cells
                if (cell.getCellType() != org.apache.poi.ss.usermodel.CellType.STRING) {
                    continue;
                }
                
                String cellValue = cell.getStringCellValue();
                if (cellValue == null || !cellValue.contains("{{")) {
                    continue;
                }
                
                log.info("Processing cell [{}][{}]: '{}'", rowIndex, cellIndex, cellValue);
                String originalValue = cellValue;
                
                // Replace all placeholders in cell
                for (Map.Entry<String, Object> entry : dataModel.entrySet()) {
                    String key = entry.getKey();
                    if ("rows".equals(key)) continue; // Skip rows - handled separately
                    
                    String placeholder = "{{" + key + "}}";
                    if (cellValue.contains(placeholder)) {
                        Object value = entry.getValue();
                        String replacement = value != null ? value.toString() : "";
                        cellValue = cellValue.replace(placeholder, replacement);
                        log.info("  ✓ Replaced '{}' with: '{}'", placeholder, replacement);
                        replacedCount++;
                    }
                }
                
                // Handle nested placeholders (e.g., {{summary.totalRecords}})
                if (cellValue.contains("{{summary.")) {
                    Object summaryObj = dataModel.get("summary");
                    if (summaryObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> summary = (Map<String, Object>) summaryObj;
                        for (Map.Entry<String, Object> entry : summary.entrySet()) {
                            String placeholder = "{{summary." + entry.getKey() + "}}";
                            if (cellValue.contains(placeholder)) {
                                String replacement = entry.getValue() != null ? entry.getValue().toString() : "";
                                cellValue = cellValue.replace(placeholder, replacement);
                                log.info("  ✓ Replaced '{}' with: '{}'", placeholder, replacement);
                                replacedCount++;
                            }
                        }
                    }
                }
                
                // Update cell if value changed
                if (!cellValue.equals(originalValue)) {
                    cell.setCellValue(cellValue);
                    log.info("  → Updated cell [{}][{}]: '{}' → '{}'", rowIndex, cellIndex, originalValue, cellValue);
                }
                
                // Warn if still contains placeholder
                if (cellValue.contains("{{")) {
                    log.warn("  ⚠ Cell [{}][{}] still contains unreplaced placeholders: '{}'", rowIndex, cellIndex, cellValue);
                }
            }
        }
        
        log.info("Excel placeholders replacement completed: {} replacements made", replacedCount);
    }

    /**
     * Insert data rows into Excel table
     * Assumes: Row with {{rowNumber}} placeholder is the template row
     */
    private void insertDataIntoExcelTable(org.apache.poi.ss.usermodel.Sheet sheet, List<?> rowsData) {
        log.info("Inserting {} rows into Excel table", rowsData.size());
        
        // Find template row (contains {{rowNumber}} or other row placeholders)
        int templateRowIndex = -1;
        org.apache.poi.ss.usermodel.Row templateRow = null;
        
        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            org.apache.poi.ss.usermodel.Row row = sheet.getRow(i);
            if (row == null) continue;
            
            for (org.apache.poi.ss.usermodel.Cell cell : row) {
                if (cell != null && cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                    String cellValue = cell.getStringCellValue();
                    if (cellValue != null && (cellValue.contains("{{rowNumber}}") || 
                                             cellValue.contains("{{scaleName}}") ||
                                             cellValue.contains("{{data1Total}}"))) {
                        templateRowIndex = i;
                        templateRow = row;
                        break;
                    }
                }
            }
            if (templateRowIndex != -1) break;
        }
        
        if (templateRowIndex == -1) {
            log.warn("No template row found in Excel (looking for {{rowNumber}} or similar placeholders)");
            return;
        }
        
        log.info("Found template row at index: {}", templateRowIndex);
        
        // Detect column mapping from template row
        String[] columnKeys = detectExcelColumnKeys(templateRow);
        log.info("Detected Excel column mapping: {}", String.join(", ", columnKeys));
        
        // Insert data rows AFTER template row
        for (int i = 0; i < rowsData.size(); i++) {
            Object rowObj = rowsData.get(i);
            if (!(rowObj instanceof Map)) continue;
            
            @SuppressWarnings("unchecked")
            Map<String, Object> rowData = (Map<String, Object>) rowObj;
            
            // Shift existing rows down to make space (if there are rows below)
            int insertPosition = templateRowIndex + 1 + i;
            if (insertPosition <= sheet.getLastRowNum()) {
                sheet.shiftRows(insertPosition, sheet.getLastRowNum(), 1, true, false);
            }
            
            // Create new row at insert position
            org.apache.poi.ss.usermodel.Row newRow = sheet.createRow(insertPosition);
            
            // Copy style from template and fill data
            fillExcelRowWithData(newRow, templateRow, rowData, columnKeys);
            
            log.debug("Inserted data row {} at position {}", i + 1, insertPosition);
        }
        
        // Remove template row (now shifted down by rowsData.size() positions)
        int templateRowNewIndex = templateRowIndex;
        org.apache.poi.ss.usermodel.Row rowToRemove = sheet.getRow(templateRowNewIndex);
        if (rowToRemove != null) {
            sheet.removeRow(rowToRemove);
            // Shift remaining rows up to fill the gap
            if (templateRowNewIndex < sheet.getLastRowNum()) {
                sheet.shiftRows(templateRowNewIndex + 1, sheet.getLastRowNum(), -1);
            }
            log.info("Removed template row at index {}", templateRowNewIndex);
        }
        
        log.info("Successfully inserted {} data rows into Excel and removed template", rowsData.size());
    }

    /**
     * Detect column keys from Excel template row
     */
    private String[] detectExcelColumnKeys(org.apache.poi.ss.usermodel.Row templateRow) {
        int lastCellNum = templateRow.getLastCellNum();
        String[] keys = new String[lastCellNum];
        
        for (int i = 0; i < lastCellNum; i++) {
            org.apache.poi.ss.usermodel.Cell cell = templateRow.getCell(i);
            if (cell != null && cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                String cellValue = cell.getStringCellValue();
                String key = extractPlaceholder(cellValue);
                
                if (key != null) {
                    keys[i] = key;
                } else {
                    keys[i] = "unknown_" + i;
                }
            } else {
                keys[i] = "unknown_" + i;
            }
        }
        
        return keys;
    }

    /**
     * Fill Excel row with data using column mapping
     */
    private void fillExcelRowWithData(org.apache.poi.ss.usermodel.Row newRow,
                                      org.apache.poi.ss.usermodel.Row templateRow,
                                      Map<String, Object> rowData,
                                      String[] columnKeys) {
        for (int i = 0; i < columnKeys.length; i++) {
            org.apache.poi.ss.usermodel.Cell newCell = newRow.createCell(i);
            
            // Copy style from template
            org.apache.poi.ss.usermodel.Cell templateCell = templateRow.getCell(i);
            if (templateCell != null && templateCell.getCellStyle() != null) {
                newCell.setCellStyle(templateCell.getCellStyle());
            }
            
            // Fill data
            String key = columnKeys[i];
            Object value = rowData.get(key);
            
            if (value != null) {
                if (value instanceof Number) {
                    newCell.setCellValue(((Number) value).doubleValue());
                } else {
                    newCell.setCellValue(value.toString());
                }
            } else {
                newCell.setCellValue("");
            }
        }
    }

    /**
     * Template file type enumeration
     */
    private enum TemplateFileType {
        WORD,      // .docx
        PDF_HTML,  // .html for PDF generation
        EXCEL,     // .xlsx
        UNKNOWN
    }
    
    /**
     * Render template with DIRECT table row insertion
     * This method finds the table in Word, then directly inserts data rows
     * Template only needs: Header row + one template row (will be copied and removed)
     */
    private byte[] renderTemplateWithDirectTableInsertion(byte[] templateFile, Map<String, Object> dataModel) throws IOException {
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
        if (text == null || (!text.contains("{{") && !text.contains("[[LOGO]]"))) return;
        
        // Handle logo placeholder
        if (text.contains("[[LOGO]]")) {
            insertLogoIntoParagraph(paragraph, dataModel);
            return;
        }
        
        // Replace placeholders
        for (Map.Entry<String, Object> entry : dataModel.entrySet()) {
            String key = entry.getKey();
            if ("rows".equals(key) || "logoData".equals(key)) continue; // Skip rows and logoData
            
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
        
        // Handle direction summaries (e.g., {{importSummaries.totalScales}})
        text = replaceNestedPlaceholders(text, dataModel, "importSummaries");
        text = replaceNestedPlaceholders(text, dataModel, "exportSummaries");
        text = replaceNestedPlaceholders(text, dataModel, "unknownSummaries");
        
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
     * Replace nested placeholders like {{object.property}}
     */
    private String replaceNestedPlaceholders(String text, Map<String, Object> dataModel, String objectKey) {
        String prefix = "{{" + objectKey + ".";
        if (!text.contains(prefix)) {
            return text;
        }
        
        Object obj = dataModel.get(objectKey);
        if (!(obj instanceof Map)) {
            return text;
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> nestedMap = (Map<String, Object>) obj;
        
        for (Map.Entry<String, Object> entry : nestedMap.entrySet()) {
            String placeholder = "{{" + objectKey + "." + entry.getKey() + "}}";
            if (text.contains(placeholder)) {
                String replacement = entry.getValue() != null ? entry.getValue().toString() : "";
                text = text.replace(placeholder, replacement);
            }
        }
        
        return text;
    }
    
    /**
     * Insert logo into paragraph
     */
    private void insertLogoIntoParagraph(org.apache.poi.xwpf.usermodel.XWPFParagraph paragraph, 
                                         Map<String, Object> dataModel) {
        if (!dataModel.containsKey("logoData") || dataModel.get("logoData") == null) {
            log.warn("Logo placeholder found but no logo data available");
            return;
        }
        
        try {
            byte[] logoBytes = (byte[]) dataModel.get("logoData");
            
            // Clear paragraph content
            while (paragraph.getRuns().size() > 0) {
                paragraph.removeRun(0);
            }
            
            // Create run and insert picture
            org.apache.poi.xwpf.usermodel.XWPFRun run = paragraph.createRun();
            
            try (java.io.ByteArrayInputStream logoStream = new java.io.ByteArrayInputStream(logoBytes)) {
                // Insert picture with size: width 150px (approx 2 inches), height auto-scale
                run.addPicture(
                    logoStream,
                    org.apache.poi.xwpf.usermodel.Document.PICTURE_TYPE_PNG,
                    "logo.png",
                    org.apache.poi.util.Units.toEMU(150), // width in pixels
                    org.apache.poi.util.Units.toEMU(50)   // height in pixels
                );
                
                log.info("Logo inserted into Word document: {} bytes", logoBytes.length);
            }
        } catch (Exception e) {
            log.error("Failed to insert logo into Word document", e);
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
            
            // Load logo
            byte[] logoBytes = loadOrganizationLogo(org);
            if (logoBytes != null) {
                model.put("logoData", logoBytes);
                model.put("hasLogo", true);
                log.debug("Logo loaded: {} bytes", logoBytes.length);
            } else {
                model.put("hasLogo", false);
                log.debug("No logo available");
            }
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
        
        // Direction-based summaries for V2 reports
        if (reportData.getImportSummaries() != null) {
            Map<String, Object> importMap = convertDirectionSummaryToMap(reportData.getImportSummaries());
            model.put("importSummaries", importMap);
            log.info("Added importSummaries: totalScales={}, totalRecords={}", 
                    importMap.get("totalScales"), importMap.get("totalRecords"));
        } else {
            log.warn("No importSummaries available in reportData");
        }
        if (reportData.getExportSummaries() != null) {
            Map<String, Object> exportMap = convertDirectionSummaryToMap(reportData.getExportSummaries());
            model.put("exportSummaries", exportMap);
            log.info("Added exportSummaries: totalScales={}, totalRecords={}", 
                    exportMap.get("totalScales"), exportMap.get("totalRecords"));
        } else {
            log.warn("No exportSummaries available in reportData");
        }
        if (reportData.getUnknownSummaries() != null) {
            Map<String, Object> unknownMap = convertDirectionSummaryToMap(reportData.getUnknownSummaries());
            model.put("unknownSummaries", unknownMap);
            log.info("Added unknownSummaries: totalScales={}, totalRecords={}", 
                    unknownMap.get("totalScales"), unknownMap.get("totalRecords"));
        } else {
            log.warn("No unknownSummaries available in reportData");
        }
        
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
        map.put("rowNumber", row.getRowNumber() != null ? row.getRowNumber() : 0);
        
        // Scale information
        map.put("scaleId", row.getScaleId() != null ? row.getScaleId() : 0L);
        map.put("scaleCode", row.getScaleCode() != null ? row.getScaleCode() : "");
        map.put("scaleName", row.getScaleName() != null ? row.getScaleName() : "");
        map.put("location", row.getLocation() != null && !row.getLocation().isEmpty() && !"string".equalsIgnoreCase(row.getLocation()) 
                ? row.getLocation() : "");
        
        // Period (for time-based grouping)
        map.put("period", row.getPeriod() != null ? row.getPeriod() : "");
        
        // Data totals
        map.put("data1Total", row.getData1Total() != null ? NUMBER_FORMAT.format(row.getData1Total()) : "0,00");
        map.put("data2Total", row.getData2Total() != null ? NUMBER_FORMAT.format(row.getData2Total()) : "0,00");
        map.put("data3Total", row.getData3Total() != null ? NUMBER_FORMAT.format(row.getData3Total()) : "0,00");
        map.put("data4Total", row.getData4Total() != null ? NUMBER_FORMAT.format(row.getData4Total()) : "0,00");
        map.put("data5Total", row.getData5Total() != null ? NUMBER_FORMAT.format(row.getData5Total()) : "0,00");
        
        // Start values (V2 interval reports)
        map.put("data1Start", row.getData1Start() != null ? NUMBER_FORMAT.format(row.getData1Start()) : "0,00");
        map.put("data2Start", row.getData2Start() != null ? NUMBER_FORMAT.format(row.getData2Start()) : "0,00");
        map.put("data3Start", row.getData3Start() != null ? NUMBER_FORMAT.format(row.getData3Start()) : "0,00");
        map.put("data4Start", row.getData4Start() != null ? NUMBER_FORMAT.format(row.getData4Start()) : "0,00");
        map.put("data5Start", row.getData5Start() != null ? NUMBER_FORMAT.format(row.getData5Start()) : "0,00");
        
        // End values (V2 interval reports)
        map.put("data1End", row.getData1End() != null ? NUMBER_FORMAT.format(row.getData1End()) : "0,00");
        map.put("data2End", row.getData2End() != null ? NUMBER_FORMAT.format(row.getData2End()) : "0,00");
        map.put("data3End", row.getData3End() != null ? NUMBER_FORMAT.format(row.getData3End()) : "0,00");
        map.put("data4End", row.getData4End() != null ? NUMBER_FORMAT.format(row.getData4End()) : "0,00");
        map.put("data5End", row.getData5End() != null ? NUMBER_FORMAT.format(row.getData5End()) : "0,00");
        
        // Direction (V2 reports with direction tracking)
        map.put("direction", row.getDirection() != null ? row.getDirection() : 0);
        map.put("directionName", getDirectionNameFromCode(row.getDirection()));
        
        // Record count
        map.put("recordCount", row.getRecordCount() != null ? row.getRecordCount() : 0);
        
        // Last time
        map.put("lastTime", row.getLastTime() != null ? DATE_TIME_FORMATTER.format(row.getLastTime()) : "");

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
    
    /**
     * Export Interval Report V2 with template
     * Combines interval/v2 data generation with template-based export
     */
    public byte[] exportIntervalReportV2WithTemplate(IntervalReportExportRequestV2 request) 
            throws IOException, DocumentException {
        log.info("Exporting Interval Report V2: importId={}, interval={}, fromTime={}, toTime={}", 
                request.getImportId(), request.getInterval(), request.getFromTime(), request.getToTime());

        // 1. Get template import record
        TemplateImport templateImport = templateImportRepository.findById(request.getImportId())
                .orElseThrow(() -> new RuntimeException("Template import not found for importId: " + request.getImportId()));

        log.info("Found template import: id={}, filename={}", 
                templateImport.getId(), templateImport.getOriginalFilename());

        // 2. Load template file
        byte[] templateFile = loadTemplateFile(templateImport);
        
        // 3. Generate interval report V2 data (with overview)
        IntervalReportResponseDtoV2 intervalReportV2 = generateIntervalReportV2DataWithOverview(request);
        
        log.info("Generated interval data: {} rows, overview present: {}", 
                intervalReportV2.getRows() != null ? intervalReportV2.getRows().size() : 0,
                intervalReportV2.getOverview() != null);
        
        // 4. Convert interval V2 data to ReportData structure
        ReportData reportData = convertIntervalV2ToReportData(intervalReportV2, request);
        
        log.info("Converted to ReportData: {} rows", reportData.getRows().size());
        
        // 5. Determine template type and export
        String filename = templateImport.getOriginalFilename().toLowerCase();
        TemplateFileType fileType = detectTemplateFileType(filename);
        
        return switch (fileType) {
            case WORD -> exportWordTemplate(templateFile, reportData);
            case PDF_HTML -> exportPdfTemplate(templateFile, reportData);
            case EXCEL -> exportExcelTemplate(templateFile, reportData, templateImport.getTemplate());
            default -> throw new RuntimeException("Unsupported template file type: " + filename);
        };
    }
    
    /**
     * Load template file from TemplateImport record
     */
    private byte[] loadTemplateFile(TemplateImport templateImport) throws IOException {
        try {
            byte[] templateFile = templateFileUtil.readTemplateFile(templateImport.getResourcePath());
            log.info("Loaded template file: {} ({} bytes)", 
                    templateImport.getOriginalFilename(), templateFile.length);
            return templateFile;
        } catch (IOException e) {
            log.error("Failed to load from resourcePath: {}, trying filePath: {}", 
                    templateImport.getResourcePath(), templateImport.getFilePath());
            
            java.nio.file.Path absolutePath = java.nio.file.Paths.get(templateImport.getFilePath());
            if (java.nio.file.Files.exists(absolutePath)) {
                return java.nio.file.Files.readAllBytes(absolutePath);
            }
            
            throw new RuntimeException("Template file not found: " + templateImport.getOriginalFilename());
        }
    }
    
    /**
     * Generate interval report V2 data with overview using ReportService
     */
    private IntervalReportResponseDtoV2 generateIntervalReportV2DataWithOverview(
            IntervalReportExportRequestV2 request) {
        
        // Convert export request to interval request
        IntervalReportRequestDtoV2 intervalRequest = IntervalReportRequestDtoV2.builder()
                .scaleIds(request.getScaleIds())
                .manufacturerIds(request.getManufacturerIds())
                .locationIds(request.getLocationIds())
                .direction(request.getDirection())
                .shiftIds(request.getShiftIds())
                .fromTime(request.getFromTime())
                .toTime(request.getToTime())
                .interval(request.getInterval())
                .aggregationByField(request.getAggregationByField())
                .ratioFormula(request.getRatioFormula())
                .page(0)
                .size(10000) // Get all data for export
                .build();
        
        // Generate report using interval service
        PageResponseDto<IntervalReportResponseDtoV2> result = intervalReportService.generateIntervalReportV2(intervalRequest);
        
        // Return the full IntervalReportResponseDtoV2 object (with overview)
        if (result.getData().isEmpty()) {
            return IntervalReportResponseDtoV2.builder()
                    .rows(List.of())
                    .overview(Map.of())
                    .build();
        }
        
        return result.getData().get(0);
    }
    
    /**
     * OLD METHOD - deprecated, keeping for backward compatibility
     * Generate interval report V2 data using ReportService
     */
    private PageResponseDto<IntervalReportResponseDtoV2.Row> generateIntervalReportV2Data(
            IntervalReportExportRequestV2 request) {
        
        // Convert export request to interval request
        IntervalReportRequestDtoV2 intervalRequest = IntervalReportRequestDtoV2.builder()
                .scaleIds(request.getScaleIds())
                .manufacturerIds(request.getManufacturerIds())
                .locationIds(request.getLocationIds())
                .direction(request.getDirection())
                .shiftIds(request.getShiftIds())
                .fromTime(request.getFromTime())
                .toTime(request.getToTime())
                .interval(request.getInterval())
                .aggregationByField(request.getAggregationByField())
                .ratioFormula(request.getRatioFormula())
                .page(0)
                .size(10000) // Get all data for export
                .build();
        
        // Generate report using interval service
        PageResponseDto<IntervalReportResponseDtoV2> result = intervalReportService.generateIntervalReportV2(intervalRequest);
        
        // Extract rows from nested structure
        if (result.getData().isEmpty()) {
            return PageResponseDto.<IntervalReportResponseDtoV2.Row>builder()
                    .data(List.of())
                    .page(0)
                    .size(0)
                    .totalElements(0L)
                    .totalPages(0)
                    .isFirst(true)
                    .isLast(true)
                    .hasNext(false)
                    .hasPrevious(false)
                    .build();
        }
        
        IntervalReportResponseDtoV2 reportData = result.getData().get(0);
        
        return PageResponseDto.<IntervalReportResponseDtoV2.Row>builder()
                .data(reportData.getRows())
                .page(0)
                .size(reportData.getRows().size())
                .totalElements((long) reportData.getRows().size())
                .totalPages(1)
                .isFirst(true)
                .isLast(true)
                .hasNext(false)
                .hasPrevious(false)
                .build();
    }
    
    /**
     * Convert Interval Report V2 data to ReportData structure
     * Transforms direction-based overview and enhanced row data into template-ready format
     */
    private ReportData convertIntervalV2ToReportData(
            IntervalReportResponseDtoV2 reportData,
            IntervalReportExportRequestV2 request) {
        
        // Get data field names from first row
        String data1Name = "Data 1";
        String data2Name = "Data 2";
        String data3Name = "Data 3";
        String data4Name = "Data 4";
        String data5Name = "Data 5";
        
        if (reportData != null && reportData.getRows() != null && !reportData.getRows().isEmpty()) {
            IntervalReportResponseDtoV2.Row firstRow = reportData.getRows().get(0);
            Map<String, IntervalReportResponseDtoV2.DataFieldValue> dataValues = firstRow.getDataValues();
            
            if (dataValues.get("data_1") != null && dataValues.get("data_1").getName() != null) {
                data1Name = dataValues.get("data_1").getName();
            }
            if (dataValues.get("data_2") != null && dataValues.get("data_2").getName() != null) {
                data2Name = dataValues.get("data_2").getName();
            }
            if (dataValues.get("data_3") != null && dataValues.get("data_3").getName() != null) {
                data3Name = dataValues.get("data_3").getName();
            }
            if (dataValues.get("data_4") != null && dataValues.get("data_4").getName() != null) {
                data4Name = dataValues.get("data_4").getName();
            }
            if (dataValues.get("data_5") != null && dataValues.get("data_5").getName() != null) {
                data5Name = dataValues.get("data_5").getName();
            }
        }
        
        // Convert rows (use reportData.getRows() which has full data including overview)
        List<ReportData.ReportRow> reportRows = new java.util.ArrayList<>();
        int rowNum = 1;
        
        if (reportData != null && reportData.getRows() != null) {
            for (IntervalReportResponseDtoV2.Row v2Row : reportData.getRows()) {
                ReportData.ReportRow reportRow = convertV2RowToReportRow(v2Row, rowNum++);
                reportRows.add(reportRow);
            }
        }
        
        // Calculate summary from overview
        ReportData.ReportSummary summary = calculateSummaryFromOverview(reportData, reportRows);
        
        // Calculate direction-based summaries
        Map<String, ReportData.DirectionSummary> directionSummariesMap = 
                calculateDirectionSummaries(reportData, reportRows);
        
        // Convert dataFieldSummaries from V2 to ReportData
        Map<String, ReportData.DataFieldSummary> dataFieldSummaries = 
                convertDataFieldSummaries(reportData);
        
        log.info("Direction summaries map keys: {}", directionSummariesMap.keySet());
        if (directionSummariesMap.get("1") != null) {
            log.info("Import summary: totalScales={}, totalRecords={}, data1Total={}", 
                    directionSummariesMap.get("1").getTotalScales(),
                    directionSummariesMap.get("1").getTotalRecords(),
                    directionSummariesMap.get("1").getData1GrandTotal());
        }
        if (directionSummariesMap.get("2") != null) {
            log.info("Export summary: totalScales={}, totalRecords={}, data1Total={}", 
                    directionSummariesMap.get("2").getTotalScales(),
                    directionSummariesMap.get("2").getTotalRecords(),
                    directionSummariesMap.get("2").getData1GrandTotal());
        }
        
        return ReportData.builder()
                .reportTitle("Báo cáo khoảng thời gian V2")
                .reportCode("INTERVAL_V2")
                .startTime(request.getFromTime())
                .endTime(request.getToTime())
                .exportTime(OffsetDateTime.now())
                .preparedBy(getCurrentUser())
                .data1Name(data1Name)
                .data2Name(data2Name)
                .data3Name(data3Name)
                .data4Name(data4Name)
                .data5Name(data5Name)
                .rows(reportRows)
                .summary(summary)
                .dataFieldSummaries(dataFieldSummaries)
                .unknownSummaries(directionSummariesMap.get("0"))
                .importSummaries(directionSummariesMap.get("1"))
                .exportSummaries(directionSummariesMap.get("2"))
                .build();
    }
    
    /**
     * Convert dataFieldSummaries from IntervalReportResponseDtoV2 to ReportData format
     */
    private Map<String, ReportData.DataFieldSummary> convertDataFieldSummaries(
            IntervalReportResponseDtoV2 reportData) {
        
        if (reportData == null || reportData.getDataFieldSummaries() == null) {
            return new HashMap<>();
        }
        
        Map<String, ReportData.DataFieldSummary> result = new HashMap<>();
        
        for (Map.Entry<String, IntervalReportResponseDtoV2.DataFieldSummary> entry : 
                reportData.getDataFieldSummaries().entrySet()) {
            
            String key = entry.getKey();
            IntervalReportResponseDtoV2.DataFieldSummary v2Summary = entry.getValue();
            
            result.put(key, ReportData.DataFieldSummary.builder()
                    .value(parseDouble(v2Summary.getValue()))
                    .aggregation(v2Summary.getAggregation())
                    .name(v2Summary.getName())
                    .unit(v2Summary.getUnit())
                    .used(v2Summary.isUsed())
                    .build());
        }
        
        return result;
    }
    
    /**
     * Convert Interval V2 Row to ReportData.ReportRow
     * Includes start values, end values, and aggregated data values
     */
    private ReportData.ReportRow convertV2RowToReportRow(IntervalReportResponseDtoV2.Row v2Row, int rowNumber) {
        String scaleName = v2Row.getScale() != null ? v2Row.getScale().getName() : "Unknown";
        Long scaleId = v2Row.getScale() != null ? v2Row.getScale().getId() : null;
        String location = v2Row.getScale() != null && v2Row.getScale().getLocation() != null 
                ? v2Row.getScale().getLocation().getName() : "";

        // Get start values (null-safe)
        Double data1Start = extractDataFieldValue(v2Row.getStartValues(), "data_1");
        Double data2Start = extractDataFieldValue(v2Row.getStartValues(), "data_2");
        Double data3Start = extractDataFieldValue(v2Row.getStartValues(), "data_3");
        Double data4Start = extractDataFieldValue(v2Row.getStartValues(), "data_4");
        Double data5Start = extractDataFieldValue(v2Row.getStartValues(), "data_5");
        
        // Get end values (null-safe)
        Double data1End = extractDataFieldValue(v2Row.getEndValues(), "data_1");
        Double data2End = extractDataFieldValue(v2Row.getEndValues(), "data_2");
        Double data3End = extractDataFieldValue(v2Row.getEndValues(), "data_3");
        Double data4End = extractDataFieldValue(v2Row.getEndValues(), "data_4");
        Double data5End = extractDataFieldValue(v2Row.getEndValues(), "data_5");
        
        // Get aggregated data values (null-safe)
        Double data1 = extractDataFieldValue(v2Row.getDataValues(), "data_1");
        Double data2 = extractDataFieldValue(v2Row.getDataValues(), "data_2");
        Double data3 = extractDataFieldValue(v2Row.getDataValues(), "data_3");
        Double data4 = extractDataFieldValue(v2Row.getDataValues(), "data_4");
        Double data5 = extractDataFieldValue(v2Row.getDataValues(), "data_5");
        
        return ReportData.ReportRow.builder()
                .rowNumber(rowNumber)
                .scaleId(scaleId)
                .scaleName(scaleName)
                .location(location)
                .period(v2Row.getPeriod())
                .data1Start(data1Start)
                .data2Start(data2Start)
                .data3Start(data3Start)
                .data4Start(data4Start)
                .data5Start(data5Start)
                .data1End(data1End)
                .data2End(data2End)
                .data3End(data3End)
                .data4End(data4End)
                .data5End(data5End)
                .data1Total(data1)
                .data2Total(data2)
                .data3Total(data3)
                .data4Total(data4)
                .data5Total(data5)
                .recordCount(v2Row.getRecordCount())
                .direction(v2Row.getDirection())
                .ratio(v2Row.getRatio() != null && v2Row.getRatio().getValue() != null 
                        ? parseDouble(v2Row.getRatio().getValue()) : null)
                .build();
    }
    
    /**
     * Calculate summary statistics from overview data
     * Aggregates across all directions (0, 1, 2)
     */
    private ReportData.ReportSummary calculateSummaryFromOverview(
            IntervalReportResponseDtoV2 reportData,
            List<ReportData.ReportRow> rows) {
        
        if (reportData == null || reportData.getOverview() == null) {
            return calculateSummaryFromRows(rows);
        }
        
        Map<String, Map<String, IntervalReportResponseDtoV2.OverviewStats>> overview = reportData.getOverview();
        
        // Aggregate across all directions
        double data1Total = 0;
        double data2Total = 0;
        double data3Total = 0;
        double data4Total = 0;
        double data5Total = 0;
        
        for (Map.Entry<String, Map<String, IntervalReportResponseDtoV2.OverviewStats>> directionEntry : overview.entrySet()) {
            Map<String, IntervalReportResponseDtoV2.OverviewStats> fieldStats = directionEntry.getValue();
            
            if (fieldStats.get("data_1") != null) {
                data1Total += parseDouble(fieldStats.get("data_1").getValue());
            }
            if (fieldStats.get("data_2") != null) {
                data2Total += parseDouble(fieldStats.get("data_2").getValue());
            }
            if (fieldStats.get("data_3") != null) {
                data3Total += parseDouble(fieldStats.get("data_3").getValue());
            }
            if (fieldStats.get("data_4") != null) {
                data4Total += parseDouble(fieldStats.get("data_4").getValue());
            }
            if (fieldStats.get("data_5") != null) {
                data5Total += parseDouble(fieldStats.get("data_5").getValue());
            }
        }
        
        int totalRecords = rows.stream()
                .mapToInt(r -> r.getRecordCount() != null ? r.getRecordCount() : 0)
                .sum();
        
        return ReportData.ReportSummary.builder()
                .totalScales((int) rows.stream().map(ReportData.ReportRow::getScaleId).distinct().count())
                .totalRecords(totalRecords)
                .data1GrandTotal(data1Total)
                .data2GrandTotal(data2Total)
                .data3GrandTotal(data3Total)
                .data4GrandTotal(data4Total)
                .data5GrandTotal(data5Total)
                .data1Average(rows.isEmpty() ? 0 : data1Total / rows.size())
                .data2Average(rows.isEmpty() ? 0 : data2Total / rows.size())
                .data3Average(rows.isEmpty() ? 0 : data3Total / rows.size())
                .data4Average(rows.isEmpty() ? 0 : data4Total / rows.size())
                .data5Average(rows.isEmpty() ? 0 : data5Total / rows.size())
                .build();
    }
    
    /**
     * Calculate direction-specific summaries from overview data
     * Returns Map with keys: "0" (unknown), "1" (nhập), "2" (xuất)
     */
    private Map<String, ReportData.DirectionSummary> calculateDirectionSummaries(
            IntervalReportResponseDtoV2 reportData,
            List<ReportData.ReportRow> rows) {
        
        Map<String, ReportData.DirectionSummary> summaries = new HashMap<>();
        
        // Initialize all 3 directions with empty summaries
        summaries.put("0", createEmptyDirectionSummary(0, "Chưa rõ"));
        summaries.put("1", createEmptyDirectionSummary(1, "Nhập"));
        summaries.put("2", createEmptyDirectionSummary(2, "Xuất"));
        
        if (reportData == null || reportData.getOverview() == null || reportData.getOverview().isEmpty()) {
            log.warn("No overview data available, falling back to row-based calculation");
            // Fallback: calculate from rows grouped by direction
            return calculateDirectionSummariesFromRows(rows);
        }
        
        Map<String, Map<String, IntervalReportResponseDtoV2.OverviewStats>> overview = reportData.getOverview();
        log.info("Processing overview with {} directions", overview.size());
        
        // Process each direction in overview
        for (Map.Entry<String, Map<String, IntervalReportResponseDtoV2.OverviewStats>> directionEntry : overview.entrySet()) {
            String directionCode = directionEntry.getKey();
            Map<String, IntervalReportResponseDtoV2.OverviewStats> fieldStats = directionEntry.getValue();
            
            log.debug("Processing direction {}: {} fields", directionCode, fieldStats.size());
            
            // Get direction name
            String directionName = getDirectionName(directionCode);
            
            // Get rows for this direction
            int dirCode = Integer.parseInt(directionCode);
            List<ReportData.ReportRow> directionRows = rows.stream()
                    .filter(r -> r.getDirection() != null && r.getDirection() == dirCode)
                    .collect(Collectors.toList());
            
            // Extract stats from overview (null-safe)
            double data1Total = extractOverviewValue(fieldStats, "data_1");
            double data2Total = extractOverviewValue(fieldStats, "data_2");
            double data3Total = extractOverviewValue(fieldStats, "data_3");
            double data4Total = extractOverviewValue(fieldStats, "data_4");
            double data5Total = extractOverviewValue(fieldStats, "data_5");
            
            int totalRecords = directionRows.stream()
                    .mapToInt(r -> r.getRecordCount() != null ? r.getRecordCount() : 0)
                    .sum();
            
            int totalScales = (int) directionRows.stream()
                    .map(ReportData.ReportRow::getScaleId)
                    .distinct()
                    .count();
            
            // Use totalRecords for average calculation, not directionRows.size()
            double data1Avg = totalRecords > 0 ? data1Total / totalRecords : 0;
            double data2Avg = totalRecords > 0 ? data2Total / totalRecords : 0;
            double data3Avg = totalRecords > 0 ? data3Total / totalRecords : 0;
            double data4Avg = totalRecords > 0 ? data4Total / totalRecords : 0;
            double data5Avg = totalRecords > 0 ? data5Total / totalRecords : 0;
            
            ReportData.DirectionSummary summary = ReportData.DirectionSummary.builder()
                    .directionCode(dirCode)
                    .directionName(directionName)
                    .totalScales(totalScales)
                    .totalRecords(totalRecords)
                    .data1GrandTotal(data1Total)
                    .data2GrandTotal(data2Total)
                    .data3GrandTotal(data3Total)
                    .data4GrandTotal(data4Total)
                    .data5GrandTotal(data5Total)
                    .data1Average(data1Avg)
                    .data2Average(data2Avg)
                    .data3Average(data3Avg)
                    .data4Average(data4Avg)
                    .data5Average(data5Avg)
                    .build();
            
            log.info("Direction {} summary: totalScales={}, totalRecords={}, data1Total={}", 
                    directionCode, totalScales, totalRecords, data1Total);
            
            summaries.put(directionCode, summary);
        }
        
        log.info("Direction summaries calculated: {}", summaries.keySet());
        return summaries;
    }
    
    /**
     * Fallback: Calculate direction summaries from rows when overview not available
     */
    private Map<String, ReportData.DirectionSummary> calculateDirectionSummariesFromRows(
            List<ReportData.ReportRow> rows) {
        
        Map<String, ReportData.DirectionSummary> summaries = new HashMap<>();
        
        // Initialize all 3 directions with empty summaries
        summaries.put("0", createEmptyDirectionSummary(0, "Unknown"));
        summaries.put("1", createEmptyDirectionSummary(1, "Nhập"));
        summaries.put("2", createEmptyDirectionSummary(2, "Xuất"));
        
        // Group rows by direction
        Map<Integer, List<ReportData.ReportRow>> rowsByDirection = rows.stream()
                .collect(Collectors.groupingBy(
                        row -> row.getDirection() != null ? row.getDirection() : 0
                ));
        
        // Calculate summary for each direction
        for (Map.Entry<Integer, List<ReportData.ReportRow>> entry : rowsByDirection.entrySet()) {
            Integer dirCode = entry.getKey();
            List<ReportData.ReportRow> dirRows = entry.getValue();
            
            double data1Total = dirRows.stream().mapToDouble(r -> r.getData1Total() != null ? r.getData1Total() : 0).sum();
            double data2Total = dirRows.stream().mapToDouble(r -> r.getData2Total() != null ? r.getData2Total() : 0).sum();
            double data3Total = dirRows.stream().mapToDouble(r -> r.getData3Total() != null ? r.getData3Total() : 0).sum();
            double data4Total = dirRows.stream().mapToDouble(r -> r.getData4Total() != null ? r.getData4Total() : 0).sum();
            double data5Total = dirRows.stream().mapToDouble(r -> r.getData5Total() != null ? r.getData5Total() : 0).sum();
            
            int totalRecords = dirRows.stream().mapToInt(r -> r.getRecordCount() != null ? r.getRecordCount() : 0).sum();
            int totalScales = (int) dirRows.stream().map(ReportData.ReportRow::getScaleId).distinct().count();
            
            // Use totalRecords for average calculation, not dirRows.size()
            double data1Avg = totalRecords > 0 ? data1Total / totalRecords : 0;
            double data2Avg = totalRecords > 0 ? data2Total / totalRecords : 0;
            double data3Avg = totalRecords > 0 ? data3Total / totalRecords : 0;
            double data4Avg = totalRecords > 0 ? data4Total / totalRecords : 0;
            double data5Avg = totalRecords > 0 ? data5Total / totalRecords : 0;
            
            ReportData.DirectionSummary summary = ReportData.DirectionSummary.builder()
                    .directionCode(dirCode)
                    .directionName(getDirectionName(String.valueOf(dirCode)))
                    .totalScales(totalScales)
                    .totalRecords(totalRecords)
                    .data1GrandTotal(data1Total)
                    .data2GrandTotal(data2Total)
                    .data3GrandTotal(data3Total)
                    .data4GrandTotal(data4Total)
                    .data5GrandTotal(data5Total)
                    .data1Average(data1Avg)
                    .data2Average(data2Avg)
                    .data3Average(data3Avg)
                    .data4Average(data4Avg)
                    .data5Average(data5Avg)
                    .build();
            
            summaries.put(String.valueOf(dirCode), summary);
        }
        
        return summaries;
    }
    
    /**
     * Get direction name from code
     */
    private String getDirectionName(String directionCode) {
        return switch (directionCode) {
            case "0" -> "Unknown";
            case "1" -> "Nhập";
            case "2" -> "Xuất";
            default -> "Unknown";
        };
    }
    
    /**
     * Create an empty DirectionSummary for a direction
     */
    private ReportData.DirectionSummary createEmptyDirectionSummary(Integer directionCode, String directionName) {
        return ReportData.DirectionSummary.builder()
                .directionCode(directionCode)
                .directionName(directionName)
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
                .build();
    }
    
    /**
     * Fallback: Calculate summary directly from rows if overview not available
     */
    private ReportData.ReportSummary calculateSummaryFromRows(List<ReportData.ReportRow> rows) {
        if (rows.isEmpty()) {
            return ReportData.ReportSummary.builder()
                    .totalScales(0)
                    .totalRecords(0)
                    .build();
        }
        
        double data1Total = rows.stream().mapToDouble(r -> r.getData1Total() != null ? r.getData1Total() : 0).sum();
        double data2Total = rows.stream().mapToDouble(r -> r.getData2Total() != null ? r.getData2Total() : 0).sum();
        double data3Total = rows.stream().mapToDouble(r -> r.getData3Total() != null ? r.getData3Total() : 0).sum();
        double data4Total = rows.stream().mapToDouble(r -> r.getData4Total() != null ? r.getData4Total() : 0).sum();
        double data5Total = rows.stream().mapToDouble(r -> r.getData5Total() != null ? r.getData5Total() : 0).sum();
        
        int totalRecords = rows.stream().mapToInt(r -> r.getRecordCount() != null ? r.getRecordCount() : 0).sum();
        
        return ReportData.ReportSummary.builder()
                .totalScales((int) rows.stream().map(ReportData.ReportRow::getScaleId).distinct().count())
                .totalRecords(totalRecords)
                .data1GrandTotal(data1Total)
                .data2GrandTotal(data2Total)
                .data3GrandTotal(data3Total)
                .data4GrandTotal(data4Total)
                .data5GrandTotal(data5Total)
                .data1Average(data1Total / rows.size())
                .data2Average(data2Total / rows.size())
                .data3Average(data3Total / rows.size())
                .data4Average(data4Total / rows.size())
                .data5Average(data5Total / rows.size())
                .build();
    }
    
    /**
     * Parse double value from DataFieldValue or string
     */
    private Double parseDouble(Object value) {
        if (value == null) return 0.0;
        
        if (value instanceof IntervalReportResponseDtoV2.DataFieldValue) {
            return parseDouble(((IntervalReportResponseDtoV2.DataFieldValue) value).getValue());
        }
        
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    /**
     * Safely extract double value from DataFieldValue map
     */
    private Double extractDataFieldValue(Map<String, IntervalReportResponseDtoV2.DataFieldValue> map, String key) {
        if (map == null || key == null) return 0.0;
        IntervalReportResponseDtoV2.DataFieldValue fieldValue = map.get(key);
        if (fieldValue == null || fieldValue.getValue() == null) return 0.0;
        return parseDouble(fieldValue.getValue());
    }
    
    /**
     * Safely extract double value from OverviewStats map
     */
    private Double extractOverviewValue(Map<String, IntervalReportResponseDtoV2.OverviewStats> map, String key) {
        if (map == null || key == null) return 0.0;
        IntervalReportResponseDtoV2.OverviewStats stats = map.get(key);
        if (stats == null || stats.getValue() == null) return 0.0;
        return parseDouble(stats.getValue());
    }
    
    /**
     * Generate filename for Interval Report V2 export
     */
    public String generateFilenameForIntervalV2(IntervalReportExportRequestV2 request) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String fromDateStr = formatter.format(request.getFromTime());
        String toDateStr = formatter.format(request.getToTime());
        
        String intervalStr = request.getInterval().name();
        
        // Determine extension from template
        TemplateImport templateImport = templateImportRepository.findById(request.getImportId())
                .orElseThrow(() -> new RuntimeException("Template not found"));
        
        String filename = templateImport.getOriginalFilename();
        String extension = filename.substring(filename.lastIndexOf('.'));
        
        return String.format("IntervalReportV2_%s_%s_%s%s", 
                intervalStr, fromDateStr, toDateStr, extension);
    }
    
    /**
     * Convert DirectionSummary to Map for template access
     */
    private Map<String, Object> convertDirectionSummaryToMap(ReportData.DirectionSummary dirSummary) {
        Map<String, Object> map = new HashMap<>();
        map.put("directionCode", dirSummary.getDirectionCode());
        map.put("directionName", dirSummary.getDirectionName());
        map.put("totalScales", dirSummary.getTotalScales() != null ? dirSummary.getTotalScales() : 0);
        map.put("totalRecords", dirSummary.getTotalRecords() != null ? dirSummary.getTotalRecords() : 0);
        map.put("data1GrandTotal", NUMBER_FORMAT.format(dirSummary.getData1GrandTotal() != null ? dirSummary.getData1GrandTotal() : 0));
        map.put("data2GrandTotal", NUMBER_FORMAT.format(dirSummary.getData2GrandTotal() != null ? dirSummary.getData2GrandTotal() : 0));
        map.put("data3GrandTotal", NUMBER_FORMAT.format(dirSummary.getData3GrandTotal() != null ? dirSummary.getData3GrandTotal() : 0));
        map.put("data4GrandTotal", NUMBER_FORMAT.format(dirSummary.getData4GrandTotal() != null ? dirSummary.getData4GrandTotal() : 0));
        map.put("data5GrandTotal", NUMBER_FORMAT.format(dirSummary.getData5GrandTotal() != null ? dirSummary.getData5GrandTotal() : 0));
        map.put("data1Average", NUMBER_FORMAT.format(dirSummary.getData1Average() != null ? dirSummary.getData1Average() : 0));
        map.put("data2Average", NUMBER_FORMAT.format(dirSummary.getData2Average() != null ? dirSummary.getData2Average() : 0));
        map.put("data3Average", NUMBER_FORMAT.format(dirSummary.getData3Average() != null ? dirSummary.getData3Average() : 0));
        map.put("data4Average", NUMBER_FORMAT.format(dirSummary.getData4Average() != null ? dirSummary.getData4Average() : 0));
        map.put("data5Average", NUMBER_FORMAT.format(dirSummary.getData5Average() != null ? dirSummary.getData5Average() : 0));
        return map;
    }
    
    /**
     * Get direction name from direction code
     */
    private String getDirectionNameFromCode(Integer direction) {
        if (direction == null) {
            return "Chưa rõ";
        }
        return switch (direction) {
            case 1 -> "Nhập";
            case 2 -> "Xuất";
            default -> "Chưa rõ";
        };
    }
    
    /**
     * Get current authenticated user
     */
    private String getCurrentUser() {
        try {
            org.springframework.security.core.Authentication auth = 
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                return auth.getName();
            }
        } catch (Exception e) {
            log.trace("Could not get authenticated user: {}", e.getMessage());
        }
        return "System";
    }
}
