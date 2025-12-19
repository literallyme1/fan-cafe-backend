package com.example.fan_cafe.comment.events;

import java.util.UUID;

public class CommentEventIdGenerator {

    //eventID 생성
    public static String generate() {
        return UUID.randomUUID().toString();
    }
}
