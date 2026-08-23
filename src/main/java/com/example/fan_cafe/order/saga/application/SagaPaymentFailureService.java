package com.example.fan_cafe.order.saga.application;

import com.example.fan_cafe.order.application.OrderPaymentCommandService;
import com.example.fan_cafe.order.interfaces.dto.OrderQueryResponse;
import com.example.fan_cafe.order.saga.domain.SagaStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SagaPaymentFailureService {
    private final OrderPaymentCommandService orderPaymentCommandService;
    private final SagaTransactionService sagaTransactionService;

    @Transactional
    public OrderQueryResponse fail(UUID sagaId, Long orderId, String reason) {
        OrderQueryResponse response = orderPaymentCommandService.applyPaymentFailed(orderId, reason);
        sagaTransactionService.transition(sagaId, SagaStatus.CANCELLED);
        return response;
    }
}
