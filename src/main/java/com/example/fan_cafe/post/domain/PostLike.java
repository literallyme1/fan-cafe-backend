package com.example.fan_cafe.post.domain;

import com.example.fan_cafe.like.domain.BaseLike;
import com.example.fan_cafe.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "post_like", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "post_id"})
})
public class PostLike extends BaseLike {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

public static PostLike of(Post post, User user) {
        PostLike newPostLike = new PostLike();
        newPostLike.post = post;
        newPostLike.user = user;
        return newPostLike;
    }
}