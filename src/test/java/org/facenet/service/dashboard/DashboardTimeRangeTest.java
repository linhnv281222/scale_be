package org.facenet.service.dashboard;

import org.facenet.entity.shift.Shift;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DashboardTimeRangeTest {

    private static final ZoneId BANGKOK = ZoneId.of("Asia/Bangkok");

    @Test
    void dayUsesSameElapsedWindowOnPreviousDay() {
        ZonedDateTime now = ZonedDateTime.of(2026, 10, 14, 10, 30, 0, 0, BANGKOK);
        DashboardTimeRange range = DashboardTimeRange.day(now);

        assertEquals("2026-10-14T00:00+07:00", range.start().toString());
        assertEquals("2026-10-13T00:00+07:00", range.previousStart().toString());
        assertEquals("2026-10-13T10:30+07:00", range.previousEnd().toString());
    }

    @Test
    void weekStartsOnMonday() {
        ZonedDateTime now = ZonedDateTime.of(2026, 10, 14, 10, 30, 0, 0, BANGKOK);
        DashboardTimeRange range = DashboardTimeRange.week(now);

        assertEquals("2026-10-12T00:00+07:00", range.start().toString());
        assertEquals("2026-10-05T00:00+07:00", range.previousStart().toString());
    }

    @Test
    void overnightShiftStartedOnPreviousDate() {
        Shift shift = Shift.builder()
                .startTime(LocalTime.of(22, 0))
                .endTime(LocalTime.of(6, 0))
                .build();
        ZonedDateTime now = ZonedDateTime.of(2026, 10, 14, 2, 0, 0, 0, BANGKOK);
        DashboardTimeRange range = DashboardTimeRange.currentShift(now, shift);

        assertEquals("2026-10-13T22:00+07:00", range.start().toString());
        assertEquals("2026-10-14T06:00+07:00", range.end().toString());
    }
}
