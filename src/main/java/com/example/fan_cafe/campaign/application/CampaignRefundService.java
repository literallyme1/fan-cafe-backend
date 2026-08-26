package com.example.fan_cafe.campaign.application;

import com.example.fan_cafe.campaign.exception.CampaignErrorCode;
import com.example.fan_cafe.campaign.infrastructure.ContributionRepository;
import com.example.fan_cafe.campaign.interfaces.dto.ContributionResponse;
import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.order.saga.application.SagaCompensationService;
import com.example.fan_cafe.order.saga.infrastructure.SagaInstanceRepository;
import com.example.fan_cafe.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CampaignRefundService {
    private final ContributionRepository contributionRepository;
    private final SagaInstanceRepository sagaRepository;
    private final SagaCompensationService compensationService;

    public ContributionResponse requestUserRefund(User user, Long campaignId, Long contributionId) {
        Long orderId = contributionRepository.findOrderIdByIdAndUserId(contributionId, user.getId())
                .orElseThrow(() -> new CustomException(CampaignErrorCode.CONTRIBUTION_NOT_FOUND));
        var saga = sagaRepository.findByOrderId(orderId)
                .orElseThrow(() -> new CustomException(CampaignErrorCode.CONTRIBUTION_NOT_FOUND));
        compensationService.startCampaignUserRefund(
                saga.getSagaId(), orderId, campaignId, user.getId());
        return find(contributionId);
    }

    @Transactional(readOnly = true)
    public ContributionResponse find(Long contributionId) {
        return contributionRepository.findById(contributionId)
                .map(ContributionResponse::from)
                .orElseThrow(() -> new CustomException(CampaignErrorCode.CONTRIBUTION_NOT_FOUND));
    }
}
