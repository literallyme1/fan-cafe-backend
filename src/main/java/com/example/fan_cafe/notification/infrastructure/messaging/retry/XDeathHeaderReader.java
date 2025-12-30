package com.example.fan_cafe.notification.infrastructure.messaging.retry;

import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class XDeathHeaderReader {

    //메세지가 지금까지 몇 번 실패했는 지 읽음.
    @SuppressWarnings("unchecked")
    public int getRetryCount(Message message) {
        Object xDeath = message.getMessageProperties().getHeaders().get("x-death");

        //xDeath 가 list 면 자동 캐스팅
        if(!(xDeath instanceof List<?> list) || list.isEmpty()) return 0;

        //첫번째 정보 Get
        Map<String, Object> deathInfo = (Map<String, Object>) list.getFirst();

        //Count 반환 (count 꺼내고 없으면 0 반환), Long -> int
        return ((Long) deathInfo.getOrDefault("count", 0L)).intValue();
    }
}
