package com.example.fan_cafe.global.redis;

import java.time.Duration;

public class CacheTTL {

    public static final Duration POST_LIST_LATEST = Duration.ofMinutes(3);

    private CacheTTL() {}
}
