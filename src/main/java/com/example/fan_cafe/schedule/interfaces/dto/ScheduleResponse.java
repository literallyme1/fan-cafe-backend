package com.example.fan_cafe.schedule.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.fan_cafe.schedule.domain.Schedule;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Getter
@NoArgsConstructor
public class ScheduleResponse {

    @Schema(description = "일정 식별자", example = "901")
    private Long id;
    @Schema(description = "일정 제목", example = "2026 서울 월드투어")
    private String title;
    @Schema(description = "일정 장소", example = "고척스카이돔")
    private String location;
    @Schema(description = "시작 시각", example = "2026-08-15T18:00:00")
    private LocalDateTime startAt;
    @Schema(description = "종료 시각", example = "2026-08-15T21:00:00")
    private LocalDateTime endAt;

    @QueryProjection
    public ScheduleResponse(Long id, String title, String location, LocalDateTime startAt, LocalDateTime endAt) {
        this.id = id;
        this.title = title;
        this.location = location;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public static ScheduleResponse from(Schedule schedule) {
        return ScheduleResponse.builder()
                .id(schedule.getId())
                .title(schedule.getTitle())
                .location(schedule.getLocation())
                .startAt(schedule.getStartAt())
                .endAt(schedule.getEndAt())
                .build();
    }

}
