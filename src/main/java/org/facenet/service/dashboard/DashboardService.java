package org.facenet.service.dashboard;

import lombok.RequiredArgsConstructor;
import org.facenet.common.exception.ValidationException;
import org.facenet.dto.dashboard.DashboardDto;
import org.facenet.entity.scale.Scale;
import org.facenet.entity.scale.ScaleCurrentState;
import org.facenet.entity.shift.Shift;
import org.facenet.repository.scale.ScaleCurrentStateRepository;
import org.facenet.repository.scale.ScaleRepository;
import org.facenet.repository.shift.ShiftRepository;
import org.facenet.service.shift.ShiftDetectionService;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final String DEFAULT_ZONE = "Asia/Bangkok";
    private static final Set<String> DATA_FIELDS = Set.of("data_1", "data_2", "data_3", "data_4", "data_5");

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ScaleRepository scaleRepository;
    private final ScaleCurrentStateRepository currentStateRepository;
    private final ShiftRepository shiftRepository;
    private final ShiftDetectionService shiftDetectionService;

    public DashboardDto.Overview getOverview(Long locationId, String totalField, String timezone) {
        String field = normalizeField(totalField, "data_1");
        ZoneId zone = parseZone(timezone);
        ZonedDateTime now = ZonedDateTime.now(zone);
        List<Scale> scales = activeScales(locationId);
        List<Long> scaleIds = scales.stream().map(Scale::getId).toList();

        Map<Long, ScaleCurrentState> currentStates = currentStateRepository.findAllById(scaleIds).stream()
                .collect(Collectors.toMap(ScaleCurrentState::getScaleId, Function.identity()));
        int online = (int) scaleIds.stream().filter(id -> isOnline(currentStates.get(id))).count();

        DashboardTimeRange day = DashboardTimeRange.day(now);
        DashboardTimeRange week = DashboardTimeRange.week(now);
        DashboardTimeRange month = DashboardTimeRange.month(now);

        return DashboardDto.Overview.builder()
                .generatedAt(now.toOffsetDateTime())
                .timezone(zone.getId())
                .totalScales(scaleIds.size())
                .onlineScales(online)
                .offlineScales(scaleIds.size() - online)
                .totalField(field)
                .day(periodKpi(day, field, scaleIds))
                .week(periodKpi(week, field, scaleIds))
                .month(periodKpi(month, field, scaleIds))
                .shifts(shiftProduction(now, field, scaleIds))
                .build();
    }

    public DashboardDto.ScaleTable getScaleTable(Long locationId, String totalField,
                                                   String flowField, String timezone) {
        String total = normalizeField(totalField, "data_1");
        String flow = normalizeField(flowField, "data_2");
        ZoneId zone = parseZone(timezone);
        ZonedDateTime now = ZonedDateTime.now(zone);
        List<Scale> scales = activeScales(locationId);
        List<Long> ids = scales.stream().map(Scale::getId).toList();
        Map<Long, ScaleCurrentState> states = currentStateRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(ScaleCurrentState::getScaleId, Function.identity()));

        DashboardTimeRange day = DashboardTimeRange.day(now);
        DashboardTimeRange week = DashboardTimeRange.week(now);
        DashboardTimeRange month = DashboardTimeRange.month(now);
        PeriodMaps dayMaps = periodMaps(day, total, ids);
        PeriodMaps weekMaps = periodMaps(week, total, ids);
        PeriodMaps monthMaps = periodMaps(month, total, ids);
        Map<Long, Map<String, BigDecimal>> shifts = shiftProductionByScale(now, total, ids);

        List<DashboardDto.ScaleDetail> details = scales.stream().map(scale -> {
            ScaleCurrentState state = states.get(scale.getId());
            return DashboardDto.ScaleDetail.builder()
                    .scaleId(scale.getId())
                    .scaleName(scale.getName())
                    .locationId(scale.getLocation() != null ? scale.getLocation().getId() : null)
                    .locationName(scale.getLocation() != null ? scale.getLocation().getName() : null)
                    .status(isOnline(state) ? "ONLINE" : "OFFLINE")
                    .lastTime(state != null ? state.getLastTime() : null)
                    .currentFlow(currentValue(state, flow))
                    .currentTotal(currentValue(state, total))
                    .shiftProduction(shifts.getOrDefault(scale.getId(), Map.of()))
                    .day(periodKpi(day, dayMaps.current(), dayMaps.previous(), scale.getId()))
                    .week(periodKpi(week, weekMaps.current(), weekMaps.previous(), scale.getId()))
                    .month(periodKpi(month, monthMaps.current(), monthMaps.previous(), scale.getId()))
                    .build();
        }).toList();

        return DashboardDto.ScaleTable.builder()
                .generatedAt(now.toOffsetDateTime())
                .totalField(total)
                .flowField(flow)
                .scales(details)
                .build();
    }

    public DashboardDto.Trend getTrend(List<Long> requestedScaleIds, Long locationId, String flowField,
                                       String period, String bucket, OffsetDateTime from,
                                       OffsetDateTime to, String timezone) {
        String field = normalizeField(flowField, "data_2");
        ZoneId zone = parseZone(timezone);
        ZonedDateTime now = ZonedDateTime.now(zone);
        List<Scale> allowedScales = activeScales(locationId);
        if (requestedScaleIds != null && !requestedScaleIds.isEmpty()) {
            Set<Long> requested = Set.copyOf(requestedScaleIds);
            allowedScales = allowedScales.stream().filter(scale -> requested.contains(scale.getId())).toList();
        }
        List<Long> scaleIds = allowedScales.stream().map(Scale::getId).toList();
        TimeWindow window = resolveWindow(period, from, to, now);
        String normalizedBucket = resolveBucket(bucket, window);
        Map<Long, String> names = allowedScales.stream()
                .collect(Collectors.toMap(Scale::getId, Scale::getName));

        Map<Long, List<DashboardDto.TrendPoint>> points = queryTrend(
                scaleIds, field, normalizedBucket, window.from(), window.to(), zone);
        List<DashboardDto.TrendSeries> series = allowedScales.stream()
                .map(scale -> DashboardDto.TrendSeries.builder()
                        .scaleId(scale.getId())
                        .scaleName(names.get(scale.getId()))
                        .points(points.getOrDefault(scale.getId(), List.of()))
                        .build())
                .toList();

        return DashboardDto.Trend.builder()
                .from(window.from())
                .to(window.to())
                .bucket(normalizedBucket.toUpperCase(Locale.ROOT))
                .flowField(field)
                .series(series)
                .build();
    }

    private List<Scale> activeScales(Long locationId) {
        return scaleRepository.findAllActiveWithConfig().stream()
                .filter(scale -> locationId == null || (scale.getLocation() != null
                        && Objects.equals(scale.getLocation().getId(), locationId)))
                .sorted(Comparator.comparing(Scale::getId))
                .toList();
    }

    private DashboardDto.PeriodKpi periodKpi(DashboardTimeRange range, String field, List<Long> scaleIds) {
        BigDecimal current = productionTotal(field, scaleIds, range.start(), range.end());
        BigDecimal previous = productionTotal(field, scaleIds, range.previousStart(), range.previousEnd());
        return periodKpi(range, current, previous);
    }

    private DashboardDto.PeriodKpi periodKpi(DashboardTimeRange range, Map<Long, BigDecimal> current,
                                               Map<Long, BigDecimal> previous, Long scaleId) {
        return periodKpi(range, current.getOrDefault(scaleId, BigDecimal.ZERO),
                previous.getOrDefault(scaleId, BigDecimal.ZERO));
    }

    private DashboardDto.PeriodKpi periodKpi(DashboardTimeRange range,
                                               BigDecimal current, BigDecimal previous) {
        BigDecimal change = percentage(current.subtract(previous), previous);
        BigDecimal achievement = percentage(current, previous);
        return DashboardDto.PeriodKpi.builder()
                .periodStart(range.start()).periodEnd(range.end())
                .previousStart(range.previousStart()).previousEnd(range.previousEnd())
                .currentValue(current).previousValue(previous)
                .changePercentage(change).achievementPercentage(achievement)
                .build();
    }

    private BigDecimal percentage(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) return null;
        return numerator.multiply(BigDecimal.valueOf(100))
                .divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private PeriodMaps periodMaps(DashboardTimeRange range, String field, List<Long> ids) {
        return new PeriodMaps(
                productionByScale(field, ids, range.start(), range.end()),
                productionByScale(field, ids, range.previousStart(), range.previousEnd()));
    }

    private BigDecimal productionTotal(String field, List<Long> ids,
                                       OffsetDateTime start, OffsetDateTime end) {
        return productionByScale(field, ids, start, end).values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<Long, BigDecimal> productionByScale(String field, List<Long> ids,
                                                    OffsetDateTime start, OffsetDateTime end) {
        if (ids.isEmpty() || start == null || end == null) return Map.of();
        String numeric = numericExpression("wl." + field);
        String sql = """
                SELECT wl.scale_id, COALESCE(ABS(MAX(%1$s) - MIN(%1$s)), 0) AS production
                FROM weighing_logs wl
                WHERE wl.scale_id IN (:scaleIds)
                  AND wl.last_time >= :startTime AND wl.last_time < :endTime
                GROUP BY wl.scale_id
                """.formatted(numeric);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("scaleIds", ids).addValue("startTime", start).addValue("endTime", end);
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        jdbcTemplate.queryForList(sql, params).forEach(row -> result.put(
                ((Number) row.get("scale_id")).longValue(), decimal(row.get("production"))));
        return result;
    }

    private List<DashboardDto.ShiftProduction> shiftProduction(ZonedDateTime now, String field,
                                                                List<Long> scaleIds) {
        Map<Long, Map<String, BigDecimal>> perScale = shiftProductionByScale(now, field, scaleIds);
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        perScale.values().forEach(values -> values.forEach(
                (code, value) -> totals.merge(code, value, BigDecimal::add)));
        return activeShifts().stream().map(shift -> DashboardDto.ShiftProduction.builder()
                .shiftId(shift.getId()).shiftCode(shift.getCode()).shiftName(shift.getName())
                .startTime(shift.getStartTime()).endTime(shift.getEndTime())
                .production(totals.getOrDefault(shift.getCode(), BigDecimal.ZERO))
                .build()).toList();
    }

    private Map<Long, Map<String, BigDecimal>> shiftProductionByScale(ZonedDateTime now, String field,
                                                                      List<Long> scaleIds) {
        if (scaleIds.isEmpty()) return Map.of();
        Map<Long, Map<String, BigDecimal>> result = new LinkedHashMap<>();
        for (Shift shift : activeShifts()) {
            TimeWindow window = shiftWindow(now, shift);
            Map<Long, BigDecimal> values = window.from().isBefore(window.to())
                    ? productionByScale(field, scaleIds, window.from(), window.to()) : Map.of();
            for (Long scaleId : scaleIds) {
                result.computeIfAbsent(scaleId, ignored -> new LinkedHashMap<>())
                        .put(shift.getCode(), values.getOrDefault(scaleId, BigDecimal.ZERO));
            }
        }
        return result;
    }

    private TimeWindow shiftWindow(ZonedDateTime now, Shift shift) {
        LocalDate date = now.toLocalDate();
        boolean overnight = !shift.getStartTime().isBefore(shift.getEndTime());
        ZonedDateTime start;
        ZonedDateTime scheduledEnd;
        if (overnight && now.toLocalTime().isBefore(shift.getEndTime())) {
            start = date.minusDays(1).atTime(shift.getStartTime()).atZone(now.getZone());
            scheduledEnd = date.atTime(shift.getEndTime()).atZone(now.getZone());
        } else {
            start = date.atTime(shift.getStartTime()).atZone(now.getZone());
            scheduledEnd = (overnight ? date.plusDays(1) : date)
                    .atTime(shift.getEndTime()).atZone(now.getZone());
        }
        ZonedDateTime effectiveEnd = scheduledEnd.isBefore(now) ? scheduledEnd : now;
        return new TimeWindow(start.toOffsetDateTime(), effectiveEnd.toOffsetDateTime());
    }

    private List<Shift> activeShifts() {
        return shiftRepository.findAll().stream()
                .filter(shift -> Boolean.TRUE.equals(shift.getIsActive()))
                .filter(shift -> shift.getStartTime() != null && shift.getEndTime() != null)
                .sorted(Comparator.comparing(Shift::getStartTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private Map<Long, List<DashboardDto.TrendPoint>> queryTrend(List<Long> scaleIds, String field,
                                                                String bucket, OffsetDateTime from,
                                                                OffsetDateTime to, ZoneId zone) {
        if (scaleIds.isEmpty()) return Map.of();
        String numeric = numericExpression("wl." + field);
        String sql = """
                SELECT wl.scale_id, DATE_TRUNC('%s', wl.last_time) AS bucket_time,
                       AVG(%s) AS average_value
                FROM weighing_logs wl
                WHERE wl.scale_id IN (:scaleIds)
                  AND wl.last_time >= :fromTime AND wl.last_time < :toTime
                  AND %s IS NOT NULL
                GROUP BY wl.scale_id, DATE_TRUNC('%s', wl.last_time)
                ORDER BY wl.scale_id, bucket_time
                """.formatted(bucket, numeric, numeric, bucket);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("scaleIds", scaleIds).addValue("fromTime", from).addValue("toTime", to);
        Map<Long, List<DashboardDto.TrendPoint>> result = new LinkedHashMap<>();
        jdbcTemplate.queryForList(sql, params).forEach(row -> {
            Long scaleId = ((Number) row.get("scale_id")).longValue();
            result.computeIfAbsent(scaleId, ignored -> new ArrayList<>()).add(
                    DashboardDto.TrendPoint.builder()
                            .timestamp(toOffsetDateTime(row.get("bucket_time"), zone))
                            .value(decimal(row.get("average_value")))
                            .build());
        });
        return result;
    }

    private TimeWindow resolveWindow(String period, OffsetDateTime from, OffsetDateTime to, ZonedDateTime now) {
        String value = period == null ? "HOUR" : period.trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "HOUR" -> new TimeWindow(now.minusHours(1).toOffsetDateTime(), now.toOffsetDateTime());
            case "DAY" -> new TimeWindow(now.toLocalDate().atStartOfDay(now.getZone()).toOffsetDateTime(),
                    now.toOffsetDateTime());
            case "SHIFT" -> shiftDetectionService.detectCurrentShift(now.toOffsetDateTime())
                    .map(shift -> {
                        DashboardTimeRange range = DashboardTimeRange.currentShift(now, shift);
                        return new TimeWindow(range.start(), now.toOffsetDateTime().isBefore(range.end())
                                ? now.toOffsetDateTime() : range.end());
                    })
                    .orElseThrow(() -> new ValidationException("No active shift matches the current time"));
            case "CUSTOM" -> validateCustomWindow(from, to);
            default -> throw new ValidationException("period must be SHIFT, HOUR, DAY or CUSTOM");
        };
    }

    private TimeWindow validateCustomWindow(OffsetDateTime from, OffsetDateTime to) {
        if (from == null || to == null || !from.isBefore(to)) {
            throw new ValidationException("CUSTOM period requires from < to");
        }
        if (Duration.between(from, to).toDays() > 366) {
            throw new ValidationException("Dashboard trend range must not exceed 366 days");
        }
        return new TimeWindow(from, to);
    }

    private String resolveBucket(String bucket, TimeWindow window) {
        if (bucket != null && !bucket.isBlank() && !bucket.equalsIgnoreCase("AUTO")) {
            String value = bucket.trim().toLowerCase(Locale.ROOT);
            if (!Set.of("minute", "hour", "day").contains(value)) {
                throw new ValidationException("bucket must be AUTO, MINUTE, HOUR or DAY");
            }
            return value;
        }
        Duration duration = Duration.between(window.from(), window.to());
        if (duration.toHours() <= 6) return "minute";
        if (duration.toDays() <= 3) return "hour";
        return "day";
    }

    private String numericExpression(String column) {
        return "CASE WHEN TRIM(" + column + ") ~ '^[+-]?([0-9]+([.][0-9]*)?|[.][0-9]+)$' "
                + "THEN TRIM(" + column + ")::NUMERIC END";
    }

    private String normalizeField(String field, String defaultField) {
        String value = field == null || field.isBlank() ? defaultField : field.trim().toLowerCase(Locale.ROOT);
        if (value.matches("data[1-5]")) value = value.replace("data", "data_");
        if (!DATA_FIELDS.contains(value)) throw new ValidationException("Data field must be data_1 to data_5");
        return value;
    }

    private ZoneId parseZone(String timezone) {
        try {
            return ZoneId.of(timezone == null || timezone.isBlank() ? DEFAULT_ZONE : timezone.trim());
        } catch (Exception exception) {
            throw new ValidationException("Invalid timezone: " + timezone);
        }
    }

    private boolean isOnline(ScaleCurrentState state) {
        return state != null && "ONLINE".equalsIgnoreCase(state.getStatus());
    }

    private BigDecimal currentValue(ScaleCurrentState state, String field) {
        if (state == null) return null;
        String value = switch (field) {
            case "data_1" -> state.getData1();
            case "data_2" -> state.getData2();
            case "data_3" -> state.getData3();
            case "data_4" -> state.getData4();
            case "data_5" -> state.getData5();
            default -> null;
        };
        try {
            return value == null || value.isBlank() ? null : new BigDecimal(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private BigDecimal decimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        return value instanceof BigDecimal decimal ? decimal : new BigDecimal(value.toString());
    }

    private OffsetDateTime toOffsetDateTime(Object value, ZoneId zone) {
        if (value instanceof OffsetDateTime offsetDateTime) return offsetDateTime;
        if (value instanceof Timestamp timestamp) return timestamp.toInstant().atZone(zone).toOffsetDateTime();
        if (value instanceof LocalDateTime localDateTime) return localDateTime.atZone(zone).toOffsetDateTime();
        throw new IllegalStateException("Unsupported dashboard timestamp type: " + value);
    }

    private record PeriodMaps(Map<Long, BigDecimal> current, Map<Long, BigDecimal> previous) {
    }

    private record TimeWindow(OffsetDateTime from, OffsetDateTime to) {
    }
}
