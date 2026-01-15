package com.example.fan_cafe.global.logging;

import java.util.UUID;

public class TraceIdGenerator {
    public static String generate() {
        //UUID 길어서 앞단만 사용
        return UUID.randomUUID().toString().substring(0,8);
    }
}
