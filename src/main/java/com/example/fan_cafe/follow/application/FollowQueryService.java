package com.example.fan_cafe.follow.application;

import com.example.fan_cafe.follow.infrastructure.FollowRepository;
import com.example.fan_cafe.follow.interfaces.dto.FollowerItemResponse;
import com.example.fan_cafe.follow.interfaces.dto.FollowerListResponse;
import com.example.fan_cafe.global.common.Cursor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FollowQueryService {
    private final FollowRepository followRepository;

    public FollowerListResponse getFollowers(Long targetId, Long viewerId,
                                             LocalDateTime cursorAt, Long cursorId, int size) {
        List<FollowerItemResponse> items =
                followRepository.findFollowers(targetId, viewerId, new Cursor(cursorId, cursorAt), size);
        boolean hasNext = items.size() > size;
        if (hasNext) items = items.subList(0, size);
        return new FollowerListResponse(items, hasNext);
    }
}