package com.example.fan_cafe.post.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PostUpdateRequest {

    @NotBlank(message = "제목을 입력하세요.")
    @Schema(description = "게시글 제목", example = "서울 콘서트 첫날 후기 수정")
    private String title;
    @Schema(description = "게시글 내용", example = "공연 사진과 후기를 추가했습니다.")
    private String content;

    @NotEmpty(message = "사진이 한 장 이상이어야 합니다.")
    @Schema(description = "유지할 이미지 URL 목록", example = "[\"https://cdn.fancafe.kr/posts/3201/01.jpg\"]")
    private List<String> imageUrls;
}
