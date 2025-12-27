package org.facenet.service.report;

import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.facenet.dto.report.ReportData;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

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

        // Render HTML
        String htmlContent = templateEngine.process("report-pdf", context);

        // Convert HTML to PDF
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ITextRenderer renderer = new ITextRenderer();
        
        try {
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(outputStream);
        } finally {
            renderer.finishPDF();
        }

        log.info("PDF export completed. Size: {} bytes", outputStream.size());
        return outputStream.toByteArray();
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
                return "0.00";
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
