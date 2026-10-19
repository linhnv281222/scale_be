package org.facenet.controller.dashboard;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.facenet.common.response.ApiResponse;
import org.facenet.dto.dashboard.DashboardDto;
import org.facenet.service.dashboard.DashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Operations Dashboard", description = "Production KPI, scale status and multi-scale trends")
@PreAuthorize("hasRole('ADMIN') or hasAuthority('DASHBOARD_VIEW')")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    @Operation(summary = "Get day/week/month production KPI, shift production and online status")
    public ResponseEntity<ApiResponse<DashboardDto.Overview>> overview(
            @RequestParam(value = "location_id", required = false) Long locationId,
            @RequestParam(value = "total_field", defaultValue = "data_1") String totalField,
            @RequestParam(value = "timezone", defaultValue = "Asia/Bangkok") String timezone) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getOverview(locationId, totalField, timezone)));
    }

    @GetMapping("/scales")
    @Operation(summary = "Get the operational dashboard row for every active scale")
    public ResponseEntity<ApiResponse<DashboardDto.ScaleTable>> scales(
            @RequestParam(value = "location_id", required = false) Long locationId,
            @RequestParam(value = "total_field", defaultValue = "data_1") String totalField,
            @RequestParam(value = "flow_field", defaultValue = "data_2") String flowField,
            @RequestParam(value = "timezone", defaultValue = "Asia/Bangkok") String timezone) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getScaleTable(locationId, totalField, flowField, timezone)));
    }

    @GetMapping("/trends")
    @Operation(summary = "Get bucketed multi-scale flow trend data")
    public ResponseEntity<ApiResponse<DashboardDto.Trend>> trends(
            @RequestParam(value = "scale_ids", required = false) List<Long> scaleIds,
            @RequestParam(value = "location_id", required = false) Long locationId,
            @RequestParam(value = "flow_field", defaultValue = "data_2") String flowField,
            @RequestParam(value = "period", defaultValue = "HOUR") String period,
            @RequestParam(value = "bucket", defaultValue = "AUTO") String bucket,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(value = "timezone", defaultValue = "Asia/Bangkok") String timezone) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getTrend(
                scaleIds, locationId, flowField, period, bucket, from, to, timezone)));
    }
}
