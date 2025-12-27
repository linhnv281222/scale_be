package org.facenet.service.report;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.facenet.config.report.ReportLayoutConstants;
import org.facenet.config.report.ReportStyleConstants;
import org.facenet.dto.report.ReportData;
import org.facenet.entity.report.OrganizationSettings;
import org.facenet.entity.report.ReportColumn;
import org.facenet.entity.report.ReportTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Service for exporting reports to Excel format
 * Uses SXSSF (Streaming) to handle large datasets efficiently
 * Implements ENTERPRISE_STANDARD layout with SCALEHUB_OFFICIAL styling
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelExportService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = 
            DateTimeFormatter.ofPattern(ReportStyleConstants.SCALEHUB_OFFICIAL.DATETIME_FORMAT);
    
    private static final DecimalFormat NUMBER_FORMAT = 
            new DecimalFormat(ReportStyleConstants.SCALEHUB_OFFICIAL.NUMBER_FORMAT_PATTERN);
    
    private final ReportTemplateService templateService;

    /**
     * Export report data to Excel using template
     */
    public byte[] exportToExcel(ReportData reportData, Long templateId) throws IOException {
        ReportTemplate template = templateId != null 
                ? templateService.getTemplate(templateId)
                : templateService.getDefaultTemplate(ReportTemplate.ReportType.EXCEL);
        
        return exportToExcel(reportData, template);
    }

    /**
     * Export report data to Excel with ENTERPRISE_STANDARD layout
     * and SCALEHUB_OFFICIAL styling
     */
    public byte[] exportToExcel(ReportData reportData, ReportTemplate template) throws IOException {
        log.info("Exporting report to Excel with template: {} (ENTERPRISE_STANDARD layout)", template.getCode());

        // Use SXSSFWorkbook for streaming (keeps only 100 rows in memory)
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            Sheet sheet = workbook.createSheet("Báo cáo sản lượng");

            // Create cell styles following SCALEHUB_OFFICIAL profile
            StyleSet styles = createStyleSet(workbook);

            int rowIndex = 0;

            // ENTERPRISE_STANDARD Layout Implementation

            // 1. Header Section (Logo, Title, Code)
            rowIndex = createEnterpriseHeader(sheet, reportData, styles, rowIndex);

            // 2. Metadata Section
            rowIndex = createMetadataSection(sheet, reportData, styles, rowIndex);

            // 3. Spacing after header
            rowIndex += ReportLayoutConstants.ENTERPRISE_STANDARD.SPACING_AFTER_HEADER;

            // 4. Data Table with Headers
            rowIndex = createColumnHeaders(sheet, styles.headerStyle, rowIndex, template);
            rowIndex = createDataRows(sheet, reportData, template, styles, rowIndex);

            // 5. Spacing after table
            rowIndex += ReportLayoutConstants.ENTERPRISE_STANDARD.SPACING_AFTER_TABLE;

            // 6. Summary Section
            if (ReportLayoutConstants.ENTERPRISE_STANDARD.FOOTER_SHOW_SUMMARY) {
                rowIndex = createSummary(sheet, reportData, styles, rowIndex);
            }

            // 7. Signature Block
            if (ReportLayoutConstants.ENTERPRISE_STANDARD.FOOTER_SHOW_SIGNATURES) {
                rowIndex += ReportLayoutConstants.ENTERPRISE_STANDARD.SPACING_BEFORE_SIGNATURE;
                createSignatureBlock(sheet, styles, rowIndex);
            }

            // Set column widths based on template
            setColumnWidths(sheet, template);

            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.dispose(); // Clean up temporary files

            log.info("Excel export completed. Size: {} bytes", outputStream.size());
            return outputStream.toByteArray();
        }
    }

    /**
     * Style set container
     */
    private static class StyleSet {
        CellStyle titleStyle;
        CellStyle headerStyle;
        CellStyle dataStyle;
        CellStyle numberStyle;
        CellStyle dateStyle;
        CellStyle summaryStyle;
        CellStyle zebraEvenStyle;
        CellStyle zebraOddStyle;
    }

    /**
     * Create enterprise header (Logo, Title, Report Code)
     * Implements ENTERPRISE_STANDARD header specification
     */
    private int createEnterpriseHeader(Sheet sheet, ReportData reportData, StyleSet styles, int startRow) {
        int rowIndex = startRow;

        // Title row (CENTER, BOLD, Size 16)
        Row titleRow = sheet.createRow(rowIndex++);
        titleRow.setHeightInPoints(ReportStyleConstants.SCALEHUB_OFFICIAL.ROW_HEIGHT_HEADER);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(reportData.getReportTitle());
        titleCell.setCellStyle(styles.titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(titleRow.getRowNum(), titleRow.getRowNum(), 0, 10));

        // Report code (RIGHT aligned)
        Row codeRow = sheet.createRow(rowIndex++);
        Cell codeCell = codeRow.createCell(0);
        codeCell.setCellValue("Mã báo cáo: " + reportData.getReportCode());
        codeCell.setCellStyle(styles.dataStyle);
        sheet.addMergedRegion(new CellRangeAddress(codeRow.getRowNum(), codeRow.getRowNum(), 0, 10));

        return rowIndex;
    }

    /**
     * Create metadata section
     */
    private int createMetadataSection(Sheet sheet, ReportData reportData, StyleSet styles, int startRow) {
        int rowIndex = startRow;

        // Export time
        Row exportRow = sheet.createRow(rowIndex++);
        Cell exportCell = exportRow.createCell(0);
        exportCell.setCellValue("Thời gian xuất: " + DATE_TIME_FORMATTER.format(reportData.getExportTime()));
        exportCell.setCellStyle(styles.dataStyle);
        sheet.addMergedRegion(new CellRangeAddress(exportRow.getRowNum(), exportRow.getRowNum(), 0, 10));

        // Date range
        Row timeRow = sheet.createRow(rowIndex++);
        Cell timeCell = timeRow.createCell(0);
        timeCell.setCellValue("Khoảng thời gian: " + reportData.getMetadata().get("dateRange"));
        timeCell.setCellStyle(styles.dataStyle);
        sheet.addMergedRegion(new CellRangeAddress(timeRow.getRowNum(), timeRow.getRowNum(), 0, 10));

        // Scale list
        if (reportData.getMetadata().containsKey("scaleList")) {
            Row scaleRow = sheet.createRow(rowIndex++);
            Cell scaleCell = scaleRow.createCell(0);
            scaleCell.setCellValue("Trạm cân: " + reportData.getMetadata().get("scaleList"));
            scaleCell.setCellStyle(styles.dataStyle);
            sheet.addMergedRegion(new CellRangeAddress(scaleRow.getRowNum(), scaleRow.getRowNum(), 0, 10));
        }

        // Prepared by
        Row preparedRow = sheet.createRow(rowIndex++);
        Cell preparedCell = preparedRow.createCell(0);
        preparedCell.setCellValue("Người thực hiện: " + reportData.getPreparedBy());
        preparedCell.setCellStyle(styles.dataStyle);
        sheet.addMergedRegion(new CellRangeAddress(preparedRow.getRowNum(), preparedRow.getRowNum(), 0, 10));

        return rowIndex;
    }

    /**
     * Create header section with title and metadata
     * @deprecated Use createEnterpriseHeader and createMetadataSection instead
     */
    @Deprecated
    private int createHeader(Sheet sheet, ReportData reportData, CellStyle titleStyle, CellStyle dataStyle, int startRow) {
        int rowIndex = startRow;

        // Title row
        Row titleRow = sheet.createRow(rowIndex++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(reportData.getReportTitle());
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(titleRow.getRowNum(), titleRow.getRowNum(), 0, 11));

        // Report code
        Row codeRow = sheet.createRow(rowIndex++);
        Cell codeCell = codeRow.createCell(0);
        codeCell.setCellValue("Mã báo cáo: " + reportData.getReportCode());
        codeCell.setCellStyle(dataStyle);
        sheet.addMergedRegion(new CellRangeAddress(codeRow.getRowNum(), codeRow.getRowNum(), 0, 11));

        // Time range
        Row timeRow = sheet.createRow(rowIndex++);
        Cell timeCell = timeRow.createCell(0);
        timeCell.setCellValue("Thời gian: " + reportData.getMetadata().get("dateRange"));
        timeCell.setCellStyle(dataStyle);
        sheet.addMergedRegion(new CellRangeAddress(timeRow.getRowNum(), timeRow.getRowNum(), 0, 11));

        // Export time
        Row exportRow = sheet.createRow(rowIndex++);
        Cell exportCell = exportRow.createCell(0);
        exportCell.setCellValue("Thời gian xuất: " + DATE_TIME_FORMATTER.format(reportData.getExportTime()));
        exportCell.setCellStyle(dataStyle);
        sheet.addMergedRegion(new CellRangeAddress(exportRow.getRowNum(), exportRow.getRowNum(), 0, 11));

        // Prepared by
        Row preparedRow = sheet.createRow(rowIndex++);
        Cell preparedCell = preparedRow.createCell(0);
        preparedCell.setCellValue("Người thực hiện: " + reportData.getPreparedBy());
        preparedCell.setCellStyle(dataStyle);
        sheet.addMergedRegion(new CellRangeAddress(preparedRow.getRowNum(), preparedRow.getRowNum(), 0, 11));

        // Empty row
        rowIndex++;

        return rowIndex;
    }

    /**
     * Create column headers (dynamic from template)
     */
    private int createColumnHeaders(Sheet sheet, CellStyle headerStyle, int startRow, ReportTemplate template) {
        Row headerRow = sheet.createRow(startRow);
        
        // Get sorted columns
        List<ReportColumn> columns = template.getColumns();
        columns.sort(Comparator.comparingInt(ReportColumn::getColumnOrder));
        
        for (int i = 0; i < columns.size(); i++) {
            ReportColumn column = columns.get(i);
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(column.getColumnLabel());
            cell.setCellStyle(headerStyle);
            
            // Set column width (POI max is 255 characters = 65280 units)
            int width = Math.min(column.getWidth() * 50, 65280);
            sheet.setColumnWidth(i, width);
        }

        return startRow + 1;
    }

    /**
     * Create data rows with zebra striping (ENTERPRISE_STANDARD specification)
     */
    private int createDataRows(Sheet sheet, ReportData reportData, ReportTemplate template,
                               StyleSet styles, int startRow) {
        int rowIndex = startRow;
        
        // Get sorted columns
        List<ReportColumn> columns = template.getColumns();
        columns.sort(Comparator.comparingInt(ReportColumn::getColumnOrder));

        boolean useZebraStriping = ReportLayoutConstants.ENTERPRISE_STANDARD.TABLE_ZEBRA_STRIPING;
        
        for (ReportData.ReportRow rowData : reportData.getRows()) {
            Row row = sheet.createRow(rowIndex);
            row.setHeightInPoints(ReportStyleConstants.SCALEHUB_OFFICIAL.ROW_HEIGHT_DATA);
            
            // Apply zebra striping
            boolean isEven = (rowIndex - startRow) % 2 == 0;
            CellStyle zebraStyle = useZebraStriping && isEven ? styles.zebraEvenStyle : styles.zebraOddStyle;

            for (int i = 0; i < columns.size(); i++) {
                ReportColumn column = columns.get(i);
                Cell cell = row.createCell(i);
                
                // Get value based on column configuration
                Object value = getColumnValue(rowData, column);
                
                // Apply appropriate style based on data type
                if (value instanceof Number) {
                    cell.setCellValue(((Number) value).doubleValue());
                    cell.setCellStyle(styles.numberStyle);
                } else if (value instanceof java.time.OffsetDateTime) {
                    cell.setCellValue(DATE_TIME_FORMATTER.format((java.time.OffsetDateTime) value));
                    cell.setCellStyle(styles.dateStyle);
                } else {
                    cell.setCellValue(value != null ? value.toString() : "");
                    // Apply zebra styling only for text cells
                    cell.setCellStyle(zebraStyle);
                }
            }
            
            rowIndex++;
        }

        return rowIndex;
    }

    /**
     * Create data rows (dynamic from template)
     * @deprecated Use createDataRows with StyleSet instead
     */
    @Deprecated
    private int createDataRows(Sheet sheet, ReportData reportData, ReportTemplate template,
                               CellStyle dataStyle, CellStyle numberStyle, CellStyle dateStyle, int startRow) {
        int rowIndex = startRow;
        
        // Get sorted columns
        List<ReportColumn> columns = template.getColumns();
        columns.sort(Comparator.comparingInt(ReportColumn::getColumnOrder));

        for (ReportData.ReportRow rowData : reportData.getRows()) {
            Row row = sheet.createRow(rowIndex++);

            for (int i = 0; i < columns.size(); i++) {
                ReportColumn column = columns.get(i);
                Cell cell = row.createCell(i);
                
                // Get value based on column configuration
                Object value = getColumnValue(rowData, column);
                
                if (value instanceof Number) {
                    cell.setCellValue(((Number) value).doubleValue());
                    cell.setCellStyle(numberStyle);
                } else if (value instanceof java.time.OffsetDateTime) {
                    cell.setCellValue(DATE_TIME_FORMATTER.format((java.time.OffsetDateTime) value));
                    cell.setCellStyle(dateStyle);
                } else {
                    cell.setCellValue(value != null ? value.toString() : "");
                    cell.setCellStyle(dataStyle);
                }
            }
        }

        return rowIndex;
    }
    
    /**
     * Get column value from row data
     */
    private Object getColumnValue(ReportData.ReportRow row, ReportColumn column) {
        return switch (column.getDataField()) {
            case "row_number" -> row.getRowNumber();
            case "scale_code" -> row.getScaleCode();
            case "scale_name" -> row.getScaleName();
            case "location" -> row.getLocation();
            case "data_1" -> row.getData1Total();
            case "data_2" -> row.getData2Total();
            case "data_3" -> row.getData3Total();
            case "data_4" -> row.getData4Total();
            case "data_5" -> row.getData5Total();
            case "record_count" -> row.getRecordCount();
            case "last_time" -> row.getLastTime();
            default -> "";
        };
    }

    /**
     * Create summary section with ENTERPRISE_STANDARD styling
     */
    private int createSummary(Sheet sheet, ReportData reportData, StyleSet styles, int startRow) {
        ReportData.ReportSummary summary = reportData.getSummary();
        int rowIndex = startRow;

        // Total row
        Row totalRow = sheet.createRow(rowIndex++);
        totalRow.setHeightInPoints(ReportStyleConstants.SCALEHUB_OFFICIAL.ROW_HEIGHT_SUMMARY);
        Cell totalLabelCell = totalRow.createCell(0);
        totalLabelCell.setCellValue("TỔNG CỘNG");
        totalLabelCell.setCellStyle(styles.summaryStyle);
        sheet.addMergedRegion(new CellRangeAddress(totalRow.getRowNum(), totalRow.getRowNum(), 0, 3));

        createNumberCell(totalRow, 4, summary.getData1GrandTotal(), styles.summaryStyle);
        createNumberCell(totalRow, 5, summary.getData2GrandTotal(), styles.summaryStyle);
        createNumberCell(totalRow, 6, summary.getData3GrandTotal(), styles.summaryStyle);
        createNumberCell(totalRow, 7, summary.getData4GrandTotal(), styles.summaryStyle);
        createNumberCell(totalRow, 8, summary.getData5GrandTotal(), styles.summaryStyle);

        Cell totalRecordsCell = totalRow.createCell(9);
        totalRecordsCell.setCellValue(summary.getTotalRecords() != null ? summary.getTotalRecords() : 0);
        totalRecordsCell.setCellStyle(styles.summaryStyle);

        // Average row
        Row avgRow = sheet.createRow(rowIndex++);
        avgRow.setHeightInPoints(ReportStyleConstants.SCALEHUB_OFFICIAL.ROW_HEIGHT_SUMMARY);
        Cell avgLabelCell = avgRow.createCell(0);
        avgLabelCell.setCellValue("TRUNG BÌNH");
        avgLabelCell.setCellStyle(styles.summaryStyle);
        sheet.addMergedRegion(new CellRangeAddress(avgRow.getRowNum(), avgRow.getRowNum(), 0, 3));

        createNumberCell(avgRow, 4, summary.getData1Average(), styles.summaryStyle);
        createNumberCell(avgRow, 5, summary.getData2Average(), styles.summaryStyle);
        createNumberCell(avgRow, 6, summary.getData3Average(), styles.summaryStyle);
        createNumberCell(avgRow, 7, summary.getData4Average(), styles.summaryStyle);
        createNumberCell(avgRow, 8, summary.getData5Average(), styles.summaryStyle);

        // Max row
        Row maxRow = sheet.createRow(rowIndex++);
        maxRow.setHeightInPoints(ReportStyleConstants.SCALEHUB_OFFICIAL.ROW_HEIGHT_SUMMARY);
        Cell maxLabelCell = maxRow.createCell(0);
        maxLabelCell.setCellValue("GIÁ TRỊ LỚN NHẤT");
        maxLabelCell.setCellStyle(styles.summaryStyle);
        sheet.addMergedRegion(new CellRangeAddress(maxRow.getRowNum(), maxRow.getRowNum(), 0, 3));

        createNumberCell(maxRow, 4, summary.getData1Max(), styles.summaryStyle);
        createNumberCell(maxRow, 5, summary.getData2Max(), styles.summaryStyle);
        createNumberCell(maxRow, 6, summary.getData3Max(), styles.summaryStyle);
        createNumberCell(maxRow, 7, summary.getData4Max(), styles.summaryStyle);
        createNumberCell(maxRow, 8, summary.getData5Max(), styles.summaryStyle);

        return rowIndex;
    }

    /**
     * Create signature block (ENTERPRISE_STANDARD specification)
     */
    private void createSignatureBlock(Sheet sheet, StyleSet styles, int startRow) {
        String[] roles = ReportLayoutConstants.ENTERPRISE_STANDARD.SIGNATURE_ROLES;
        
        // Signature date row
        Row dateRow = sheet.createRow(startRow++);
        Cell dateCell = dateRow.createCell(0);
        dateCell.setCellValue(ReportStyleConstants.SCALEHUB_OFFICIAL.SIGNATURE_DATE_FORMAT);
        dateCell.setCellStyle(styles.dataStyle);
        sheet.addMergedRegion(new CellRangeAddress(dateRow.getRowNum(), dateRow.getRowNum(), 0, 10));
        
        // Signature titles row
        Row signatureRow = sheet.createRow(startRow + 1);
        
        int colSpacing = 3; // Spacing between signatures
        for (int i = 0; i < roles.length && i < 3; i++) {
            Cell cell = signatureRow.createCell(i * colSpacing + 1);
            cell.setCellValue(roles[i]);
            
            CellStyle signatureStyle = sheet.getWorkbook().createCellStyle();
            Font font = sheet.getWorkbook().createFont();
            font.setBold(true);
            font.setFontHeightInPoints((short) ReportStyleConstants.SCALEHUB_OFFICIAL.SIGNATURE_FONT_SIZE);
            font.setFontName(ReportStyleConstants.SCALEHUB_OFFICIAL.FONT_FAMILY);
            signatureStyle.setFont(font);
            signatureStyle.setAlignment(HorizontalAlignment.CENTER);
            
            cell.setCellStyle(signatureStyle);
        }
    }

    /**
     * Set column widths based on template
     */
    private void setColumnWidths(Sheet sheet, ReportTemplate template) {
        List<ReportColumn> columns = template.getColumns();
        columns.sort(Comparator.comparingInt(ReportColumn::getColumnOrder));
        
        for (int i = 0; i < columns.size(); i++) {
            ReportColumn column = columns.get(i);
            // POI uses 1/256th of character width (max 255 characters = 65280 units)
            int width = Math.min(column.getWidth() * 256, 65280);
            sheet.setColumnWidth(i, width);
        }
    }

    /**
     * Create summary section
     * @deprecated Use createSummary with StyleSet instead
     */
    @Deprecated
    private void createSummary(Sheet sheet, ReportData reportData, CellStyle summaryStyle, 
                               CellStyle numberStyle, int startRow) {
        ReportData.ReportSummary summary = reportData.getSummary();

        // Empty row
        startRow++;

        // Total row
        Row totalRow = sheet.createRow(startRow++);
        Cell totalLabelCell = totalRow.createCell(0);
        totalLabelCell.setCellValue("TỔNG CỘNG");
        totalLabelCell.setCellStyle(summaryStyle);
        sheet.addMergedRegion(new CellRangeAddress(totalRow.getRowNum(), totalRow.getRowNum(), 0, 3));

        createNumberCell(totalRow, 4, summary.getData1GrandTotal(), summaryStyle);
        createNumberCell(totalRow, 5, summary.getData2GrandTotal(), summaryStyle);
        createNumberCell(totalRow, 6, summary.getData3GrandTotal(), summaryStyle);
        createNumberCell(totalRow, 7, summary.getData4GrandTotal(), summaryStyle);
        createNumberCell(totalRow, 8, summary.getData5GrandTotal(), summaryStyle);

        Cell totalRecordsCell = totalRow.createCell(9);
        totalRecordsCell.setCellValue(summary.getTotalRecords() != null ? summary.getTotalRecords() : 0);
        totalRecordsCell.setCellStyle(summaryStyle);

        // Average row
        Row avgRow = sheet.createRow(startRow++);
        Cell avgLabelCell = avgRow.createCell(0);
        avgLabelCell.setCellValue("TRUNG BÌNH");
        avgLabelCell.setCellStyle(summaryStyle);
        sheet.addMergedRegion(new CellRangeAddress(avgRow.getRowNum(), avgRow.getRowNum(), 0, 3));

        createNumberCell(avgRow, 4, summary.getData1Average(), summaryStyle);
        createNumberCell(avgRow, 5, summary.getData2Average(), summaryStyle);
        createNumberCell(avgRow, 6, summary.getData3Average(), summaryStyle);
        createNumberCell(avgRow, 7, summary.getData4Average(), summaryStyle);
        createNumberCell(avgRow, 8, summary.getData5Average(), summaryStyle);

        // Max row
        Row maxRow = sheet.createRow(startRow++);
        Cell maxLabelCell = maxRow.createCell(0);
        maxLabelCell.setCellValue("GIÁ TRỊ LỚN NHẤT");
        maxLabelCell.setCellStyle(summaryStyle);
        sheet.addMergedRegion(new CellRangeAddress(maxRow.getRowNum(), maxRow.getRowNum(), 0, 3));

        createNumberCell(maxRow, 4, summary.getData1Max(), summaryStyle);
        createNumberCell(maxRow, 5, summary.getData2Max(), summaryStyle);
        createNumberCell(maxRow, 6, summary.getData3Max(), summaryStyle);
        createNumberCell(maxRow, 7, summary.getData4Max(), summaryStyle);
        createNumberCell(maxRow, 8, summary.getData5Max(), summaryStyle);

        // Signature section
        startRow += 2;
        Row signatureRow = sheet.createRow(startRow);
        
        Cell preparedCell = signatureRow.createCell(1);
        preparedCell.setCellValue("Người lập biểu");
        preparedCell.setCellStyle(summaryStyle);
        
        Cell managerCell = signatureRow.createCell(5);
        managerCell.setCellValue("Trưởng bộ phận");
        managerCell.setCellStyle(summaryStyle);
        
        Cell directorCell = signatureRow.createCell(9);
        directorCell.setCellValue("Giám đốc");
        directorCell.setCellStyle(summaryStyle);
    }

    /**
     * Create a number cell with formatting
     */
    private void createNumberCell(Row row, int column, Double value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(ReportDataService.round(value, 2));
        } else {
            cell.setCellValue(0.0);
        }
        cell.setCellStyle(style);
    }

    /**
     * Create all cell styles using SCALEHUB_OFFICIAL style profile
     */
    private StyleSet createStyleSet(Workbook workbook) {
        StyleSet styles = new StyleSet();
        styles.titleStyle = createTitleStyle(workbook);
        styles.headerStyle = createHeaderStyle(workbook);
        styles.dataStyle = createDataStyle(workbook);
        styles.numberStyle = createNumberStyle(workbook);
        styles.dateStyle = createDateStyle(workbook);
        styles.summaryStyle = createSummaryStyle(workbook);
        styles.zebraEvenStyle = createZebraEvenStyle(workbook);
        styles.zebraOddStyle = createZebraOddStyle(workbook);
        return styles;
    }

    /**
     * Create title style (SCALEHUB_OFFICIAL)
     */
    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(ReportLayoutConstants.ENTERPRISE_STANDARD.TITLE_BOLD);
        font.setFontHeightInPoints((short) ReportStyleConstants.SCALEHUB_OFFICIAL.FONT_SIZE_TITLE);
        font.setFontName(ReportStyleConstants.SCALEHUB_OFFICIAL.FONT_FAMILY);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    /**
     * Create header style (SCALEHUB_OFFICIAL)
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(ReportLayoutConstants.ENTERPRISE_STANDARD.TABLE_HEADER_BOLD);
        font.setFontHeightInPoints((short) ReportStyleConstants.SCALEHUB_OFFICIAL.FONT_SIZE_BODY);
        font.setFontName(ReportStyleConstants.SCALEHUB_OFFICIAL.FONT_FAMILY);
        style.setFont(font);
        
        // Light blue background
        style.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setWrapText(ReportLayoutConstants.ENTERPRISE_STANDARD.TABLE_HEADER_BOLD);
        return style;
    }

    /**
     * Create data style (SCALEHUB_OFFICIAL)
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) ReportStyleConstants.SCALEHUB_OFFICIAL.FONT_SIZE_BODY);
        font.setFontName(ReportStyleConstants.SCALEHUB_OFFICIAL.FONT_FAMILY);
        style.setFont(font);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    /**
     * Create zebra even row style (light gray background)
     */
    private CellStyle createZebraEvenStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    /**
     * Create zebra odd row style (white background)
     */
    private CellStyle createZebraOddStyle(Workbook workbook) {
        return createDataStyle(workbook);
    }

    /**
     * Create number style with formatting (SCALEHUB_OFFICIAL)
     */
    private CellStyle createNumberStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        style.setDataFormat(workbook.createDataFormat().getFormat(
                ReportStyleConstants.SCALEHUB_OFFICIAL.NUMBER_FORMAT_PATTERN));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    /**
     * Create date style (SCALEHUB_OFFICIAL)
     */
    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    /**
     * Create summary style (SCALEHUB_OFFICIAL)
     */
    private CellStyle createSummaryStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) ReportStyleConstants.SCALEHUB_OFFICIAL.FONT_SIZE_BODY);
        font.setFontName(ReportStyleConstants.SCALEHUB_OFFICIAL.FONT_FAMILY);
        style.setFont(font);
        
        // Light gray background for summary
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setDataFormat(workbook.createDataFormat().getFormat(
                ReportStyleConstants.SCALEHUB_OFFICIAL.NUMBER_FORMAT_PATTERN));
        return style;
    }
}
