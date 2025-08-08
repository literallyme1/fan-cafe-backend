package com.example.fan_cafe.follow.infrastructure;

import com.example.fan_cafe.follow.interfaces.dto.FollowerItemResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface FollowQueryRepository {

    List<FollowerItemResponse> findFollowers(Long targetId, Long viewerId,
                                             LocalDateTime cursorAt, Long cursorId, int size);
}
