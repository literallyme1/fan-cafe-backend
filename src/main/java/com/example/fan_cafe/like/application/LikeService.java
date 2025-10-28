package com.example.fan_cafe.like.application;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.like.domain.Like;
import com.example.fan_cafe.like.domain.LikeTargetType;
import com.example.fan_cafe.like.exception.LikeErrorCode;
import com.example.fan_cafe.like.infrastructure.LikeRepository;
import com.example.fan_cafe.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;

    @Transactional
    public void toggleLike(User user, Long targetId, LikeTargetType targetType) {

        Optional<Like> alreadyLiked = likeRepository.findByTargetIdAndTargetTypeAndUserId(targetId, targetType, user);

        Like newLike = Like.of(user, targetType, targetId);

        alreadyLiked.ifPresentOrElse(
                likeRepository::delete,
                () -> {
                    try {
                        likeRepository.save(newLike);
                    } catch (DataIntegrityViolationException e) {
                        throw new CustomException(LikeErrorCode.ALREADY_LIKED);
                    }
                }
        );
    }
}
