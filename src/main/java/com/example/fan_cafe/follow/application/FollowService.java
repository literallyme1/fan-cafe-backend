package com.example.fan_cafe.follow.application;


import com.example.fan_cafe.follow.domain.Follow;
import com.example.fan_cafe.follow.domain.FollowId;
import com.example.fan_cafe.follow.domain.FollowPolicy;
import com.example.fan_cafe.follow.infrastructure.FollowRepository;
import com.example.fan_cafe.follow.interfaces.dto.FollowerItemResponse;
import com.example.fan_cafe.follow.interfaces.dto.FollowerListResponse;
import com.example.fan_cafe.global.common.Cursor;
import com.example.fan_cafe.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FollowService {
    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    @Transactional
    public void follow(Long followerId, Long targetId){

        //user 가 실제 존재하는 지 확인

//        Follow entity = Follow.create(followerId, targetId);
//        followRepository.save(entity);
        // TODO: 이벤트 발행 (알림/피드)
    }

    private boolean isValidateUser(Long followerId, Long targetId){

    }
    @Transactional
    public void unfollow(Long followerId, Long targetId) {
        followRepository.deleteById(new FollowId(followerId, targetId));
        // TODO: 이벤트 발행
    }

    public FollowerListResponse getFollowers(Long targetId, Long viewerId,
                                             LocalDateTime cursorAt, Long cursorId, int size) {
        List<FollowerItemResponse> items =
                followRepository.findFollowers(targetId, viewerId, new Cursor(cursorId, cursorAt), size);
        boolean hasNext = items.size() > size;
        if (hasNext) items = items.subList(0, size);
        return new FollowerListResponse(items, hasNext);
    }
}
