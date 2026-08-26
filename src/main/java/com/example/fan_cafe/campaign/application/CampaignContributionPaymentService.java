package com.example.fan_cafe.campaign.application;

import com.example.fan_cafe.campaign.domain.Campaign;
import com.example.fan_cafe.campaign.domain.Contribution;
import com.example.fan_cafe.campaign.domain.ContributionStatus;
import com.example.fan_cafe.campaign.exception.CampaignApprovalAfterDeadlineException;
import com.example.fan_cafe.campaign.exception.CampaignErrorCode;
import com.example.fan_cafe.campaign.infrastructure.CampaignRepository;
import com.example.fan_cafe.campaign.infrastructure.ContributionRepository;
import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.order.exception.OrderErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CampaignContributionPaymentService {
    private final ContributionRepository contributionRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignOutboxService campaignOutboxService;
    private final Clock clock;

    public void confirmIfCampaignOrder(Long orderId, LocalDateTime approvedAt) {
        Contribution contribution = contributionRepository.findByOrderIdForUpdate(orderId).orElse(null);
        if (contribution == null) {
            return;
        }
        if (contribution.getStatus() == ContributionStatus.CONFIRMED) {
            return;
        }
        if (contribution.getStatus() != ContributionStatus.RESERVED) {
            throw new CustomException(CampaignErrorCode.INVALID_CONTRIBUTION_STATE);
        }
        if (approvedAt == null) {
            throw new CustomException(OrderErrorCode.PAYMENT_SERVICE_ERROR);
        }

        Campaign campaign = campaignRepository.findByIdForUpdate(contribution.getCampaign().getId())
                .orElseThrow(() -> new CustomException(CampaignErrorCode.CAMPAIGN_NOT_FOUND));
        if (approvedAt.isAfter(campaign.getDeadlineAt())) {
            throw new CampaignApprovalAfterDeadlineException(approvedAt);
        }

        campaign.confirmReservation(contribution.getAmount());
        contribution.confirm(approvedAt, LocalDateTime.now(clock));
        if (campaign.isTargetReached()) {
            campaign.markSuccess(LocalDateTime.now(clock));
            contributionRepository.findDistinctUserIdsByCampaignIdAndStatus(
                            campaign.getId(), ContributionStatus.CONFIRMED)
                    .forEach(userId -> campaignOutboxService.saveSuccess(
                            campaign.getId(), userId, campaign.getTitle()));
        }
    }

    public void failIfCampaignOrder(Long orderId) {
        Contribution contribution = contributionRepository.findByOrderIdForUpdate(orderId).orElse(null);
        if (contribution == null || contribution.getStatus() == ContributionStatus.FAILED) {
            return;
        }
        if (contribution.getStatus() != ContributionStatus.RESERVED) {
            throw new CustomException(CampaignErrorCode.INVALID_CONTRIBUTION_STATE);
        }
        Campaign campaign = campaignRepository.findByIdForUpdate(contribution.getCampaign().getId())
                .orElseThrow(() -> new CustomException(CampaignErrorCode.CAMPAIGN_NOT_FOUND));
        campaign.releaseReservation(contribution.getAmount());
        contribution.fail(LocalDateTime.now(clock));
    }
}
