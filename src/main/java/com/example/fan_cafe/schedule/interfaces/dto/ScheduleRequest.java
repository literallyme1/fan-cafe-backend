package com.example.fan_cafe.schedule.interfaces.dto;


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
    private String title;

    @NotBlank(message = "장소를 입력해주세요.")
    private String location;

    @NotNull(message = "스케줄 시작 시간을 입력해주세요.")
    private LocalDateTime startAt;

    private LocalDateTime endAt;

}
