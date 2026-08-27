package com.example.fan_cafe.order.saga.application;

import com.example.fan_cafe.campaign.application.CampaignContributionPaymentService;
import com.example.fan_cafe.campaign.exception.CampaignApprovalAfterDeadlineException;
import com.example.fan_cafe.order.application.OrderPaymentCommandService;
import com.example.fan_cafe.order.interfaces.dto.OrderQueryResponse;
import com.example.fan_cafe.order.saga.domain.SagaStatus;
import com.example.fan_cafe.order.saga.exception.OrderCompletionFailedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SagaOrderCompletionService {
    private final OrderPaymentCommandService orderPaymentCommandService;
    private final SagaTransactionService sagaTransactionService;
    private final CampaignContributionPaymentService campaignContributionPaymentService;

    @Transactional
    public OrderQueryResponse complete(UUID sagaId, Long orderId, String historyReason) {
        return complete(sagaId, orderId, historyReason, null);
    }

    @Transactional
    public OrderQueryResponse complete(
            UUID sagaId,
            Long orderId,
            String historyReason,
            LocalDateTime approvedAt
    ) {
        try {
            OrderQueryResponse response = orderPaymentCommandService.applyPaymentApproved(orderId, historyReason);
            campaignContributionPaymentService.confirmIfCampaignOrder(orderId, approvedAt);
            sagaTransactionService.transition(sagaId, SagaStatus.COMPLETED);
            return response;
        } catch (CampaignApprovalAfterDeadlineException lateApproval) {
            throw lateApproval;
        } catch (RuntimeException completionFailure) {
            throw new OrderCompletionFailedException(completionFailure);
        }
    }
}
