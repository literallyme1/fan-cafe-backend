package com.example.fan_cafe.schedule.interfaces.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;

public record DateRange(LocalDateTime start, LocalDateTime end) {
    public static DateRange ofMonth(int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = YearMonth.of(year, month).atEndOfMonth().atTime(LocalTime.MAX);
        return new DateRange(start, end);
    }
}
