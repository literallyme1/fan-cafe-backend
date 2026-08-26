package com.example.fan_cafe.campaign.application;

import com.example.fan_cafe.campaign.domain.Campaign;
import com.example.fan_cafe.campaign.domain.CampaignStatus;
import com.example.fan_cafe.campaign.domain.Contribution;
import com.example.fan_cafe.campaign.domain.ContributionStatus;
import com.example.fan_cafe.campaign.exception.CampaignErrorCode;
import com.example.fan_cafe.campaign.infrastructure.CampaignRepository;
import com.example.fan_cafe.campaign.infrastructure.ContributionRepository;
import com.example.fan_cafe.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CampaignRefundStateService {
    private final ContributionRepository contributionRepository;
    private final CampaignRepository campaignRepository;
    private final Clock clock;

    public void begin(Long orderId, LocalDateTime approvedAt) {
        Contribution contribution = contributionRepository.findByOrderIdForUpdate(orderId).orElse(null);
        if (contribution == null
                || contribution.getStatus() == ContributionStatus.REFUNDING
                || contribution.getStatus() == ContributionStatus.REFUNDED) {
            return;
        }
        Campaign campaign = lockCampaign(contribution);
        LocalDateTime now = LocalDateTime.now(clock);
        if (contribution.getStatus() == ContributionStatus.RESERVED) {
            campaign.releaseReservation(contribution.getAmount());
            contribution.startLateApprovalRefund(approvedAt, now);
            return;
        }
        if (contribution.getStatus() == ContributionStatus.CONFIRMED) {
            campaign.subtractRefundedAmount(contribution.getAmount());
            contribution.startRefund(now);
            return;
        }
        throw new CustomException(CampaignErrorCode.INVALID_CONTRIBUTION_STATE);
    }

    public void validateUserRefund(Long orderId, Long campaignId, Long userId) {
        Contribution contribution = contributionRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new CustomException(CampaignErrorCode.CONTRIBUTION_NOT_FOUND));
        Campaign campaign = lockCampaign(contribution);
        if (!campaign.getId().equals(campaignId)
                || !contribution.getUser().getId().equals(userId)
                || campaign.getStatus() != CampaignStatus.OPEN
                || contribution.getStatus() != ContributionStatus.CONFIRMED) {
            throw new CustomException(CampaignErrorCode.CONTRIBUTION_NOT_REFUNDABLE);
        }
    }

    public void complete(Long orderId) {
        Contribution contribution = contributionRepository.findByOrderIdForUpdate(orderId).orElse(null);
        if (contribution == null || contribution.getStatus() == ContributionStatus.REFUNDED) {
            return;
        }
        if (contribution.getStatus() != ContributionStatus.REFUNDING) {
            throw new CustomException(CampaignErrorCode.INVALID_CONTRIBUTION_STATE);
        }
        Campaign campaign = lockCampaign(contribution);
        contribution.completeRefund(LocalDateTime.now(clock));
        if (campaign.getStatus() == CampaignStatus.REFUNDING
                && !contributionRepository.existsByCampaignIdAndStatusIn(
                campaign.getId(), List.of(ContributionStatus.CONFIRMED, ContributionStatus.REFUNDING))) {
            campaign.completeRefunding(LocalDateTime.now(clock));
        }
    }

    private Campaign lockCampaign(Contribution contribution) {
        return campaignRepository.findByIdForUpdate(contribution.getCampaign().getId())
                .orElseThrow(() -> new CustomException(CampaignErrorCode.CAMPAIGN_NOT_FOUND));
    }
}
