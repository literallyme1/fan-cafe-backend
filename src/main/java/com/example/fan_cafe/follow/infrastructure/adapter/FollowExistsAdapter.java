package com.example.fan_cafe.follow.infrastructure.adapter;


import com.example.fan_cafe.follow.domain.FollowPolicy;
import com.example.fan_cafe.follow.infrastructure.FollowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FollowExistsAdapter implements FollowPolicy.ExistsFollowPort {

    private final FollowRepository followRepository;
    @Override
    public boolean exists(Long followerId, Long followingId){
        return followRepository.existsByFollower_IdAndFollowing_Id(followerId, followingId);
    }

}
