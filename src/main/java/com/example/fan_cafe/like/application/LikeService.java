package com.example.fan_cafe.like.application;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.like.domain.Like;
import com.example.fan_cafe.like.exception.LikeErrorCode;
import com.example.fan_cafe.like.infrastructure.LikeRepository;
import com.example.fan_cafe.like.interfaces.dto.LikeListResponse;
import com.example.fan_cafe.like.interfaces.dto.LikeResponse;
import com.example.fan_cafe.post.application.PostHelper;
import com.example.fan_cafe.post.domain.Post;
import com.example.fan_cafe.post.exception.PostErrorCode;
import com.example.fan_cafe.post.infrastructure.PostRepository;
import com.example.fan_cafe.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final PostHelper postHelper;

    @Transactional
    public LikeResponse like(User user, Long postId){

        Post post = postHelper.findByIdOrThrow(postId);
        Like like = Like.of(user, post);
        //중복 like 막기
        try {
            likeRepository.save(like);
            post.increaseLikeCount();
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(LikeErrorCode.ALREADY_LIKED);
        }
        return LikeResponse.from(like.getId(), true, post.getLikeCount());
    }

    @Transactional
    public LikeResponse unlike(User user, Long postId){
        Post post = postHelper.findByIdOrThrow(postId);
        Like like = likeRepository.findByUserAndPost(user, post)
                .orElseThrow(() -> new CustomException(LikeErrorCode.LIKED_NOT_FOUND));
        likeRepository.delete(like);
        post.decreaseLikeCount();
        return LikeResponse.from(post.getLikeCount());
    }

    public LikeListResponse get(User user){
        List<LikeResponse> likeDtos = likeRepository.findLikeResponsesByUser(user);
        return LikeListResponse.from(likeDtos);
    }
}
