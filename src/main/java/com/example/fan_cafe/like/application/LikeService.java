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


    public boolean toggleLike(User user, Long targetId, LikeTargetType targetType) {

        //기존 like 존재 여부 확인
        Optional<Like> alreadyLiked = likeRepository.findByTargetIdAndTargetTypeAndUserId(targetId, targetType, user);

        Like newLike = Like.of(user, targetType, targetId);

        boolean liked;
        if(alreadyLiked.isPresent()){
            likeRepository.delete(alreadyLiked.get());
            liked = false;
        }else{
            try {
                likeRepository.save(newLike);
                liked = true;
            } catch (DataIntegrityViolationException e) {
                throw new CustomException(LikeErrorCode.ALREADY_LIKED);
            }
        }

        return liked;
    }
}
