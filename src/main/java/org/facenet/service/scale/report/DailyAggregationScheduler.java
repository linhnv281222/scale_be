package org.facenet.service.scale.report;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job to aggregate daily data
 * Runs every night at 00:01 to aggregate previous day's data
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyAggregationScheduler {

    private final ReportService reportService;

    /**
     * Aggregate daily data at 00:01 every day
     * Cron format: second minute hour day month day-of-week
     */
    @Scheduled(cron = "0 1 0 * * *")
    public void aggregateDailyData() {
        log.info("[SCHEDULER] Starting scheduled daily aggregation");
        try {
            reportService.aggregateDailyData();
            log.info("[SCHEDULER] Daily aggregation completed successfully");
        } catch (Exception e) {
            log.error("[SCHEDULER] Daily aggregation failed: {}", e.getMessage(), e);
        }
    }
}
