package com.example.fan_cafe.like.infrastructure;

import com.example.fan_cafe.like.interfaces.dto.LikeResponse;
import com.example.fan_cafe.user.domain.User;

import java.util.List;
import java.util.Set;

public interface LikeRepositoryCustom {

    Set<Long> findLikedPostIds(Long userId, List<Long> postIds);
}
