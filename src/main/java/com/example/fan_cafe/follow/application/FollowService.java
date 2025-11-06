package com.example.fan_cafe.follow.application;


import com.example.fan_cafe.follow.domain.Follow;
import com.example.fan_cafe.follow.domain.FollowId;
import com.example.fan_cafe.follow.domain.FollowPolicy;
import com.example.fan_cafe.follow.infrastructure.FollowRepository;
import com.example.fan_cafe.follow.interfaces.dto.FollowerItemResponse;
import com.example.fan_cafe.follow.interfaces.dto.FollowerListResponse;
import com.example.fan_cafe.global.common.Cursor;
import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.user.domain.User;
import com.example.fan_cafe.user.exception.UserErrorCode;
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
    private final FollowPolicy followPolicy;

    @Transactional
    public void follow(Long followerId, Long followingId){

        //user 가 실제 존재하는 지 확인 후 User 를 가져옴.
        long userCount = userRepository.countByIdIn(List.of(followerId, followingId));
        if(userCount < 2) { throw new CustomException(UserErrorCode.USER_NOT_FOUND);}

        //Follow 타당성 여부 확인
        followPolicy.validate(followerId, followingId);

        User follower = userRepository.getReferenceById(followerId);
        User following = userRepository.getReferenceById(followingId);
        Follow entity = Follow.create(follower, following);

        //각 followCount 를 증가시킨다.
        entity.getFollower().increaseFollowingCount(); //내가 following 함
        entity.getFollowing().increaseFollowerCount();

        //저장
        followRepository.save(entity);


        // TODO: 이벤트 발행 (알림/피드)
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
