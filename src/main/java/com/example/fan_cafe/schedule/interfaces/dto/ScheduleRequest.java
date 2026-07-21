package com.example.fan_cafe.schedule.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleRequest {

    @NotBlank(message = "스케줄을 입력해주세요.")
    @Schema(description = "일정 제목", example = "2026 서울 월드투어")
    private String title;

    @NotBlank(message = "장소를 입력해주세요.")
    @Schema(description = "일정 장소", example = "고척스카이돔")
    private String location;

    @NotNull(message = "스케줄 시작 시간을 입력해주세요.")
    @Schema(description = "시작 시각", example = "2026-08-15T18:00:00")
    private LocalDateTime startAt;

    @Schema(description = "종료 시각", example = "2026-08-15T21:00:00")
    private LocalDateTime endAt;

}
