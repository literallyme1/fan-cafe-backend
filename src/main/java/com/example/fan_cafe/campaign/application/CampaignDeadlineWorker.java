package com.example.fan_cafe.campaign.application;

import com.example.fan_cafe.campaign.domain.ContributionStatus;
import com.example.fan_cafe.campaign.infrastructure.CampaignRepository;
import com.example.fan_cafe.campaign.infrastructure.ContributionRepository;
import com.example.fan_cafe.order.saga.application.SagaCompensationService;
import com.example.fan_cafe.order.saga.domain.SagaCompensationReason;
import com.example.fan_cafe.order.saga.infrastructure.SagaInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class CampaignDeadlineWorker {
    private final CampaignDeadlineTransactionService deadlineTransactionService;
    private final CampaignRepository campaignRepository;
    private final ContributionRepository contributionRepository;
    private final SagaInstanceRepository sagaRepository;
    private final SagaCompensationService compensationService;
    private final Clock clock;

    public void processDueCampaigns(int batchSize) {
        for (int processed = 0; processed < batchSize; processed++) {
            if (deadlineTransactionService.closeNext(LocalDateTime.now(clock)).isEmpty()) {
                break;
            }
        }
        campaignRepository.findRefundingCampaignIds().forEach(this::startOutstandingRefunds);
    }

    private void startOutstandingRefunds(Long campaignId) {
        contributionRepository.findOrderIdsByCampaignIdAndStatus(
                        campaignId, ContributionStatus.CONFIRMED)
                .forEach(orderId -> {
                    try {
                        sagaRepository.findByOrderId(orderId).ifPresent(saga ->
                                compensationService.startCampaign(
                                        saga.getSagaId(), orderId,
                                        SagaCompensationReason.CAMPAIGN_FAILED.name(), null));
                    } catch (RuntimeException failure) {
                        log.warn("[CAMPAIGN] failed to start refund campaignId={}, orderId={}",
                                campaignId, orderId, failure);
                    }
                });
    }
}
