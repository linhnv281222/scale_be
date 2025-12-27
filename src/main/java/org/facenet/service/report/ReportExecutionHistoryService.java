package org.facenet.service.report;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.facenet.entity.report.ReportDefinition;
import org.facenet.entity.report.ReportExecutionHistory;
import org.facenet.repository.report.ReportExecutionHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing Report Execution History
 * Tracks and audits all report executions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportExecutionHistoryService {

    private final ReportExecutionHistoryRepository executionHistoryRepository;

    /**
     * Start tracking a new report execution
     */
    @Transactional
    public ReportExecutionHistory startExecution(
            ReportDefinition reportDefinition,
            String exportFormat,
            Map<String, Object> parameters,
            String executedBy) {
        
        ReportExecutionHistory history = ReportExecutionHistory.builder()
                .reportDefinition(reportDefinition)
                .reportCode(reportDefinition.getReportCode())
                .exportFormat(exportFormat)
                .parameters(parameters)
                .executionStartTime(OffsetDateTime.now())
                .status(ReportExecutionHistory.ExecutionStatus.STARTED)
                .executedBy(executedBy)
                .build();
        
        history = executionHistoryRepository.save(history);
        log.info("Started execution tracking for report {} (ID: {})", reportDefinition.getReportCode(), history.getId());
        
        return history;
    }

    /**
     * Mark execution as successful
     */
    @Transactional
    public void markSuccess(Long executionId, int recordCount, String fileName, long fileSizeBytes) {
        ReportExecutionHistory history = executionHistoryRepository.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution history not found: " + executionId));
        
        history.markSuccess(recordCount, fileName, fileSizeBytes);
        
        // Add result metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("recordCount", recordCount);
        metadata.put("fileSizeBytes", fileSizeBytes);
        metadata.put("fileName", fileName);
        history.setResultMetadata(metadata);
        
        executionHistoryRepository.save(history);
        log.info("Execution {} completed successfully: {} records, {} bytes", 
                executionId, recordCount, fileSizeBytes);
    }

    /**
     * Mark execution as failed
     */
    @Transactional
    public void markFailed(Long executionId, Exception exception) {
        ReportExecutionHistory history = executionHistoryRepository.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution history not found: " + executionId));
        
        String stackTrace = getStackTrace(exception);
        history.markFailed(exception.getMessage(), stackTrace);
        
        executionHistoryRepository.save(history);
        log.error("Execution {} failed: {}", executionId, exception.getMessage());
    }

    /**
     * Get execution history by report code
     */
    @Transactional(readOnly = true)
    public Page<ReportExecutionHistory> getHistoryByReportCode(String reportCode, Pageable pageable) {
        return executionHistoryRepository.findByReportCodeOrderByExecutionStartTimeDesc(reportCode, pageable);
    }

    /**
     * Get execution history by user
     */
    @Transactional(readOnly = true)
    public Page<ReportExecutionHistory> getHistoryByUser(String user, Pageable pageable) {
        return executionHistoryRepository.findByExecutedByOrderByExecutionStartTimeDesc(user, pageable);
    }

    /**
     * Get recent executions
     */
    @Transactional(readOnly = true)
    public Page<ReportExecutionHistory> getRecentExecutions(int page, int size) {
        return executionHistoryRepository.findRecentExecutions(PageRequest.of(page, size));
    }

    /**
     * Get executions in time range
     */
    @Transactional(readOnly = true)
    public List<ReportExecutionHistory> getExecutionsInRange(OffsetDateTime start, OffsetDateTime end) {
        return executionHistoryRepository.findByExecutionTimeRange(start, end);
    }

    /**
     * Get failed executions since timestamp
     */
    @Transactional(readOnly = true)
    public List<ReportExecutionHistory> getRecentFailures(OffsetDateTime since) {
        return executionHistoryRepository.findRecentFailures(since);
    }

    /**
     * Get success/failure statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getStatistics() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalExecutions", executionHistoryRepository.count());
        stats.put("successCount", executionHistoryRepository.countByStatus(ReportExecutionHistory.ExecutionStatus.SUCCESS));
        stats.put("failedCount", executionHistoryRepository.countByStatus(ReportExecutionHistory.ExecutionStatus.FAILED));
        stats.put("timeoutCount", executionHistoryRepository.countByStatus(ReportExecutionHistory.ExecutionStatus.TIMEOUT));
        return stats;
    }

    /**
     * Get average execution time for a report
     */
    @Transactional(readOnly = true)
    public Double getAverageExecutionTime(String reportCode) {
        return executionHistoryRepository.calculateAverageExecutionTime(reportCode);
    }

    /**
     * Count executions for a report
     */
    @Transactional(readOnly = true)
    public long countExecutions(String reportCode) {
        return executionHistoryRepository.countByReportCode(reportCode);
    }

    /**
     * Get stack trace as string
     */
    private String getStackTrace(Exception exception) {
        if (exception == null) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(exception.toString()).append("\n");
        
        for (StackTraceElement element : exception.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
            // Limit stack trace length
            if (sb.length() > 2000) {
                sb.append("\t... (truncated)");
                break;
            }
        }
        
        return sb.toString();
    }

    /**
     * Update execution with client information
     */
    @Transactional
    public void updateClientInfo(Long executionId, String clientIp, String userAgent) {
        ReportExecutionHistory history = executionHistoryRepository.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution history not found: " + executionId));
        
        history.setClientIp(clientIp);
        history.setUserAgent(userAgent);
        executionHistoryRepository.save(history);
    }
}
