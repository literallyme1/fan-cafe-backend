package com.example.fan_cafe.post.interfaces.dto;


import com.example.fan_cafe.post.domain.Post;
import com.example.fan_cafe.post.domain.PostImage;
import com.example.fan_cafe.user.domain.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;


@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PostCreateRequest {

    @NotBlank(message = "제목을 입력하세요")
    private String title;

    private String content;

}
