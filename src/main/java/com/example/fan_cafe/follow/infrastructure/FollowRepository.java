package com.example.fan_cafe.follow.infrastructure;

import com.example.fan_cafe.follow.domain.Follow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, FollowId>, FollowRepositoryCustom  {

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);
    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);
    Optional<Follow> findTopByOrderByCreatedAtDesc();
//    @Query("""
//            select (count(f) > 0) from Follow f
//            where f.follower.id = :followerId and f.following.id = :followingId
//            """)
//    boolean exists(Long followerId, Long followingId);

    boolean existsByFollower_IdAndFollowing_Id(Long followerId, Long followingId);

}
