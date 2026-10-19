package org.facenet.service.report;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.facenet.dto.report.ReportDesignerDto;
import org.facenet.entity.report.ReportTemplate;
import org.facenet.entity.report.TemplateImport;
import org.facenet.repository.report.ReportTemplateRepository;
import org.facenet.repository.report.TemplateImportRepository;
import org.facenet.util.TemplateFileUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportDesignerServiceTest {

    private final TemplateImportRepository importRepository = mock(TemplateImportRepository.class);
    private final ReportTemplateRepository templateRepository = mock(ReportTemplateRepository.class);
    private final TemplateFileUtil fileUtil = mock(TemplateFileUtil.class);
    private final NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
    private ReportDesignerService service;
    private ReportTemplate template;

    @BeforeEach
    void setUp() {
        service = new ReportDesignerService(importRepository, templateRepository, fileUtil, jdbcTemplate);
        template = ReportTemplate.builder().id(9L).code("P3").name("P3")
                .reportType(ReportTemplate.ReportType.EXCEL).build();
        TemplateImport imported = TemplateImport.builder().id(7L).template(template)
                .originalFilename("report.xlsx").resourcePath("templates/report.xlsx")
                .filePath("report.xlsx").build();
        when(importRepository.findById(7L)).thenReturn(Optional.of(imported));
    }

    @Test
    void analyzesExcelSheetsAndBothPlaceholderStyles() throws Exception {
        byte[] workbookBytes;
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Bao cao");
            sheet.createRow(2).createCell(1).setCellValue("[Thông số 1]");
            sheet.createRow(3).createCell(2).setCellValue("{{scaleName}}");
            workbook.write(output);
            workbookBytes = output.toByteArray();
        }
        when(fileUtil.readTemplateFile("templates/report.xlsx")).thenReturn(workbookBytes);

        ReportDesignerDto.TemplateAnalysis result = service.analyzeTemplate(7L);

        assertEquals("EXCEL", result.getFileType());
        assertEquals(1, result.getSheets().size());
        assertEquals("Bao cao", result.getSheets().get(0).getName());
        assertTrue(result.getPlaceholders().contains("Thông số 1"));
        assertTrue(result.getPlaceholders().contains("scaleName"));
        assertEquals(2, result.getSheets().get(0).getPlaceholders().size());
    }

    @Test
    void savesNormalizedPlaceholderAndExcelGridMapping() {
        ReportDesignerDto.TemplateMappingRequest request = ReportDesignerDto.TemplateMappingRequest.builder()
                .placeholderMappings(Map.of("[Tên cân]", "scaleName", "{{Bắt đầu}}", "startTime"))
                .sheetName("Bao cao").headerRow(4).dataStartRow(5).rowOffset(1).columnOffset(2)
                .columnMappings(Map.of("0", "rowNumber", "1", "scaleName")).build();

        ReportDesignerDto.TemplateMapping result = service.saveMapping(7L, request);

        assertEquals("scaleName", result.getPlaceholderMappings().get("Tên cân"));
        assertEquals("startTime", result.getPlaceholderMappings().get("Bắt đầu"));
        assertEquals(6, result.getDataStartRow() + result.getRowOffset());
        assertEquals("scaleName", ReportDesignerService.readMapping(template)
                .getColumnMappings().get("1"));
    }
}
