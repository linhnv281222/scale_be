package org.facenet.service.scale.report;

import org.facenet.dto.scale.ReportRequestDto;
import org.facenet.dto.scale.ReportResponseDto;
import org.facenet.dto.scale.IntervalReportRequestDto;
import org.facenet.dto.scale.IntervalReportResponseDto;
import org.facenet.dto.scale.IntervalReportRequestDtoV2;
import org.facenet.dto.scale.IntervalReportResponseDtoV2;

/**
 * Service for generating reports and statistics
 */
public interface ReportService {

    /**
     * Generate report based on request parameters
     *
     * @param request report request with filters and parameters
     * @return report response with data points
     */
    ReportResponseDto generateReport(ReportRequestDto request);

    /**
     * Generate interval statistics report (SHIFT/HOUR/DAY/WEEK) with per-field aggregation.
     */
    org.facenet.common.pagination.PageResponseDto<IntervalReportResponseDto.Row> generateIntervalReport(IntervalReportRequestDto request);

    /**
     * Generate interval statistics report V2 with start/end values, ratio calculation, and overview.
     */
    org.facenet.common.pagination.PageResponseDto<IntervalReportResponseDtoV2> generateIntervalReportV2(IntervalReportRequestDtoV2 request);

    /**
     * Aggregate daily data into scale_daily_reports table
     * This should be run by scheduled job every night
     */
    void aggregateDailyData();
}
