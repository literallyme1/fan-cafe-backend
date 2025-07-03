package com.example.fan_cafe.comment.interfaces.dto;

import com.example.fan_cafe.comment.domain.Comment;
import com.example.fan_cafe.post.domain.Post;
import com.example.fan_cafe.user.domain.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CommentCreateRequest {

    @NotNull(message = "게시글이 불분명합니다.")
    private Long postId;
    @NotBlank(message = "댓글을 입력해주세요.")
    private String content;
    private Long parentId;

    public Comment toEntity(Post post, User user, String content) {
        return Comment.builder()
                .post(post)
                .user(user)
                .build();
    }

    public Comment toEntity(Post post, User user, Comment parentComment, String content) {
        return Comment.builder()
                .post(post)
                .user(user)
                .parent(parentComment)
                .build();
    }
}
