package org.facenet.service.report;

import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.facenet.dto.report.ReportData;
import org.facenet.entity.report.ReportColumn;
import org.facenet.entity.report.ReportTemplate;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.lowagie.text.pdf.BaseFont;

/**
 * Service for exporting reports to PDF format
 * Uses Thymeleaf for HTML template + Flying Saucer for PDF rendering
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PdfExportService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private final TemplateEngine templateEngine;

    public record PdfColumn(String key, String label, String cssClass) {}

    public record PdfRow(Map<String, String> cells) {}

    public record PdfSummaryRow(String c1, String c2, String c3, String c4) {}

    /**
     * Export report data to PDF
     */
    public byte[] exportToPdf(ReportData reportData) throws IOException, DocumentException {
        log.info("Exporting report to PDF: {}", reportData.getReportTitle());

        // Prepare Thymeleaf context
        Context context = new Context();
        context.setVariable("report", reportData);
        context.setVariable("dateTimeFormatter", DATE_TIME_FORMATTER);
        context.setVariable("dataFormatter", new DataFormatter());

        // Build dynamic table model from selected template (to match Word/Excel behavior)
        var table = buildPdfTableModel(reportData);
        context.setVariable("pdfColumns", table.columns());
        context.setVariable("pdfRows", table.rows());

        // Build dynamic summary rows (totals/avg/max) for visible data columns only
        var summary = buildPdfSummaryModel(reportData, table.columns());
        context.setVariable("pdfSummaryRows", summary.summaryRows());
        context.setVariable("pdfMaxRows", summary.maxRows());

        // Render HTML
        String htmlContent = templateEngine.process("report-pdf", context);

        // Convert HTML to PDF
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ITextRenderer renderer = new ITextRenderer();
        
        try {
            // Register Unicode fonts if available (important for Vietnamese)
            registerUnicodeFonts(renderer);

            // Provide a base URL so relative resources (if any) can resolve
            String baseUrl = getClass().getClassLoader().getResource("templates/") != null
                    ? getClass().getClassLoader().getResource("templates/").toExternalForm()
                    : null;

            if (baseUrl != null) {
                renderer.setDocumentFromString(htmlContent, baseUrl);
            } else {
                renderer.setDocumentFromString(htmlContent);
            }
            renderer.layout();
            renderer.createPDF(outputStream);
        } finally {
            renderer.finishPDF();
        }

        log.info("PDF export completed. Size: {} bytes", outputStream.size());
        return outputStream.toByteArray();
    }

    private record PdfTableModel(List<PdfColumn> columns, List<PdfRow> rows) {}

    private record PdfSummaryModel(List<PdfSummaryRow> summaryRows, List<PdfSummaryRow> maxRows) {}

    private PdfTableModel buildPdfTableModel(ReportData reportData) {
        if (reportData == null) {
            return new PdfTableModel(List.of(), List.of());
        }

        ReportTemplate template = null;
        if (reportData.getMetadata() != null) {
            Object t = reportData.getMetadata().get("template");
            if (t instanceof ReportTemplate rt) {
                template = rt;
            }
        }

        List<ReportColumn> visibleColumns = template != null && template.getColumns() != null
                ? getVisibleColumns(template, reportData)
                : List.of();

        // Fallback to a safe default if template is missing
        if (visibleColumns.isEmpty()) {
            visibleColumns = defaultPdfColumns();
        }

        List<PdfColumn> pdfColumns = new ArrayList<>(visibleColumns.size());
        for (ReportColumn c : visibleColumns) {
            String key = normalizeDataField(c);
            String label = resolveColumnLabel(c, reportData);
            String cssClass = resolveCssClass(c, key);
            pdfColumns.add(new PdfColumn(key, label, cssClass));
        }

        List<PdfRow> pdfRows = new ArrayList<>(reportData.getRows() != null ? reportData.getRows().size() : 0);
        if (reportData.getRows() != null) {
            DataFormatter formatter = new DataFormatter();
            for (ReportData.ReportRow row : reportData.getRows()) {
                Map<String, String> cells = new HashMap<>();
                for (PdfColumn col : pdfColumns) {
                    cells.put(col.key(), formatCellValue(row, col.key(), formatter));
                }
                pdfRows.add(new PdfRow(cells));
            }
        }

        return new PdfTableModel(pdfColumns, pdfRows);
    }

    private PdfSummaryModel buildPdfSummaryModel(ReportData reportData, List<PdfColumn> columns) {
        if (reportData == null || reportData.getSummary() == null || columns == null || columns.isEmpty()) {
            return new PdfSummaryModel(List.of(), List.of());
        }

        // Determine visible data fields in display order
        List<Integer> visibleDataIndices = new ArrayList<>();
        for (PdfColumn col : columns) {
            Integer idx = dataIndexFromKey(col != null ? col.key() : null);
            if (idx != null && !visibleDataIndices.contains(idx)) {
                visibleDataIndices.add(idx);
            }
        }

        DataFormatter formatter = new DataFormatter();
        ReportData.ReportSummary s = reportData.getSummary();

        List<PdfSummaryRow> summaryRows = new ArrayList<>();
        for (Integer idx : visibleDataIndices) {
            String name = dataNameByIndex(reportData, idx);
            if (!hasText(name)) {
                continue;
            }
            Double total = dataGrandTotalByIndex(s, idx);
            Double avg = dataAverageByIndex(s, idx);
            summaryRows.add(new PdfSummaryRow(
                    "Tổng " + name + ":",
                    formatter.formatNumber(total),
                    "Trung bình " + name + ":",
                    formatter.formatNumber(avg)
            ));
        }

        List<PdfSummaryRow> maxRows = new ArrayList<>();
        for (Integer idx : visibleDataIndices) {
            String name = dataNameByIndex(reportData, idx);
            if (!hasText(name)) {
                continue;
            }
            Double max = dataMaxByIndex(s, idx);
            maxRows.add(new PdfSummaryRow(
                    "Giá trị lớn nhất " + name + ":",
                    formatter.formatNumber(max),
                    "",
                    ""
            ));
        }

        return new PdfSummaryModel(summaryRows, maxRows);
    }

    private static Integer dataIndexFromKey(String key) {
        if (key == null) {
            return null;
        }
        return switch (key) {
            case "data_1", "data1Total" -> 1;
            case "data_2", "data2Total" -> 2;
            case "data_3", "data3Total" -> 3;
            case "data_4", "data4Total" -> 4;
            case "data_5", "data5Total" -> 5;
            default -> null;
        };
    }

    private static String dataNameByIndex(ReportData reportData, int idx) {
        return switch (idx) {
            case 1 -> reportData.getData1Name();
            case 2 -> reportData.getData2Name();
            case 3 -> reportData.getData3Name();
            case 4 -> reportData.getData4Name();
            case 5 -> reportData.getData5Name();
            default -> null;
        };
    }

    private static Double dataGrandTotalByIndex(ReportData.ReportSummary s, int idx) {
        return switch (idx) {
            case 1 -> s.getData1GrandTotal();
            case 2 -> s.getData2GrandTotal();
            case 3 -> s.getData3GrandTotal();
            case 4 -> s.getData4GrandTotal();
            case 5 -> s.getData5GrandTotal();
            default -> null;
        };
    }

    private static Double dataAverageByIndex(ReportData.ReportSummary s, int idx) {
        return switch (idx) {
            case 1 -> s.getData1Average();
            case 2 -> s.getData2Average();
            case 3 -> s.getData3Average();
            case 4 -> s.getData4Average();
            case 5 -> s.getData5Average();
            default -> null;
        };
    }

    private static Double dataMaxByIndex(ReportData.ReportSummary s, int idx) {
        return switch (idx) {
            case 1 -> s.getData1Max();
            case 2 -> s.getData2Max();
            case 3 -> s.getData3Max();
            case 4 -> s.getData4Max();
            case 5 -> s.getData5Max();
            default -> null;
        };
    }

    private static List<ReportColumn> defaultPdfColumns() {
        // Minimal fallback: match legacy PDF layout
        return List.of(
                ReportColumn.builder().columnOrder(1).columnKey("row_number").columnLabel("STT").dataField("row_number").build(),
                ReportColumn.builder().columnOrder(2).columnKey("scale_code").columnLabel("Mã trạm").dataField("scale_code").build(),
                ReportColumn.builder().columnOrder(3).columnKey("scale_name").columnLabel("Tên trạm").dataField("scale_name").build(),
                ReportColumn.builder().columnOrder(4).columnKey("location").columnLabel("Vị trí").dataField("location").build(),
                ReportColumn.builder().columnOrder(5).columnKey("data_1").columnLabel("Data 1").dataField("data_1").build(),
                ReportColumn.builder().columnOrder(6).columnKey("data_2").columnLabel("Data 2").dataField("data_2").build(),
                ReportColumn.builder().columnOrder(7).columnKey("data_3").columnLabel("Data 3").dataField("data_3").build(),
                ReportColumn.builder().columnOrder(8).columnKey("data_4").columnLabel("Data 4").dataField("data_4").build(),
                ReportColumn.builder().columnOrder(9).columnKey("data_5").columnLabel("Data 5").dataField("data_5").build(),
                ReportColumn.builder().columnOrder(10).columnKey("record_count").columnLabel("Kỳ").dataField("record_count").build(),
                ReportColumn.builder().columnOrder(11).columnKey("last_time").columnLabel("Thời gian cuối").dataField("last_time").build()
        );
    }

    private static List<ReportColumn> getVisibleColumns(ReportTemplate template, ReportData reportData) {
        List<ReportColumn> sorted = new ArrayList<>(template.getColumns());
        sorted.sort(Comparator.comparingInt(c -> c.getColumnOrder() != null ? c.getColumnOrder() : 0));

        List<ReportColumn> visible = new ArrayList<>(sorted.size());
        for (ReportColumn column : sorted) {
            if (column == null) {
                continue;
            }
            if (Boolean.FALSE.equals(column.getIsVisible())) {
                continue;
            }
            String dataField = normalizeDataField(column);
            if (!hasText(dataField)) {
                continue;
            }
            if (Objects.equals(dataField, "data_1") && !hasText(reportData.getData1Name())) {
                continue;
            }
            if (Objects.equals(dataField, "data_2") && !hasText(reportData.getData2Name())) {
                continue;
            }
            if (Objects.equals(dataField, "data_3") && !hasText(reportData.getData3Name())) {
                continue;
            }
            if (Objects.equals(dataField, "data_4") && !hasText(reportData.getData4Name())) {
                continue;
            }
            if (Objects.equals(dataField, "data_5") && !hasText(reportData.getData5Name())) {
                continue;
            }
            visible.add(column);
        }
        return visible;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String normalizeDataField(ReportColumn column) {
        if (column == null) {
            return null;
        }
        String dataField = column.getDataField();
        if (hasText(dataField)) {
            return dataField.trim();
        }
        String columnKey = column.getColumnKey();
        return hasText(columnKey) ? columnKey.trim() : null;
    }

    private static String resolveColumnLabel(ReportColumn column, ReportData reportData) {
        String dataField = normalizeDataField(column);

        if (Objects.equals(dataField, "record_count") || Objects.equals(dataField, "recordCount")) {
            return "Kỳ";
        }

        if (Objects.equals(dataField, "data_1") || Objects.equals(dataField, "data1Total")) {
            return reportData.getData1Name();
        }
        if (Objects.equals(dataField, "data_2") || Objects.equals(dataField, "data2Total")) {
            return reportData.getData2Name();
        }
        if (Objects.equals(dataField, "data_3") || Objects.equals(dataField, "data3Total")) {
            return reportData.getData3Name();
        }
        if (Objects.equals(dataField, "data_4") || Objects.equals(dataField, "data4Total")) {
            return reportData.getData4Name();
        }
        if (Objects.equals(dataField, "data_5") || Objects.equals(dataField, "data5Total")) {
            return reportData.getData5Name();
        }

        return column != null && column.getColumnLabel() != null ? column.getColumnLabel() : "";
    }

    private static String resolveCssClass(ReportColumn column, String dataField) {
        if (column != null && column.getAlignment() != null) {
            return switch (column.getAlignment()) {
                case LEFT -> "text-left";
                case RIGHT -> "number";
                default -> "";
            };
        }
        if (dataField == null) {
            return "";
        }
        if (dataField.startsWith("data_") || dataField.endsWith("Total")) {
            return "number";
        }
        return switch (dataField) {
            case "scale_name", "scaleName", "location", "scaleCode", "code" -> "text-left";
            default -> "";
        };
    }

    private static String formatCellValue(ReportData.ReportRow row, String dataField, DataFormatter formatter) {
        if (row == null || dataField == null) {
            return "";
        }

        Object value = switch (dataField) {
            case "row_number", "rowNumber" -> row.getRowNumber();
            case "scale_code", "scaleCode", "code" -> row.getScaleCode();
            case "scale_name", "scaleName", "name" -> row.getScaleName();
            case "location" -> row.getLocation();
            case "data_1", "data1Total" -> row.getData1Total();
            case "data_2", "data2Total" -> row.getData2Total();
            case "data_3", "data3Total" -> row.getData3Total();
            case "data_4", "data4Total" -> row.getData4Total();
            case "data_5", "data5Total" -> row.getData5Total();
            case "record_count", "recordCount" -> row.getPeriod();
            case "last_time", "lastTime" -> row.getLastTime();
            default -> "";
        };

        if (value == null) {
            return "";
        }
        if (value instanceof Number n) {
            return formatter.formatNumber(n.doubleValue());
        }
        if (value instanceof java.time.OffsetDateTime dt) {
            return formatter.formatDateTime(dt);
        }
        return String.valueOf(value);
    }

    private void registerUnicodeFonts(ITextRenderer renderer) {
        // Common font paths in containers / Linux + Windows.
        List<String> candidates = List.of(
                "/usr/share/fonts/ttf-dejavu/DejaVuSans.ttf",
                "/usr/share/fonts/ttf-dejavu/DejaVuSans-Bold.ttf",
                "/usr/share/fonts/dejavu/DejaVuSans.ttf",
                "/usr/share/fonts/dejavu/DejaVuSans-Bold.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",

            "C:/Windows/Fonts/arial.ttf",
            "C:/Windows/Fonts/arialbd.ttf",
            "C:/Windows/Fonts/times.ttf",
            "C:/Windows/Fonts/timesbd.ttf",
            "C:/Windows/Fonts/calibri.ttf",
            "C:/Windows/Fonts/calibrib.ttf"
        );

        for (String pathStr : candidates) {
            try {
                Path path = Path.of(pathStr);
                if (!Files.exists(path)) {
                    continue;
                }
                renderer.getFontResolver().addFont(path.toAbsolutePath().toString(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } catch (Exception e) {
                log.debug("Unable to register font at {}: {}", pathStr, e.getMessage());
            }
        }
    }

    /**
     * Create Thymeleaf template engine if not provided by Spring
     */
    public static TemplateEngine createTemplateEngine() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setOrder(1);

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
        return templateEngine;
    }

    /**
     * Helper class for formatting data in templates
     */
    public static class DataFormatter {
        public String formatNumber(Double value) {
            if (value == null) {
                return "";
            }
            return String.format("%,.2f", ReportDataService.round(value, 2));
        }

        public String formatDateTime(java.time.OffsetDateTime dateTime) {
            if (dateTime == null) {
                return "N/A";
            }
            return DATE_TIME_FORMATTER.format(dateTime);
        }
    }
}
