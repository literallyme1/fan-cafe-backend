package com.example.fan_cafe.schedule.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Builder
@Getter
public class ScheduleListResponse {
    @Schema(description = "일정 날짜", example = "2026-08-15")
    private LocalDate date;
    @Schema(description = "해당 날짜의 일정 목록", example = "[{\"id\":901,\"title\":\"2026 서울 월드투어\"}]")
    private List<ScheduleResponse> schedules;

    public static ScheduleListResponse of(LocalDate date, List<ScheduleResponse> scheduleList) {
        return ScheduleListResponse.builder()
                .date(date)
                .schedules(scheduleList)
                .build();
    }
}
