package com.example.fan_cafe.global.event;

import java.util.UUID;

public class eventIdGenerator {

    //eventID 생성
    public static String generate() {
        return UUID.randomUUID().toString();
    }
}
