package org.facenet.service.dashboard;

import org.facenet.entity.shift.Shift;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

record DashboardTimeRange(OffsetDateTime start, OffsetDateTime end,
                          OffsetDateTime previousStart, OffsetDateTime previousEnd) {

    static DashboardTimeRange day(ZonedDateTime now) {
        ZonedDateTime start = now.toLocalDate().atStartOfDay(now.getZone());
        return comparison(start, now, start.minusDays(1));
    }

    static DashboardTimeRange week(ZonedDateTime now) {
        ZonedDateTime start = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .toLocalDate().atStartOfDay(now.getZone());
        return comparison(start, now, start.minusWeeks(1));
    }

    static DashboardTimeRange month(ZonedDateTime now) {
        ZonedDateTime start = now.withDayOfMonth(1).toLocalDate().atStartOfDay(now.getZone());
        return comparison(start, now, start.minusMonths(1));
    }

    static DashboardTimeRange currentShift(ZonedDateTime now, Shift shift) {
        ZoneId zone = now.getZone();
        var date = now.toLocalDate();
        var time = now.toLocalTime();
        ZonedDateTime start;
        ZonedDateTime end;
        if (shift.getStartTime().isBefore(shift.getEndTime())) {
            start = date.atTime(shift.getStartTime()).atZone(zone);
            end = date.atTime(shift.getEndTime()).atZone(zone);
        } else if (!time.isBefore(shift.getStartTime())) {
            start = date.atTime(shift.getStartTime()).atZone(zone);
            end = date.plusDays(1).atTime(shift.getEndTime()).atZone(zone);
        } else {
            start = date.minusDays(1).atTime(shift.getStartTime()).atZone(zone);
            end = date.atTime(shift.getEndTime()).atZone(zone);
        }
        return new DashboardTimeRange(start.toOffsetDateTime(), end.toOffsetDateTime(), null, null);
    }

    private static DashboardTimeRange comparison(ZonedDateTime start, ZonedDateTime end,
                                                   ZonedDateTime previousStart) {
        Duration elapsed = Duration.between(start, end);
        ZonedDateTime previousEnd = previousStart.plus(elapsed);
        if (previousEnd.isAfter(start)) previousEnd = start;
        return new DashboardTimeRange(start.toOffsetDateTime(), end.toOffsetDateTime(),
                previousStart.toOffsetDateTime(), previousEnd.toOffsetDateTime());
    }
}
