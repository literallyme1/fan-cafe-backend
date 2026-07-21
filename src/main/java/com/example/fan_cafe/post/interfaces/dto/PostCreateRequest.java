package com.example.fan_cafe.post.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

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
    @Schema(description = "게시글 제목", example = "서울 콘서트 첫날 후기")
    private String title;

    @Schema(description = "게시글 내용", example = "무대 연출과 앙코르가 정말 인상적이었어요.")
    private String content;

}
