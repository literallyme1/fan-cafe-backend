package com.example.fan_cafe.global.redis;

public class RedisKeyUtil {

    public static String postDataKey(Long postId) {
        return "post:" + postId + ":data";
    }

    public static String getLatestPostListKey(int size) {
        return "post:list:latest:size:" + size;
    }
    public static String getCommentCountKey(Long postId) {
        return "post:" + postId + ":comment_count";
    }
    //댓글 수가 변경된 게시글 목록
    public static String getCommentCountPostSetKey() { return "comment_count:posts"; }

}
