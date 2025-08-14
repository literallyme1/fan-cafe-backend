package com.example.fan_cafe.user.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileRequest (

        @Size(max = 200, message = "소개글은 200자 이하로 작성해주세요.")
        String introduction,

        @NotBlank(message = "닉네임을 입력하세요.")
        @Size(max = 10, message = "닉네임은 10자 이하여야 합니다.")
        @Pattern(
                regexp = "^[a-zA-Z0-9가-힣_-]+$",
                message = "닉네임은 한글, 영문, 숫자, 밑줄(_), 하이픈(-)만 사용할 수 있습니다."
        )
        String nickname
){
        public ProfileRequest {
                if (introduction != null) {
                        introduction = introduction.trim();
                }
}
