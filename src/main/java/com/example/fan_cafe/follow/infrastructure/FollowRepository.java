package com.example.fan_cafe.follow.infrastructure;

import com.example.fan_cafe.follow.domain.Follow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long>, FollowRepositoryCustom  {

    Optional<Follow> findByFollower_IdAndFollowing_Id(Long followerId, Long followingId);
    Optional<Follow> findTopByOrderByCreatedAtDesc();

    boolean existsByFollower_IdAndFollowing_Id(Long followerId, Long followingId);

}
