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
public class SagaOrderCompletionService {
    private final OrderPaymentCommandService orderPaymentCommandService;
    private final SagaTransactionService sagaTransactionService;

    @Transactional
    public OrderQueryResponse complete(UUID sagaId, Long orderId, String historyReason) {
        OrderQueryResponse response = orderPaymentCommandService.applyPaymentApproved(orderId, historyReason);
        sagaTransactionService.transition(sagaId, SagaStatus.COMPLETED);
        return response;
    }
}
