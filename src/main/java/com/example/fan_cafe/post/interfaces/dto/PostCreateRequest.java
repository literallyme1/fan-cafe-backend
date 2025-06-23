package com.example.fan_cafe.post.interfaces.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PostCreateRequest {

    @NotBlank(message = "제목을 입력하세요")
    private String title;

    private String content;

    @NotBlank(message = "사진을 첨부하세요")
    private String imageUrl;


}
