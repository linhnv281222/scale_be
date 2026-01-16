package org.facenet.service.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.facenet.dto.scale.ManualReadingRequestDto;
import org.facenet.dto.scale.ManualReadingResponseDto;
import org.facenet.entity.shift.Shift;
import org.facenet.repository.shift.ShiftRepository;
import org.facenet.service.scale.ManualReadingService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

/**
 * Service to schedule automatic manual readings at shift start and end times
 * Monitors active shifts and creates scheduled tasks for each shift boundary
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShiftReadingSchedulerService {
    
    private final ShiftRepository shiftRepository;
    private final ManualReadingService manualReadingService;
    private final TaskScheduler taskScheduler;
    
    // Store scheduled tasks to allow cancellation on shift update
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new HashMap<>();
    
    /**
     * Initialize shift reading schedules on application startup
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeShiftSchedules() {
        log.info("[SHIFT-SCHEDULER] Initializing shift reading schedules");
        scheduleShiftReadings();
    }
    
    /**
     * Schedule readings for all active shifts
     * Creates scheduled tasks for shift start and end times
     */
    public void scheduleShiftReadings() {
        // Cancel existing schedules
        cancelAllSchedules();
        
        // Get all active shifts
        List<Shift> activeShifts = shiftRepository.findAll().stream()
                .filter(shift -> shift.getIsActive() != null && shift.getIsActive())
                .filter(shift -> shift.getStartTime() != null && shift.getEndTime() != null)
                .toList();
        
        if (activeShifts.isEmpty()) {
            log.warn("[SHIFT-SCHEDULER] No active shifts found to schedule");
            return;
        }
        
        for (Shift shift : activeShifts) {
            scheduleShiftStartReading(shift);
            scheduleShiftEndReading(shift);
        }
        
        log.info("[SHIFT-SCHEDULER] Scheduled readings for {} shifts", activeShifts.size());
    }
    
    /**
     * Schedule reading at shift start time
     */
    private void scheduleShiftStartReading(Shift shift) {
        String taskKey = "shift_start_" + shift.getId();
        
        try {
            // Create cron expression for shift start time
            String cronExpression = createCronExpression(shift.getStartTime());
            
            ScheduledFuture<?> scheduledTask = taskScheduler.schedule(
                () -> performShiftReading(shift, ManualReadingRequestDto.ReadingType.SHIFT_START),
                new CronTrigger(cronExpression, ZoneId.systemDefault())
            );
            
            scheduledTasks.put(taskKey, scheduledTask);
            
            log.info("[SHIFT-SCHEDULER] Scheduled SHIFT_START reading for shift {} ({}) at {}", 
                    shift.getCode(), shift.getName(), shift.getStartTime());
            
        } catch (Exception e) {
            log.error("[SHIFT-SCHEDULER] Failed to schedule shift start reading for shift {}: {}", 
                    shift.getCode(), e.getMessage());
        }
    }
    
    /**
     * Schedule reading at shift end time
     */
    private void scheduleShiftEndReading(Shift shift) {
        String taskKey = "shift_end_" + shift.getId();
        
        try {
            // Create cron expression for shift end time
            String cronExpression = createCronExpression(shift.getEndTime());
            
            ScheduledFuture<?> scheduledTask = taskScheduler.schedule(
                () -> performShiftReading(shift, ManualReadingRequestDto.ReadingType.SHIFT_END),
                new CronTrigger(cronExpression, ZoneId.systemDefault())
            );
            
            scheduledTasks.put(taskKey, scheduledTask);
            
            log.info("[SHIFT-SCHEDULER] Scheduled SHIFT_END reading for shift {} ({}) at {}", 
                    shift.getCode(), shift.getName(), shift.getEndTime());
            
        } catch (Exception e) {
            log.error("[SHIFT-SCHEDULER] Failed to schedule shift end reading for shift {}: {}", 
                    shift.getCode(), e.getMessage());
        }
    }
    
    /**
     * Perform manual reading at shift boundary
     */
    private void performShiftReading(Shift shift, ManualReadingRequestDto.ReadingType readingType) {
        log.info("[SHIFT-SCHEDULER] Executing scheduled {} reading for shift {}", 
                readingType, shift.getCode());
        
        try {
            ManualReadingRequestDto request = ManualReadingRequestDto.builder()
                    .readingType(readingType)
                    .shiftId(shift.getId())
                    .note("Auto-scheduled at " + readingType.name().toLowerCase().replace("_", " "))
                    .build();
            
            ManualReadingResponseDto response = manualReadingService.performManualReading(request);
            
            log.info("[SHIFT-SCHEDULER] Completed {} reading for shift {}: success={}, failed={}", 
                    readingType, shift.getCode(), 
                    response.getSuccessfulReadings(), response.getFailedReadings());
            
        } catch (Exception e) {
            log.error("[SHIFT-SCHEDULER] Error performing {} reading for shift {}: {}", 
                    readingType, shift.getCode(), e.getMessage(), e);
        }
    }
    
    /**
     * Create cron expression from LocalTime
     * Format: "0 mm HH * * *" (every day at HH:mm)
     */
    private String createCronExpression(LocalTime time) {
        return String.format("0 %d %d * * *", time.getMinute(), time.getHour());
    }
    
    /**
     * Cancel all scheduled tasks
     */
    private void cancelAllSchedules() {
        for (Map.Entry<String, ScheduledFuture<?>> entry : scheduledTasks.entrySet()) {
            entry.getValue().cancel(false);
        }
        scheduledTasks.clear();
        log.debug("[SHIFT-SCHEDULER] Cancelled all existing schedules");
    }
    
    /**
     * Refresh schedules (useful when shifts are updated)
     */
    public void refreshSchedules() {
        log.info("[SHIFT-SCHEDULER] Refreshing shift reading schedules");
        scheduleShiftReadings();
    }
}
