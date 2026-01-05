package org.facenet.controller.report;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.facenet.common.response.ApiResponse;
import org.facenet.dto.report.ReportTemplateDto;
import org.facenet.service.report.ReportTemplateService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * APIs for configuring report templates (focus: WORD).
 * Base path is /api/v1 via server.servlet.context-path.
 */
@RestController
@RequestMapping("/report-templates")
@RequiredArgsConstructor
@Slf4j
public class ReportTemplateController {

    private final ReportTemplateService reportTemplateService;

    // ===== WORD templates =====
@Deprecated
    @GetMapping("/word")
    public ResponseEntity<ApiResponse<List<ReportTemplateDto.WordTemplateResponse>>> listWordTemplates(
            @RequestParam(name = "activeOnly", defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(ApiResponse.success(reportTemplateService.listWordTemplates(activeOnly)));
    }

    @Deprecated
    @GetMapping("/word/{id}")
    public ResponseEntity<ApiResponse<ReportTemplateDto.WordTemplateResponse>> getWordTemplate(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(reportTemplateService.getWordTemplateResponse(id)));
    }

    @Deprecated
    @PostMapping("/word")
    public ResponseEntity<ApiResponse<ReportTemplateDto.WordTemplateResponse>> createWordTemplate(
            @Valid @RequestBody ReportTemplateDto.CreateWordTemplateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(reportTemplateService.createWordTemplate(request)));
    }

    @Deprecated
    @PutMapping("/word/{id}")
    public ResponseEntity<ApiResponse<ReportTemplateDto.WordTemplateResponse>> updateWordTemplate(
            @PathVariable("id") Long id,
            @Valid @RequestBody ReportTemplateDto.UpdateWordTemplateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(reportTemplateService.updateWordTemplate(id, request)));
    }

    @Deprecated
    @PostMapping("/word/{id}/set-default")
    public ResponseEntity<ApiResponse<ReportTemplateDto.WordTemplateResponse>> setDefaultWordTemplate(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(reportTemplateService.setDefaultWordTemplate(id)));
    }

    @Deprecated
    @PostMapping(value = "/word/{id}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ReportTemplateDto.WordTemplateResponse>> uploadWordTemplateFile(
            @PathVariable("id") Long id,
            @RequestPart("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(ApiResponse.success(reportTemplateService.uploadWordTemplateFile(id, file)));
    }

    @Deprecated
    @GetMapping("/word/{id}/file")
    public ResponseEntity<byte[]> downloadWordTemplateFile(@PathVariable("id") Long id) {
        var file = reportTemplateService.getWordTemplateFile(id);
        String filename = file.filename() != null ? file.filename() : "template.docx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .contentLength(file.content().length)
                .body(file.content());
    }

    @Deprecated
    @DeleteMapping("/word/{id}")
    public ResponseEntity<Void> deleteWordTemplate(@PathVariable("id") Long id) {
        reportTemplateService.deleteWordTemplate(id);
        return ResponseEntity.noContent().build();
    }

    // ===== TEMPLATE IMPORT OPERATIONS =====

    /**
     * Import template file from resources
     * POST /api/v1/report-templates/import
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ReportTemplateDto.TemplateImportResponse>> importTemplate(
            @RequestPart("file") MultipartFile file,
            @RequestParam("templateCode") String templateCode,
            @RequestParam("templateName") String templateName,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "titleTemplate", required = false) String titleTemplate,
            @RequestParam(value = "importNotes", required = false) String importNotes,
            @RequestParam(value = "isActive", defaultValue = "true") Boolean isActive) throws IOException {

        ReportTemplateDto.TemplateImportRequest request = ReportTemplateDto.TemplateImportRequest.builder()
                .templateCode(templateCode)
                .templateName(templateName)
                .description(description)
                .titleTemplate(titleTemplate)
                .importNotes(importNotes)
                .isActive(isActive)
                .build();

        ReportTemplateDto.TemplateImportResponse response = reportTemplateService.importTemplateFile(request, file);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * List all imported templates
     * GET /api/v1/report-templates/imports
     */
    @Deprecated
    @GetMapping("/imports")
    public ResponseEntity<ApiResponse<List<ReportTemplateDto.TemplateImportListResponse>>> listImportedTemplates() {
        List<ReportTemplateDto.TemplateImportListResponse> imports = reportTemplateService.listImportedTemplates();
        return ResponseEntity.ok(ApiResponse.success(imports));
    }

    /**
     * Get details of imported template
     * GET /api/v1/report-templates/imports/{importId}
     */
    @GetMapping("/imports/{importId}")
    public ResponseEntity<ApiResponse<ReportTemplateDto.ImportedTemplateDetailsResponse>> getImportedTemplateDetails(
            @PathVariable("importId") Long importId) {
        ReportTemplateDto.ImportedTemplateDetailsResponse details = reportTemplateService.getImportedTemplateDetails(importId);
        return ResponseEntity.ok(ApiResponse.success(details));
    }

    /**
     * Get import by template code
     * GET /api/v1/report-templates/imports/by-code/{templateCode}
     */
    @Deprecated
    @GetMapping("/imports/by-code/{templateCode}")
    public ResponseEntity<ApiResponse<List<ReportTemplateDto.TemplateImportListResponse>>> getImportsByCode(
            @PathVariable("templateCode") String templateCode) {
        List<ReportTemplateDto.TemplateImportListResponse> imports = reportTemplateService.getImportsByTemplateCode(templateCode);
        return ResponseEntity.ok(ApiResponse.success(imports));
    }

    /**
     * Get import by template ID
     * GET /api/v1/report-templates/imports/by-template/{templateId}
     */
    @Deprecated
    @GetMapping("/imports/by-template/{templateId}")
    public ResponseEntity<ApiResponse<ReportTemplateDto.TemplateImportResponse>> getImportByTemplateId(
            @PathVariable("templateId") Long templateId) {
        ReportTemplateDto.TemplateImportResponse import_ = reportTemplateService.getImportByTemplateId(templateId);
        return ResponseEntity.ok(ApiResponse.success(import_));
    }

    /**
     * Download imported template file
     * GET /api/v1/report-templates/imports/{importId}/download
     */
    @GetMapping("/imports/{importId}/download")
    public ResponseEntity<byte[]> downloadImportedTemplate(@PathVariable("importId") Long importId) throws IOException {
        ReportTemplateService.TemplateFileRecord file = reportTemplateService.downloadImportedTemplate(importId);
        String filename = file.filename() != null ? file.filename() : "template.docx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(file.content().length)
                .body(file.content());
    }

    /**
     * Archive imported template (soft delete)
     * POST /api/v1/report-templates/imports/{importId}/archive
     */
    @PostMapping("/imports/{importId}/archive")
    public ResponseEntity<ApiResponse<ReportTemplateDto.TemplateImportResponse>> archiveImportedTemplate(
            @PathVariable("importId") Long importId) {
        ReportTemplateDto.TemplateImportResponse response = reportTemplateService.archiveImportedTemplate(importId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Delete imported template permanently
     * DELETE /api/v1/report-templates/imports/{importId}
     */
    @Deprecated
    @DeleteMapping("/imports/{importId}")
    public ResponseEntity<Void> deleteImportedTemplate(@PathVariable("importId") Long importId) {
        reportTemplateService.deleteImportedTemplate(importId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Verify template file integrity
     * GET /api/v1/report-templates/imports/{importId}/verify
     */
    @Deprecated
    @GetMapping("/imports/{importId}/verify")
    public ResponseEntity<ApiResponse<Boolean>> verifyTemplateFileIntegrity(@PathVariable("importId") Long importId) throws IOException {
        boolean isValid = reportTemplateService.verifyTemplateFileIntegrity(importId);
        return ResponseEntity.ok(ApiResponse.success(isValid));
    }
}
