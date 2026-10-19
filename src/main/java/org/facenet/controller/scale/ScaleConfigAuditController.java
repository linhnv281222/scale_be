package org.facenet.controller.scale;

import lombok.RequiredArgsConstructor;
import org.facenet.common.response.ApiResponse;
import org.facenet.entity.scale.ScaleConfigAuditLog;
import org.facenet.service.scale.ScaleConfigAuditService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/scales/{scaleId}/config-audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasAuthority('AUDIT_VIEW')")
public class ScaleConfigAuditController {
    private final ScaleConfigAuditService auditService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ScaleConfigAuditLog>>> history(
            @PathVariable Long scaleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.success(auditService.history(scaleId, page, size)));
    }
}
