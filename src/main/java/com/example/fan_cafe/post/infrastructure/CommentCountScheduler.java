package com.example.fan_cafe.post.infrastructure;

import com.example.fan_cafe.global.redis.RedisKeyUtil;
import com.example.fan_cafe.global.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommentCountScheduler {

    private final StringRedisTemplate stringRedisTemplate;
    private final PostRepository postRepository;
    private final RedisService redisService;

    @Scheduled(fixedDelay = 5000)
    public void syncCommentCount() {

        //post 목록 조회
        Set<String> postIds = redisService.getCommentCountChangedPosts();
        if (postIds == null || postIds.isEmpty()) {
            return;
        }

        for (String postIdStr : postIds) {
            Long postId = Long.valueOf(postIdStr);
            String countKey = RedisKeyUtil.getCommentCountKey(postId);

            //Redis 증가분 조회
            int extraCount = redisService.getInt(countKey);

            if (extraCount <= 0) {
                //DB에 옮길 데이터 X -> 목록에서만 뺌.
                redisService.removeFromCommentCountSyncTarget(postIdStr);
                continue;
            }

            //DB 반영
            postRepository.increaseCommentCount(postId, extraCount);

            //redis 정리
            redisService.delete(countKey); //post의 댓글 증가분 삭제
            redisService.removeFromCommentCountSyncTarget(postIdStr); //postSetKey라는 Set에서 postIdStr라는 값을 하나 제거
        }
    }
}
