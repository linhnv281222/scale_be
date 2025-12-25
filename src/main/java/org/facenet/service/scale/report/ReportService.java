package org.facenet.service.scale.report;

import org.facenet.dto.scale.ReportRequestDto;
import org.facenet.dto.scale.ReportResponseDto;

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
     * Aggregate daily data into scale_daily_reports table
     * This should be run by scheduled job every night
     */
    void aggregateDailyData();
}
