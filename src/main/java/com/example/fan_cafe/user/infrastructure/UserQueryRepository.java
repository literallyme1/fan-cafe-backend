package com.example.fan_cafe.user.infrastructure;

import com.example.fan_cafe.follow.interfaces.dto.FollowerItemResponse;
import com.example.fan_cafe.global.common.Cursor;

import java.util.List;

public interface UserQueryRepository {

    boolean existsNickname(String nickname, Long id);
}
