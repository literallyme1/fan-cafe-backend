package com.example.fan_cafe.post.application;

import com.example.fan_cafe.global.redis.RedisService;
import com.example.fan_cafe.post.infrastructure.PostRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentCountSyncService {

    private final PostRepository postRepository;
    private final RedisService redisService;

    @Transactional
    public void syncToDatabase(Long postId, String countKey) {
        //Redis 증가분 조회
        int extraCount = redisService.getInt(countKey);
        if (extraCount <= 0) {
        }
        postRepository.increaseCommentCount(postId, extraCount);
    }
}
