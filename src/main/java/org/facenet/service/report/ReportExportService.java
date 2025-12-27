package org.facenet.service.report;

import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.facenet.dto.report.ReportData;
import org.facenet.dto.report.ReportExportRequest;
import org.facenet.entity.report.ReportTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

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
                request.getStartTime(),
                request.getEndTime(),
                request.getScaleIds(),
                request.getPreparedBy()
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
