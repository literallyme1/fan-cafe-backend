package com.example.fan_cafe.order.saga.application;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.exception.GlobalErrorCode;
import com.example.fan_cafe.order.saga.messaging.ApprovePaymentCommand;
import com.example.fan_cafe.outbox.domain.OutboxEvent;
import com.example.fan_cafe.outbox.infrastructure.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PaymentApprovalOutboxService {
    public static final String AGGREGATE_TYPE = "PAYMENT_APPROVAL";

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void save(
            Long orderId,
            BigDecimal expectedAmount,
            BigDecimal approvalAmount,
            String paymentKey
    ) {
        ApprovePaymentCommand command = ApprovePaymentCommand.of(
                orderId, expectedAmount, approvalAmount, paymentKey);
        OutboxEvent saved = outboxRepository.save(OutboxEvent.init(
                AGGREGATE_TYPE, orderId, serialize(command)));
        outboxRepository.flush();
        saved.assignEventIdFromPrimaryKey();
        outboxRepository.save(saved);
    }

    private String serialize(ApprovePaymentCommand command) {
        try {
            return objectMapper.writeValueAsString(command);
        } catch (JsonProcessingException failure) {
            throw new CustomException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
