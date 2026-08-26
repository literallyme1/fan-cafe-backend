package com.example.fan_cafe.campaign.application;

import com.example.fan_cafe.campaign.exception.CampaignErrorCode;
import com.example.fan_cafe.campaign.infrastructure.ContributionRepository;
import com.example.fan_cafe.campaign.interfaces.dto.ContributionCreateRequest;
import com.example.fan_cafe.campaign.interfaces.dto.ContributionResponse;
import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.order.saga.application.PaymentSagaOrchestrator;
import com.example.fan_cafe.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CampaignContributionService {
    private final CampaignContributionReservationService reservationService;
    private final ContributionRepository contributionRepository;
    private final PaymentSagaOrchestrator paymentSagaOrchestrator;

    public ContributionResponse contribute(User user, Long campaignId, ContributionCreateRequest request) {
        CampaignContributionReservation reservation = reservationService.reserve(
                user, campaignId, request.amount());
        paymentSagaOrchestrator.approve(
                reservation.orderId(), request.amount(), request.amount(), request.paymentKey());
        return get(reservation.contributionId());
    }

    @Transactional(readOnly = true)
    public ContributionResponse get(Long contributionId) {
        return contributionRepository.findById(contributionId)
                .map(ContributionResponse::from)
                .orElseThrow(() -> new CustomException(CampaignErrorCode.CONTRIBUTION_NOT_FOUND));
    }
}
