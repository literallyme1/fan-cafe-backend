package com.example.fan_cafe.schedule.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MonthlyScheduleResponse {


    @Schema(description = "날짜별 일정 목록", example = "[{\"date\":\"2026-08-15\",\"schedules\":[]}]")
    private List<ScheduleListResponse> schedules;

    public static MonthlyScheduleResponse of(List<ScheduleListResponse> schedules) {
        return MonthlyScheduleResponse.builder()
                .schedules(schedules)
                .build();
    }
}
