package com.example.fan_cafe.post.interfaces.dto;


import com.example.fan_cafe.post.domain.Post;
import com.example.fan_cafe.post.domain.PostImage;
import com.example.fan_cafe.user.domain.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;


@Builder
@Getter
@AllArgsConstructor
public class PostCreateRequest {

    @NotBlank(message = "제목을 입력하세요")
    private String title;

    private String content;

    @NotEmpty(message = "사진을 최소 1장 첨부하세요")
    private List<String> imageUrls;

    public Post toEntity(User user) {
        Post post = Post.builder()
                .user(user)
                .title(this.title)
                .content(this.content)
                .build();

        for (String url : imageUrls) {
            PostImage image = new PostImage(url);
            post.addImage(image);
        }

        return post;
    }
}
