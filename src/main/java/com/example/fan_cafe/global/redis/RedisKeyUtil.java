package com.example.fan_cafe.global.redis;

public class RedisKeyUtil {

    public static String postDataKey(Long postId) {
        return "post:" + postId + ":data";
    }

    public static String getLatestPostListKey(int size) {
        return "post:list:latest:size:" + size;
    }

}
