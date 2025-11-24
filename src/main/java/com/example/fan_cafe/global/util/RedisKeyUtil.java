package com.example.fan_cafe.global.util;

public class RedisKeyUtil {

    public static String postData(Long postId) {
        return "post:" + postId + ":data";
    }

}
