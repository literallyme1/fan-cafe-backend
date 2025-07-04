package com.example.fan_cafe.schedule.interfaces.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class ScheduleListResponse {

    private List<ScheduleResponse> scheduleList;

    public static ScheduleListResponse from(List<ScheduleResponse> scheduleList) {
        return ScheduleListResponse.builder()
                .scheduleList(scheduleList)
                .build();
    }
}
