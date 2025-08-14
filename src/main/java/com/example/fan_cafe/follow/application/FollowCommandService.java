package com.example.fan_cafe.follow.application;


import com.example.fan_cafe.follow.domain.Follow;
import com.example.fan_cafe.follow.domain.FollowId;
import com.example.fan_cafe.follow.domain.FollowPolicy;
import com.example.fan_cafe.follow.infrastructure.FollowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FollowCommandService {
    private final FollowRepository followRepository;
    @Transactional
    public void follow(Long followerId, Long targetId){
        FollowPolicy policy = new FollowPolicy(
                followRepository::exists,
                null
        );

        Follow entity = Follow.create(followerId, targetId, policy);
        followRepository.save(entity);
        // TODO: 이벤트 발행 (알림/피드)
    }

    @Transactional
    public void unfollow(Long followerId, Long targetId) {
        followRepository.deleteById(new FollowId(followerId, targetId));
        // TODO: 이벤트 발행
    }
}
