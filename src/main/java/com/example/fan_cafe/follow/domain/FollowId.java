package com.example.fan_cafe.follow.domain;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Embeddable
public class FollowId implements Serializable {
    private Long followerId;
    private Long followingId;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FollowId that)) return false;
        return Objects.equals(followerId, that.followerId)
                && Objects.equals(followingId, that.followingId);
    }

    @Override
    public int hashCode() { return Objects.hash(followerId, followingId); }
}
