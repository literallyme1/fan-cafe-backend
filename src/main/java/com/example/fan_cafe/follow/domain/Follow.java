package com.example.fan_cafe.follow.domain;


import com.example.fan_cafe.global.common.BaseTimeEntity;
import com.example.fan_cafe.user.domain.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
@Table(name = "follow")
public class Follow extends BaseTimeEntity {

    @EmbeddedId
    private FollowId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("followerId")
    @JoinColumn(name = "follower_id")
    private User follower;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("followingId")
    @JoinColumn(name = "following_id")
    private User following;

    private Follow(FollowId id) {
        this.id = id;
    }

    public static Follow create(Long followerId, Long followingId, FollowPolicy policy) {
        policy.validate(followerId, followingId);
        return new Follow(new FollowId(followerId, followingId));
    }
}