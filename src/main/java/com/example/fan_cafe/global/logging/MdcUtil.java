package com.example.fan_cafe.global.logging;

import org.slf4j.MDC;

public class MdcUtil {
    //MDC 접근 유틸
    private static final String USER_ID = "userId";

    public static void putUserId(Long userId) {
        if (userId != null) {
            MDC.put(USER_ID, String.valueOf(userId));
        }
    }

    public static void clearUserId() {
        MDC.remove(USER_ID);
    }
}
