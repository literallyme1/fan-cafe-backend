package com.example.fan_cafe.follow.domain;


import com.example.fan_cafe.follow.exception.FollowErrorCode;
import com.example.fan_cafe.global.exception.CustomException;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class FollowPolicy {

    private final ExistsFollowPort existsPort;
    private final BlockCheckPort blockPort;

    public void validate(Long followerId, Long followingId) {
        if (followerId.equals(followingId))
            throw new CustomException(FollowErrorCode.SELF_FOLLOW);
        if (existsPort.exists(followerId, followingId))
            throw new CustomException(FollowErrorCode.SELF_FOLLOW);
        if (blockPort != null && blockPort.isBlockedEitherSide(followerId, followingId))
            throw new CustomException(FollowErrorCode.SELF_FOLLOW);
    }

    public interface ExistsFollowPort { boolean exists(Long followerId, Long followingId); }
    public interface BlockCheckPort { boolean isBlockedEitherSide(Long a, Long b); }
}
