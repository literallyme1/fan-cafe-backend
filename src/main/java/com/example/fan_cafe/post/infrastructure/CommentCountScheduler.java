package com.example.fan_cafe.post.infrastructure;

import com.example.fan_cafe.global.config.SchedulerProperties;
import com.example.fan_cafe.global.redis.RedisKeyUtil;
import com.example.fan_cafe.global.redis.RedisLockManager;
import com.example.fan_cafe.global.redis.RedisService;
import com.example.fan_cafe.post.application.CommentCountSyncService;
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

    private final RedisService redisService;
    private final CommentCountSyncService syncService;
    private final RedisLockManager redisLockManager;

    @Scheduled(fixedDelay = SchedulerProperties.COMMENT_COUNT_DELAY_MS)
    public void syncCommentCount() {
        log.info("[SCHEDULER] syncCommentCount tick");
        String lockKey = "comment_count:scheduler:lock";
        //락 획득해야 실행 가능
        if (!redisLockManager.tryLock(lockKey, 10)) {
            return;
        }

        try {
            //post 목록 조회
            Set<String> postIds = redisService.getCommentCountChangedPosts();
            if (postIds == null || postIds.isEmpty()) {
                return;
            }

            for (String postIdStr : postIds) {
                syncOnePost(postIdStr);
            }
        } finally {
            redisLockManager.unlock(lockKey);
        }
    }

    private void syncOnePost(String postIdStr) {
        Long postId = Long.valueOf(postIdStr);
        String countKey = RedisKeyUtil.getCommentCountKey(postId);
        try {
            //db
            syncService.syncToDatabase(postId, countKey);
            //redis
            redisService.delete(countKey); //post의 댓글 증가분 삭제
            redisService.removeFromCommentCountSyncTarget(postIdStr);//postSetKey라는 Set에서 postIdStr라는 값을 하나 제거
        } catch (Exception e) {
            log.error("[COMMENT COUNT SYNC FAIL] postId={}", postId, e);
        }
    }

}

