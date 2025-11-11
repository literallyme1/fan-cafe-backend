package com.example.fan_cafe.follow.infrastructure;

import com.example.fan_cafe.follow.domain.Follow;
import com.example.fan_cafe.follow.domain.FollowId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, FollowId>, FollowRepositoryCustom  {

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);
    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);
    Optional<Follow> findTopByOrderByCreatedAtDesc();
    @Query("""
            select (count(f) > 0) from Follow f
            where f.follower.id = :followerId and f.following.id = :followingId
            """)
    boolean exists(Long followerId, Long followingId);

}
