package com.example.fan_cafe.schedule.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;

public record DateRange(
        @Schema(description = "조회 시작 시각", example = "2026-08-01T00:00:00") LocalDateTime start,
        @Schema(description = "조회 종료 시각", example = "2026-08-31T23:59:59") LocalDateTime end
) {
    public static DateRange ofMonth(int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = YearMonth.of(year, month).atEndOfMonth().atTime(LocalTime.MAX);
        return new DateRange(start, end);
    }
}
