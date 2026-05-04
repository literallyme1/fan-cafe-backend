package com.example.fan_cafe.outbox.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OutboxPayloadJson {

    public static final String EVENT_ID_FIELD = "eventId";

    private final ObjectMapper objectMapper;

    /** Slack·로그용으로 페이로드에서 eventId를 읽을 때 사용한다(파싱 실패 시 empty). */
    public Optional<String> tryExtractEventId(String jsonPayload) {
        try {
            JsonNode node = objectMapper.readTree(jsonPayload);
            if (node.hasNonNull(EVENT_ID_FIELD)) {
                return Optional.of(node.get(EVENT_ID_FIELD).asText());
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }

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
