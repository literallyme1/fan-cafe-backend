package com.example.fan_cafe.comment.domain;


import com.example.fan_cafe.like.domain.BaseLike;
import com.example.fan_cafe.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "comment_like", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "comment_id"})
})
public class CommentLike extends BaseLike {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

public static CommentLike of(Comment comment, User user) {
        CommentLike newCommentLike = new CommentLike();
        newCommentLike.comment = comment;
        newCommentLike.user = user;
        return newCommentLike;
    }
}
