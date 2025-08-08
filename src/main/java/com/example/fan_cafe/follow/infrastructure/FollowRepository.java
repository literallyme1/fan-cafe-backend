package com.example.fan_cafe.follow.infrastructure;

import com.example.fan_cafe.follow.domain.Follow;
import com.example.fan_cafe.follow.domain.FollowId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FollowRepository extends JpaRepository<Follow, FollowId>, FollowQueryRepository  {

    boolean existsById(FollowId id);
    void deleteById(FollowId id);

    @Query("""
            select (count(f) > 0) from Follow f
            where f.id.followerId = :followerId and f.id.followingId = :followingId
            """)
    boolean exists(Long followerId, Long followingId);

    long countById_FollowingId(Long userId);
    long countById_FollowerId(Long userId);
}
