package com.example.fan_cafe.outbox.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxPayloadJson {

    public static final String EVENT_ID_FIELD = "eventId";

    private final ObjectMapper objectMapper;

    public String mergeEventId(String jsonPayload, String eventId) {
        try {
            ObjectNode node = (ObjectNode) objectMapper.readTree(jsonPayload);
            node.put(EVENT_ID_FIELD, eventId);
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            throw new IllegalStateException("outbox payload에 eventId를 병합할 수 없습니다", e);
        }
    }
}
