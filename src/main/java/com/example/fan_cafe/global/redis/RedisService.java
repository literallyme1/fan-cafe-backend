package com.example.fan_cafe.global.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisService {

    private final StringRedisTemplate stringRedisTemplate;

    public void set(String key, String value, Duration ttl) {
        stringRedisTemplate.opsForValue().set(key, value, ttl);
    }

    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    public int getInt(String key) {
        try {
            String v = stringRedisTemplate.opsForValue().get(key);
            return v == null ? 0 : Integer.parseInt(v);
        } catch (Exception e) {
            log.warn("[CACHE GET ERROR] key={}", key);
            return 0;
        }
    }
    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }

    public void increaseCount(String key) {
        try {
            stringRedisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            log.warn("[CACHE INCR ERROR] key={} not increased", key, e);
        }
    }

    public void recordCommentCountChangedPost(Long postId) {
        try {
            String setKey = RedisKeyUtil.getCommentCountPostSetKey();
            stringRedisTemplate.opsForSet().add(setKey, postId.toString());
        }
            catch (Exception e) {
                log.warn(
                        "[REDIS COMMENT COUNT TRACKING FAILED] postId={}, key={}",
                        postId,
                        RedisKeyUtil.getCommentCountPostSetKey(),
                        e
                );
            }
    }

    public void removeFromCommentCountSyncTarget(String postIdStr) {
        String key = RedisKeyUtil.getCommentCountPostSetKey();
        stringRedisTemplate.opsForSet().remove(key, postIdStr);
    }

    public Set<String> getCommentCountChangedPosts(){
        String postSetKey = RedisKeyUtil.getCommentCountPostSetKey();
        return stringRedisTemplate.opsForSet().members(postSetKey);
    }
}
