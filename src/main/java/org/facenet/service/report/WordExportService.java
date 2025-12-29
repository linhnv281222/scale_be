package org.facenet.service.report;

import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.config.Configure;
import com.deepoove.poi.plugin.table.LoopRowTableRenderPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.facenet.dto.report.ReportData;
import org.facenet.entity.report.OrganizationSettings;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for exporting reports to Word format
 * ENTERPRISE_STANDARD layout with professional styling
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WordExportService {

    private final ReportTemplateService reportTemplateService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String TEMPLATE_PATH = "templates/enterprise-report-template.docx";
    
    // Style constants - ENTERPRISE_STANDARD
    private static final String FONT_FAMILY = "Times New Roman";
    private static final int TITLE_FONT_SIZE = 16;
    private static final int HEADER_FONT_SIZE = 12;
    private static final int BODY_FONT_SIZE = 11;
    private static final String COLOR_HEADER_BG = "D9D9D9";
    private static final String COLOR_ZEBRA_ODD = "F5F5F5";
    private static final String COLOR_SUMMARY_BG = "E0E0E0";
    
    // Number format for Vietnamese locale (1.234,56)
    private static final DecimalFormat NUMBER_FORMAT;
    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        NUMBER_FORMAT = new DecimalFormat("#,##0.00", symbols);
    }

    /**
     * Export report data to Word document
     */
    public byte[] exportToWord(ReportData reportData) throws IOException {
        log.info("Exporting report to Word: {}", reportData.getReportTitle());

        // Try to load template first
        InputStream templateStream = null;
        try {
            templateStream = new ClassPathResource(TEMPLATE_PATH).getInputStream();
            return exportWithTemplate(reportData, templateStream);
        } catch (IOException e) {
            log.warn("Template not found at {}, generating programmatically", TEMPLATE_PATH);
            return generateEnterpriseReport(reportData);
        } finally {
            if (templateStream != null) {
                try { templateStream.close(); } catch (IOException ignored) {}
            }
        }
    }

    /**
     * Export using POI-TL template
     */
    private byte[] exportWithTemplate(ReportData reportData, InputStream templateStream) throws IOException {
        LoopRowTableRenderPolicy policy = new LoopRowTableRenderPolicy();
        Configure config = Configure.builder()
                .bind("rows", policy)
                .build();

        Map<String, Object> dataModel = prepareDataModel(reportData);
        XWPFTemplate template = XWPFTemplate.compile(templateStream, config).render(dataModel);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        template.write(outputStream);
        template.close();

        log.info("Word export with template completed. Size: {} bytes", outputStream.size());
        return outputStream.toByteArray();
    }

    /**
     * Generate ENTERPRISE_STANDARD Word document programmatically
     */
    private byte[] generateEnterpriseReport(ReportData reportData) throws IOException {

        XWPFDocument document = new XWPFDocument();
        
        // Set page margins (2cm = 1134 twips)
        CTSectPr sectPr = document.getDocument().getBody().addNewSectPr();
        CTPageMar pageMar = sectPr.addNewPgMar();
        pageMar.setTop(BigInteger.valueOf(1134));
        pageMar.setBottom(BigInteger.valueOf(1134));
        pageMar.setLeft(BigInteger.valueOf(1134));
        pageMar.setRight(BigInteger.valueOf(1134));

        // ===== SECTION 1: HEADER =====
        createReportHeader(document, reportData);

        // ===== SECTION 2: METADATA =====
        createMetadataSection(document, reportData);

        // ===== SECTION 3: DATA TABLE =====
        createDataTable(document, reportData);

        // ===== SECTION 4: SUMMARY =====
        createSummarySection(document, reportData);

        // ===== SECTION 5: SIGNATURE BLOCK =====
        createSignatureBlock(document, reportData);

        // Write to output
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        document.write(outputStream);
        document.close();

        log.info("ENTERPRISE Word export completed. Size: {} bytes", outputStream.size());
        return outputStream.toByteArray();
    }

    /**
     * Create report header with title and code
     */
    private void createReportHeader(XWPFDocument document, ReportData reportData) {
        // Get organization settings
        OrganizationSettings org = reportTemplateService.getOrganizationSettings();
        
        // Company name
        XWPFParagraph companyPara = document.createParagraph();
        companyPara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun companyRun = companyPara.createRun();
        companyRun.setText(org.getCompanyName() != null ? org.getCompanyName() : "SCALEHUB IoT");
        companyRun.setBold(true);
        companyRun.setFontFamily(FONT_FAMILY);
        companyRun.setFontSize(14);
        
        // Empty line
        document.createParagraph();
        
        // Report Title
        XWPFParagraph titlePara = document.createParagraph();
        titlePara.setAlignment(ParagraphAlignment.CENTER);
        titlePara.setSpacingAfter(200);
        XWPFRun titleRun = titlePara.createRun();
        titleRun.setText(reportData.getReportTitle() != null ? reportData.getReportTitle() : "BÁO CÁO SẢN LƯỢNG TRẠM CÂN");
        titleRun.setBold(true);
        titleRun.setFontFamily(FONT_FAMILY);
        titleRun.setFontSize(TITLE_FONT_SIZE);
        
        // Report Code
        XWPFParagraph codePara = document.createParagraph();
        codePara.setAlignment(ParagraphAlignment.CENTER);
        codePara.setSpacingAfter(400);
        XWPFRun codeRun = codePara.createRun();
        codeRun.setText("Mã báo cáo: " + (reportData.getReportCode() != null ? reportData.getReportCode() : "BCSL"));
        codeRun.setBold(true);
        codeRun.setFontFamily(FONT_FAMILY);
        codeRun.setFontSize(12);
        
        // Horizontal line
        XWPFParagraph linePara = document.createParagraph();
        linePara.setBorderBottom(Borders.SINGLE);
        linePara.setSpacingAfter(200);
    }

    /**
     * Create metadata section with export info
     */
    private void createMetadataSection(XWPFDocument document, ReportData reportData) {
        // Create 2-column table for metadata (no visible borders)
        XWPFTable metaTable = document.createTable(4, 2);
        metaTable.setWidth("100%");
        
        // Remove borders
        CTTblPr tblPr = metaTable.getCTTbl().getTblPr();
        if (tblPr == null) tblPr = metaTable.getCTTbl().addNewTblPr();
        CTTblBorders borders = tblPr.addNewTblBorders();
        borders.addNewTop().setVal(STBorder.NONE);
        borders.addNewBottom().setVal(STBorder.NONE);
        borders.addNewLeft().setVal(STBorder.NONE);
        borders.addNewRight().setVal(STBorder.NONE);
        borders.addNewInsideH().setVal(STBorder.NONE);
        borders.addNewInsideV().setVal(STBorder.NONE);
        
        // Row 1: Export time & Date range
        setMetaCell(metaTable.getRow(0).getCell(0), "Thời gian xuất báo cáo:", 
                DATE_TIME_FORMATTER.format(reportData.getExportTime()));
        setMetaCell(metaTable.getRow(0).getCell(1), "Khoảng thời gian dữ liệu:", 
                DATE_FORMATTER.format(reportData.getStartTime()) + " - " + DATE_FORMATTER.format(reportData.getEndTime()));
        
        // Row 2: Prepared by & Scales
        String scaleNames = reportData.getRows().stream()
                .map(ReportData.ReportRow::getScaleName)
                .filter(name -> name != null && !name.isEmpty())
                .distinct()
                .collect(Collectors.joining(", "));
        setMetaCell(metaTable.getRow(1).getCell(0), "Người xuất báo cáo:", 
                reportData.getPreparedBy() != null ? reportData.getPreparedBy() : "System");
        setMetaCell(metaTable.getRow(1).getCell(1), "Danh sách trạm cân:", 
                scaleNames.isEmpty() ? "Tất cả" : scaleNames);
        
        // Row 3: Total scales & Total records
        ReportData.ReportSummary summary = reportData.getSummary();
        setMetaCell(metaTable.getRow(2).getCell(0), "Tổng số trạm cân:", 
                String.valueOf(summary.getTotalScales() != null ? summary.getTotalScales() : 0));
        setMetaCell(metaTable.getRow(2).getCell(1), "Tổng số lần cân:", 
                String.valueOf(summary.getTotalRecords() != null ? summary.getTotalRecords() : 0));
        
        // Row 4: Calculation method
        Object aggLabelObj = reportData.getMetadata() != null ? reportData.getMetadata().get("aggregationMethodLabel") : null;
        String aggLabel = aggLabelObj != null ? aggLabelObj.toString() : "";
        setMetaCell(metaTable.getRow(3).getCell(0), "Phương pháp tính:", aggLabel);
        setMetaCell(metaTable.getRow(3).getCell(1), "", "");
        
        // Add spacing
        document.createParagraph().setSpacingAfter(200);
    }
    
    private void setMetaCell(XWPFTableCell cell, String label, String value) {
        cell.removeParagraph(0);
        XWPFParagraph para = cell.addParagraph();
        
        if (!label.isEmpty()) {
            XWPFRun labelRun = para.createRun();
            labelRun.setText(label + " ");
            labelRun.setBold(true);
            labelRun.setFontFamily(FONT_FAMILY);
            labelRun.setFontSize(BODY_FONT_SIZE);
        }
        
        if (!value.isEmpty()) {
            XWPFRun valueRun = para.createRun();
            valueRun.setText(value);
            valueRun.setFontFamily(FONT_FAMILY);
            valueRun.setFontSize(BODY_FONT_SIZE);
        }
    }

    /**
     * Create main data table with header, data rows, and zebra striping
     */
    private void createDataTable(XWPFDocument document, ReportData reportData) {
        List<Integer> activeDataIndexes = getActiveDataIndexes(reportData);

        List<String> colNames = new ArrayList<>();
        colNames.add("STT");
        colNames.add("Mã trạm");
        colNames.add("Tên trạm");
        colNames.add("Vị trí");
        for (Integer idx : activeDataIndexes) {
            colNames.add(getDataName(reportData, idx));
        }
        colNames.add("Kỳ");

        int numRows = reportData.getRows().size() + 2; // +1 header, +1 summary
        int numCols = colNames.size();

        XWPFTable table = document.createTable(numRows, numCols);
        table.setWidth("100%");
        
        // Column widths (in twips, total ~9638 for A4 with 2cm margins)
        List<Integer> colWidths = new ArrayList<>();
        colWidths.add(600);
        colWidths.add(900);
        colWidths.add(1400);
        colWidths.add(1200);
        for (int i = 0; i < activeDataIndexes.size(); i++) {
            colWidths.add(900);
        }
        colWidths.add(800);
        
        // ===== HEADER ROW =====
        XWPFTableRow headerRow = table.getRow(0);
        for (int i = 0; i < numCols; i++) {
            XWPFTableCell cell = headerRow.getCell(i);
            setHeaderCellStyle(cell, colNames.get(i), colWidths.get(i));
        }
        
        // ===== DATA ROWS =====
        int rowIdx = 1;
        for (ReportData.ReportRow rowData : reportData.getRows()) {
            XWPFTableRow row = table.getRow(rowIdx);
            boolean isOddRow = (rowIdx % 2 == 1);

            int col = 0;
            setDataCell(row.getCell(col++), String.valueOf(rowData.getRowNumber()), isOddRow, true);
            setDataCell(row.getCell(col++), rowData.getScaleCode() != null ? rowData.getScaleCode() : "", isOddRow, true);
            setDataCell(row.getCell(col++), rowData.getScaleName() != null ? rowData.getScaleName() : "", isOddRow, false);
            setDataCell(row.getCell(col++), rowData.getLocation() != null ? rowData.getLocation() : "N/A", isOddRow, false);

            for (Integer idx : activeDataIndexes) {
                setDataCell(row.getCell(col++), formatNumber(getRowDataTotal(rowData, idx)), isOddRow, true);
            }

            setDataCell(row.getCell(col), rowData.getPeriod() != null ? rowData.getPeriod() : "", isOddRow, true);
            
            rowIdx++;
        }
        
        // ===== SUMMARY ROW (TỔNG CỘNG) =====
        XWPFTableRow summaryRow = table.getRow(rowIdx);
        ReportData.ReportSummary summary = reportData.getSummary();

        int col = 0;
        setSummaryCell(summaryRow.getCell(col++), "TỔNG CỘNG");
        setSummaryCell(summaryRow.getCell(col++), "");
        setSummaryCell(summaryRow.getCell(col++), "");
        setSummaryCell(summaryRow.getCell(col++), "");

        for (Integer idx : activeDataIndexes) {
            setSummaryCell(summaryRow.getCell(col++), formatNumber(getSummaryGrandTotal(summary, idx)));
        }
        setSummaryCell(summaryRow.getCell(col), "");
        
        // Add spacing after table
        document.createParagraph().setSpacingAfter(400);
    }

    private static List<Integer> getActiveDataIndexes(ReportData reportData) {
        List<Integer> indexes = new ArrayList<>();
        if (reportData == null) {
            return indexes;
        }
        if (hasText(reportData.getData1Name())) {
            indexes.add(1);
        }
        if (hasText(reportData.getData2Name())) {
            indexes.add(2);
        }
        if (hasText(reportData.getData3Name())) {
            indexes.add(3);
        }
        if (hasText(reportData.getData4Name())) {
            indexes.add(4);
        }
        if (hasText(reportData.getData5Name())) {
            indexes.add(5);
        }
        return indexes;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String getDataName(ReportData reportData, int idx) {
        return switch (idx) {
            case 1 -> reportData.getData1Name();
            case 2 -> reportData.getData2Name();
            case 3 -> reportData.getData3Name();
            case 4 -> reportData.getData4Name();
            case 5 -> reportData.getData5Name();
            default -> "";
        };
    }

    private static Double getRowDataTotal(ReportData.ReportRow row, int idx) {
        if (row == null) {
            return null;
        }
        return switch (idx) {
            case 1 -> row.getData1Total();
            case 2 -> row.getData2Total();
            case 3 -> row.getData3Total();
            case 4 -> row.getData4Total();
            case 5 -> row.getData5Total();
            default -> null;
        };
    }

    private static Double getSummaryGrandTotal(ReportData.ReportSummary summary, int idx) {
        if (summary == null) {
            return null;
        }
        return switch (idx) {
            case 1 -> summary.getData1GrandTotal();
            case 2 -> summary.getData2GrandTotal();
            case 3 -> summary.getData3GrandTotal();
            case 4 -> summary.getData4GrandTotal();
            case 5 -> summary.getData5GrandTotal();
            default -> null;
        };
    }

    private static Double getSummaryAverage(ReportData.ReportSummary summary, int idx) {
        if (summary == null) {
            return null;
        }
        return switch (idx) {
            case 1 -> summary.getData1Average();
            case 2 -> summary.getData2Average();
            case 3 -> summary.getData3Average();
            case 4 -> summary.getData4Average();
            case 5 -> summary.getData5Average();
            default -> null;
        };
    }

    private static Double getSummaryMax(ReportData.ReportSummary summary, int idx) {
        if (summary == null) {
            return null;
        }
        return switch (idx) {
            case 1 -> summary.getData1Max();
            case 2 -> summary.getData2Max();
            case 3 -> summary.getData3Max();
            case 4 -> summary.getData4Max();
            case 5 -> summary.getData5Max();
            default -> null;
        };
    }
    
    private void setHeaderCellStyle(XWPFTableCell cell, String text, int width) {
        // Background color
        CTTc ctTc = cell.getCTTc();
        CTTcPr tcPr = ctTc.addNewTcPr();
        CTShd shd = tcPr.addNewShd();
        shd.setFill(COLOR_HEADER_BG);
        
        // Width
        CTTblWidth tblWidth = tcPr.addNewTcW();
        tblWidth.setW(BigInteger.valueOf(width));
        tblWidth.setType(STTblWidth.DXA);
        
        // Text
        cell.removeParagraph(0);
        XWPFParagraph para = cell.addParagraph();
        para.setAlignment(ParagraphAlignment.CENTER);
        para.setSpacingBefore(60);
        para.setSpacingAfter(60);
        
        XWPFRun run = para.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontFamily(FONT_FAMILY);
        run.setFontSize(BODY_FONT_SIZE);
    }
    
    private void setDataCell(XWPFTableCell cell, String text, boolean isOddRow, boolean centerAlign) {
        // Zebra striping
        if (isOddRow) {
            CTTc ctTc = cell.getCTTc();
            CTTcPr tcPr = ctTc.isSetTcPr() ? ctTc.getTcPr() : ctTc.addNewTcPr();
            CTShd shd = tcPr.isSetShd() ? tcPr.getShd() : tcPr.addNewShd();
            shd.setFill(COLOR_ZEBRA_ODD);
        }
        
        // Text
        cell.removeParagraph(0);
        XWPFParagraph para = cell.addParagraph();
        para.setAlignment(centerAlign ? ParagraphAlignment.CENTER : ParagraphAlignment.LEFT);
        para.setSpacingBefore(40);
        para.setSpacingAfter(40);
        
        XWPFRun run = para.createRun();
        run.setText(text);
        run.setFontFamily(FONT_FAMILY);
        run.setFontSize(BODY_FONT_SIZE);
    }
    
    private void setSummaryCell(XWPFTableCell cell, String text) {
        CTTc ctTc = cell.getCTTc();
        CTTcPr tcPr = ctTc.isSetTcPr() ? ctTc.getTcPr() : ctTc.addNewTcPr();
        CTShd shd = tcPr.isSetShd() ? tcPr.getShd() : tcPr.addNewShd();
        shd.setFill(COLOR_SUMMARY_BG);
        
        cell.removeParagraph(0);
        XWPFParagraph para = cell.addParagraph();
        para.setAlignment(ParagraphAlignment.CENTER);
        para.setSpacingBefore(60);
        para.setSpacingAfter(60);
        
        XWPFRun run = para.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontFamily(FONT_FAMILY);
        run.setFontSize(BODY_FONT_SIZE);
    }

    /**
     * Create summary statistics section
     */
    private void createSummarySection(XWPFDocument document, ReportData reportData) {
        ReportData.ReportSummary summary = reportData.getSummary();

        List<Integer> activeDataIndexes = getActiveDataIndexes(reportData);
        if (activeDataIndexes.isEmpty()) {
            document.createParagraph().setSpacingAfter(400);
            return;
        }
        
        // Title
        XWPFParagraph titlePara = document.createParagraph();
        titlePara.setSpacingBefore(200);
        XWPFRun titleRun = titlePara.createRun();
        titleRun.setText("THỐNG KÊ TỔNG HỢP");
        titleRun.setBold(true);
        titleRun.setFontFamily(FONT_FAMILY);
        titleRun.setFontSize(HEADER_FONT_SIZE);
        
        // Statistics table
        XWPFTable statsTable = document.createTable(3, 1 + activeDataIndexes.size());
        statsTable.setWidth("100%");
        
        // Row 1: Labels
        setStatsHeaderCell(statsTable.getRow(0).getCell(0), "Thống kê");
        int colIdx = 1;
        for (Integer idx : activeDataIndexes) {
            setStatsHeaderCell(statsTable.getRow(0).getCell(colIdx++), getDataName(reportData, idx));
        }
        
        // Row 2: Averages
        setStatsCell(statsTable.getRow(1).getCell(0), "Trung bình", true);
        colIdx = 1;
        for (Integer idx : activeDataIndexes) {
            setStatsCell(statsTable.getRow(1).getCell(colIdx++), formatNumber(getSummaryAverage(summary, idx)), false);
        }
        
        // Row 3: Max
        setStatsCell(statsTable.getRow(2).getCell(0), "Giá trị lớn nhất", true);
        colIdx = 1;
        for (Integer idx : activeDataIndexes) {
            setStatsCell(statsTable.getRow(2).getCell(colIdx++), formatNumber(getSummaryMax(summary, idx)), false);
        }
        
        document.createParagraph().setSpacingAfter(400);
    }
    
    private void setStatsHeaderCell(XWPFTableCell cell, String text) {
        CTTc ctTc = cell.getCTTc();
        CTTcPr tcPr = ctTc.isSetTcPr() ? ctTc.getTcPr() : ctTc.addNewTcPr();
        CTShd shd = tcPr.isSetShd() ? tcPr.getShd() : tcPr.addNewShd();
        shd.setFill(COLOR_HEADER_BG);
        
        cell.removeParagraph(0);
        XWPFParagraph para = cell.addParagraph();
        para.setAlignment(ParagraphAlignment.CENTER);
        
        XWPFRun run = para.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontFamily(FONT_FAMILY);
        run.setFontSize(10);
    }
    
    private void setStatsCell(XWPFTableCell cell, String text, boolean bold) {
        cell.removeParagraph(0);
        XWPFParagraph para = cell.addParagraph();
        para.setAlignment(ParagraphAlignment.CENTER);
        
        XWPFRun run = para.createRun();
        run.setText(text);
        run.setBold(bold);
        run.setFontFamily(FONT_FAMILY);
        run.setFontSize(10);
    }

    /**
     * Create signature block with 3 positions
     */
    private void createSignatureBlock(XWPFDocument document, ReportData reportData) {
        // Spacing
        document.createParagraph();
        document.createParagraph();
        
        // Signature table - 4 rows, 3 columns
        XWPFTable sigTable = document.createTable(4, 3);
        sigTable.setWidth("100%");
        
        // Remove borders
        CTTblPr tblPr = sigTable.getCTTbl().getTblPr();
        if (tblPr == null) tblPr = sigTable.getCTTbl().addNewTblPr();
        CTTblBorders borders = tblPr.addNewTblBorders();
        borders.addNewTop().setVal(STBorder.NONE);
        borders.addNewBottom().setVal(STBorder.NONE);
        borders.addNewLeft().setVal(STBorder.NONE);
        borders.addNewRight().setVal(STBorder.NONE);
        borders.addNewInsideH().setVal(STBorder.NONE);
        borders.addNewInsideV().setVal(STBorder.NONE);
        
        // Row 0: Date
        String dateText = "Ngày " + java.time.LocalDate.now().getDayOfMonth() + 
                " tháng " + java.time.LocalDate.now().getMonthValue() + 
                " năm " + java.time.LocalDate.now().getYear();
        setSignatureCell(sigTable.getRow(0).getCell(0), "");
        setSignatureCell(sigTable.getRow(0).getCell(1), "");
        setSignatureCell(sigTable.getRow(0).getCell(2), dateText);
        
        // Row 1: Titles
        setSignatureTitleCell(sigTable.getRow(1).getCell(0), "NGƯỜI LẬP BIỂU");
        setSignatureTitleCell(sigTable.getRow(1).getCell(1), "TRƯỞNG BỘ PHẬN");
        setSignatureTitleCell(sigTable.getRow(1).getCell(2), "GIÁM ĐỐC");
        
        // Row 2: Signature hint
        setSignatureCell(sigTable.getRow(2).getCell(0), "(Ký, ghi rõ họ tên)");
        setSignatureCell(sigTable.getRow(2).getCell(1), "(Ký, ghi rõ họ tên)");
        setSignatureCell(sigTable.getRow(2).getCell(2), "(Ký, ghi rõ họ tên)");
        
        // Row 3: Empty for signature space + name placeholder
        String preparedBy = reportData.getPreparedBy() != null ? reportData.getPreparedBy() : "";
        setSignatureNameCell(sigTable.getRow(3).getCell(0), preparedBy);
        setSignatureNameCell(sigTable.getRow(3).getCell(1), "");
        setSignatureNameCell(sigTable.getRow(3).getCell(2), "");
        
        // Footer note
        // XWPFParagraph footerPara = document.createParagraph();
        // footerPara.setAlignment(ParagraphAlignment.CENTER);
        // footerPara.setSpacingBefore(600);
        // XWPFRun footerRun = footerPara.createRun();
        // footerRun.setText("--- Báo cáo nội bộ - ScaleHub IoT ---");
        // footerRun.setItalic(true);
        // footerRun.setFontFamily(FONT_FAMILY);
        // footerRun.setFontSize(9);
        // footerRun.setColor("808080");
    }
    
    private void setSignatureCell(XWPFTableCell cell, String text) {
        cell.removeParagraph(0);
        XWPFParagraph para = cell.addParagraph();
        para.setAlignment(ParagraphAlignment.CENTER);
        
        XWPFRun run = para.createRun();
        run.setText(text);
        run.setFontFamily(FONT_FAMILY);
        run.setFontSize(10);
        run.setItalic(true);
    }
    
    private void setSignatureTitleCell(XWPFTableCell cell, String text) {
        cell.removeParagraph(0);
        XWPFParagraph para = cell.addParagraph();
        para.setAlignment(ParagraphAlignment.CENTER);
        para.setSpacingBefore(200);
        
        XWPFRun run = para.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontFamily(FONT_FAMILY);
        run.setFontSize(BODY_FONT_SIZE);
    }
    
    private void setSignatureNameCell(XWPFTableCell cell, String text) {
        cell.removeParagraph(0);
        XWPFParagraph para = cell.addParagraph();
        para.setAlignment(ParagraphAlignment.CENTER);
        para.setSpacingBefore(800); // Space for signature
        
        XWPFRun run = para.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontFamily(FONT_FAMILY);
        run.setFontSize(BODY_FONT_SIZE);
    }

    /**
     * Prepare data model for POI-TL template
     */
    private Map<String, Object> prepareDataModel(ReportData reportData) {
        Map<String, Object> model = new HashMap<>();

        // Header
        model.put("reportTitle", reportData.getReportTitle());
        model.put("reportCode", reportData.getReportCode());
        
        // Metadata
        model.put("exportDateTime", DATE_TIME_FORMATTER.format(reportData.getExportTime()));
        model.put("preparedBy", reportData.getPreparedBy() != null ? reportData.getPreparedBy() : "System");
        model.put("startDate", DATE_FORMATTER.format(reportData.getStartTime()));
        model.put("endDate", DATE_FORMATTER.format(reportData.getEndTime()));
        
        // Column names
        model.put("data1Name", reportData.getData1Name());
        model.put("data2Name", reportData.getData2Name());
        model.put("data3Name", reportData.getData3Name());
        model.put("data4Name", reportData.getData4Name());
        model.put("data5Name", reportData.getData5Name());
        
        // Statistics
        ReportData.ReportSummary summary = reportData.getSummary();
        model.put("totalScales", summary.getTotalScales() != null ? summary.getTotalScales() : 0);
        model.put("totalRecords", summary.getTotalRecords() != null ? summary.getTotalRecords() : 0);
        
        // Scale names
        String scaleNames = reportData.getRows().stream()
                .map(ReportData.ReportRow::getScaleName)
                .filter(name -> name != null && !name.isEmpty())
                .distinct()
                .collect(Collectors.joining(", "));
        model.put("scaleNames", scaleNames.isEmpty() ? "Tất cả" : scaleNames);

        // Data rows
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ReportData.ReportRow row : reportData.getRows()) {
            Map<String, Object> rowMap = new HashMap<>();
            rowMap.put("rowNumber", row.getRowNumber());
            rowMap.put("scaleCode", row.getScaleCode() != null ? row.getScaleCode() : "");
            rowMap.put("scaleName", row.getScaleName() != null ? row.getScaleName() : "");
            rowMap.put("location", row.getLocation() != null ? row.getLocation() : "N/A");
            rowMap.put("data1Total", formatNumber(row.getData1Total()));
            rowMap.put("data2Total", formatNumber(row.getData2Total()));
            rowMap.put("data3Total", formatNumber(row.getData3Total()));
            rowMap.put("data4Total", formatNumber(row.getData4Total()));
            rowMap.put("data5Total", formatNumber(row.getData5Total()));
            rowMap.put("period", row.getPeriod() != null ? row.getPeriod() : "");
            // Back-compat with existing templates expecting recordCount column
            rowMap.put("recordCount", row.getPeriod() != null ? row.getPeriod() : "");
            rows.add(rowMap);
        }
        model.put("rows", rows);

        // Summary totals
        model.put("data1GrandTotal", formatNumber(summary.getData1GrandTotal()));
        model.put("data2GrandTotal", formatNumber(summary.getData2GrandTotal()));
        model.put("data3GrandTotal", formatNumber(summary.getData3GrandTotal()));
        model.put("data4GrandTotal", formatNumber(summary.getData4GrandTotal()));
        model.put("data5GrandTotal", formatNumber(summary.getData5GrandTotal()));
        
        // Summary averages
        model.put("data1Average", formatNumber(summary.getData1Average()));
        model.put("data2Average", formatNumber(summary.getData2Average()));
        model.put("data3Average", formatNumber(summary.getData3Average()));
        model.put("data4Average", formatNumber(summary.getData4Average()));
        model.put("data5Average", formatNumber(summary.getData5Average()));
        
        // Summary max
        model.put("data1Max", formatNumber(summary.getData1Max()));
        model.put("data2Max", formatNumber(summary.getData2Max()));
        model.put("data3Max", formatNumber(summary.getData3Max()));
        model.put("data4Max", formatNumber(summary.getData4Max()));
        model.put("data5Max", formatNumber(summary.getData5Max()));

        return model;
    }

    /**
     * Format number with thousand separator and 2 decimals (Vietnamese locale: 1.234,56)
     */
    private String formatNumber(Double value) {
        if (value == null) {
            return "0,00";
        }
        return NUMBER_FORMAT.format(ReportDataService.round(value, 2));
    }
}
