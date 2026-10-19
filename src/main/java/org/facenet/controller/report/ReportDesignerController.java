package org.facenet.controller.report;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.facenet.common.response.ApiResponse;
import org.facenet.dto.report.ReportDesignerDto;
import org.facenet.service.report.ReportDesignerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/reports/designer")
@RequiredArgsConstructor
@Tag(name = "Report Designer", description = "Template mapping, detailed report queries and report charts")
@PreAuthorize("hasRole('ADMIN') or hasAuthority('REPORT_VIEW')")
public class ReportDesignerController {

    private final ReportDesignerService reportDesignerService;

    @GetMapping("/templates/{importId}/analysis")
    @Operation(summary = "Analyze Word/Excel placeholders and workbook layout")
    public ResponseEntity<ApiResponse<ReportDesignerDto.TemplateAnalysis>> analyze(
            @PathVariable Long importId) throws IOException {
        return ResponseEntity.ok(ApiResponse.success(reportDesignerService.analyzeTemplate(importId)));
    }

    @GetMapping("/templates/{importId}/mapping")
    @Operation(summary = "Get saved placeholder and Excel grid mapping")
    public ResponseEntity<ApiResponse<ReportDesignerDto.TemplateMapping>> getMapping(@PathVariable Long importId) {
        return ResponseEntity.ok(ApiResponse.success(reportDesignerService.getMapping(importId)));
    }

    @PutMapping("/templates/{importId}/mapping")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('CONFIG_MANAGE')")
    @Operation(summary = "Save placeholder aliases and Excel row/column mapping; indexes are zero-based")
    public ResponseEntity<ApiResponse<ReportDesignerDto.TemplateMapping>> saveMapping(
            @PathVariable Long importId, @Valid @RequestBody ReportDesignerDto.TemplateMappingRequest request) {
        return ResponseEntity.ok(ApiResponse.success(reportDesignerService.saveMapping(importId, request),
                "Report template mapping saved"));
    }

    @PostMapping("/detail-query")
    @Operation(summary = "Query detailed scale rows with filters, pagination and row sampling")
    public ResponseEntity<ApiResponse<ReportDesignerDto.DetailQueryResponse>> detail(
            @Valid @RequestBody ReportDesignerDto.DetailQueryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(reportDesignerService.queryDetail(request)));
    }

    @PostMapping("/chart")
    @Operation(summary = "Convert filtered report data to chart series")
    public ResponseEntity<ApiResponse<ReportDesignerDto.ChartResponse>> chart(
            @Valid @RequestBody ReportDesignerDto.ChartRequest request) {
        return ResponseEntity.ok(ApiResponse.success(reportDesignerService.queryChart(request)));
    }
}
