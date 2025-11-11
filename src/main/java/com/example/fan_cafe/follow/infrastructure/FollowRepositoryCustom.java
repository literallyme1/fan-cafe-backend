package com.example.fan_cafe.follow.infrastructure;

import com.example.fan_cafe.follow.interfaces.dto.FollowResponse;
import com.example.fan_cafe.global.common.Cursor;

import java.util.List;

public interface FollowRepositoryCustom {

//    List<FollowerItemResponse> findFollowers(Long targetId, Long viewerId,
//                                             Cursor cursor, int size);

    List<FollowResponse> findNextFollowingPage(Cursor cursor, int size, Long userId);
}
