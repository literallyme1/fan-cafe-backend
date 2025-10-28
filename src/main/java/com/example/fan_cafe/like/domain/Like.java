package com.example.fan_cafe.like.domain;

import com.example.fan_cafe.global.common.BaseTimeEntity;
import com.example.fan_cafe.post.domain.Post;
import com.example.fan_cafe.user.domain.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
@Table(
        name = "likes",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "target_id", "target_type"})
        }
)
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Long targetId;

    @Enumerated(EnumType.STRING)
    private LikeTargetType targetType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    public static Like of(User user, LikeTargetType targetType, Long targetId) {
        return Like.builder()
                .user(user)
                .targetType(targetType)
                .targetId(targetId)
                .build();
    }
}
