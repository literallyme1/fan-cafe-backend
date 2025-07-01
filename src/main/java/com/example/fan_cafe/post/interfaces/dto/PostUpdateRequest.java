package com.example.fan_cafe.post.interfaces.dto;

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
    private String title;
    private String content;

    @NotEmpty(message = "사진이 한 장 이상이어야 합니다.")
    private List<String> imageUrls;
}
