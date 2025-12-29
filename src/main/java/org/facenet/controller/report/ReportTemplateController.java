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

    @GetMapping("/word")
    public ResponseEntity<ApiResponse<List<ReportTemplateDto.WordTemplateResponse>>> listWordTemplates(
            @RequestParam(name = "activeOnly", defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(ApiResponse.success(reportTemplateService.listWordTemplates(activeOnly)));
    }

    @GetMapping("/word/{id}")
    public ResponseEntity<ApiResponse<ReportTemplateDto.WordTemplateResponse>> getWordTemplate(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(reportTemplateService.getWordTemplateResponse(id)));
    }

    @PostMapping("/word")
    public ResponseEntity<ApiResponse<ReportTemplateDto.WordTemplateResponse>> createWordTemplate(
            @Valid @RequestBody ReportTemplateDto.CreateWordTemplateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(reportTemplateService.createWordTemplate(request)));
    }

    @PutMapping("/word/{id}")
    public ResponseEntity<ApiResponse<ReportTemplateDto.WordTemplateResponse>> updateWordTemplate(
            @PathVariable("id") Long id,
            @Valid @RequestBody ReportTemplateDto.UpdateWordTemplateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(reportTemplateService.updateWordTemplate(id, request)));
    }

    @PostMapping("/word/{id}/set-default")
    public ResponseEntity<ApiResponse<ReportTemplateDto.WordTemplateResponse>> setDefaultWordTemplate(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(reportTemplateService.setDefaultWordTemplate(id)));
    }

    @PostMapping(value = "/word/{id}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ReportTemplateDto.WordTemplateResponse>> uploadWordTemplateFile(
            @PathVariable("id") Long id,
            @RequestPart("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(ApiResponse.success(reportTemplateService.uploadWordTemplateFile(id, file)));
    }

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

    @DeleteMapping("/word/{id}")
    public ResponseEntity<Void> deleteWordTemplate(@PathVariable("id") Long id) {
        reportTemplateService.deleteWordTemplate(id);
        return ResponseEntity.noContent().build();
    }
}
