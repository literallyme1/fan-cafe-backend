package com.example.fan_cafe.like.infrastructure;

import com.example.fan_cafe.like.domain.Like;

import com.example.fan_cafe.like.domain.LikeTargetType;
import com.example.fan_cafe.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long>, LikeRepositoryCustom  {

    //사용자의 like 여부 확인
    //TODO : User FK 를 지운 후 User 대신 UserId 로 검색
    Optional<Like> findByTargetIdAndTargetTypeAndUserId(
            Long targetId,
            LikeTargetType targetType,
            User user
    );
}
