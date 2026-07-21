package com.example.fan_cafe.user.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

public record ProfileRequest (

        @Size(max = 200, message = "소개글은 200자 이하로 작성해주세요.")
        @Schema(description = "프로필 소개", example = "콘서트와 신보 소식을 기록합니다.")
        String introduction,

        @NotBlank(message = "닉네임을 입력하세요.")
        @Size(max = 10, message = "닉네임은 10자 이하여야 합니다.")
        @Pattern(
                regexp = "^[a-zA-Z0-9가-힣_-]+$",
                message = "닉네임은 한글, 영문, 숫자, 밑줄(_), 하이픈(-)만 사용할 수 있습니다."
        )
        @Schema(description = "회원 닉네임", example = "별빛팬")
        String nickname,

        @NotNull(message = "isImageChanged 값은 필수입니다.")
        @Schema(description = "프로필 이미지 변경 여부", example = "true")
        Boolean isImageChanged
){
        public ProfileRequest {
                if (introduction != null) {
                        introduction = introduction.trim();
                }
        }
}
