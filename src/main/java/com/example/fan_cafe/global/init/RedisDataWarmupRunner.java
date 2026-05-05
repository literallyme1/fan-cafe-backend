package com.example.fan_cafe.global.init;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "redis.warmup.enabled",
        havingValue = "true"
)// 테스트 프로필에서만 실행되도록 설정
@RequiredArgsConstructor
public class RedisDataWarmupRunner implements CommandLineRunner {

    private final StringRedisTemplate redisTemplate;
    // PostRepository가 있다면 사용하고, 없다면 1~10000 루프를 돌려도 됩니다.
    // private final PostRepository postRepository;

    @Override
    public void run(String... args) {
        System.out.println("🚀 [부하 테스트 준비] Redis 데이터 웜업 시작..");

        // 랜덤 숫자를 생성해줄 도구
        java.util.concurrent.ThreadLocalRandom random = java.util.concurrent.ThreadLocalRandom.current();

        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (int i = 1; i <= 10000; i++) {
                String key = "post:" + i + ":comment_count";

                // 0~50 사이의 랜덤 숫자를 문자열로 만듦. (comment count)
                String randomValue = String.valueOf(random.nextInt(0, 51));

                // 레디스에 저장
                connection.set(key.getBytes(), randomValue.getBytes());
            }
            return null;
        });

        System.out.println("✅ [완료] 10,000개 포스트에 랜덤한 댓글 증가분이 세팅되었습니다.");
    }
}