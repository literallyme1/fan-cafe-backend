package com.example.fan_cafe.global.logging.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record LogLevelApplyResponse(
        @Schema(description = "로그 도메인", example = "order") String domain,
        @Schema(description = "적용 로그 레벨", example = "DEBUG") String level,
        @Schema(description = "적용 상태", example = "applied") String status
) {
}
