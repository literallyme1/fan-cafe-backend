package com.example.fan_cafe.follow.infrastructure;

import com.example.fan_cafe.follow.interfaces.dto.FollowerItemResponse;
import com.example.fan_cafe.global.common.Cursor;

import java.time.LocalDateTime;
import java.util.List;

public interface FollowQueryRepository {

    List<FollowerItemResponse> findFollowers(Long targetId, Long viewerId,
                                             Cursor cursor, int size);
}
