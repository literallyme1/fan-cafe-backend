package com.example.fan_cafe.campaign.application;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.exception.GlobalErrorCode;
import com.example.fan_cafe.outbox.domain.OutboxEvent;
import com.example.fan_cafe.outbox.infrastructure.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
class CampaignOutboxService {
    static final String CAMPAIGN_SUCCEEDED = "CAMPAIGN_SUCCEEDED";

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    void saveSuccess(Long campaignId, Long receiverId, String title) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", CAMPAIGN_SUCCEEDED);
        payload.put("campaignId", campaignId);
        payload.put("receiverId", receiverId);
        payload.put("message", "Campaign '" + title + "' 목표가 달성되었습니다.");
        persist(OutboxEvent.init("CAMPAIGN", campaignId, serialize(payload)));
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException failure) {
            throw new CustomException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private void persist(OutboxEvent event) {
        OutboxEvent saved = outboxRepository.save(event);
        outboxRepository.flush();
        saved.assignEventIdFromPrimaryKey();
        outboxRepository.save(saved);
    }
}
