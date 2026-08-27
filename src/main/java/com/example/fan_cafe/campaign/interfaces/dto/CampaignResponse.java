package com.example.fan_cafe.campaign.interfaces.dto;

import com.example.fan_cafe.campaign.domain.Campaign;
import com.example.fan_cafe.campaign.domain.CampaignStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CampaignResponse(
        Long campaignId,
        String title,
        String content,
        BigDecimal targetAmount,
        BigDecimal fundedAmount,
        BigDecimal reservedAmount,
        LocalDateTime startsAt,
        LocalDateTime deadlineAt,
        CampaignStatus status
) {
    public static CampaignResponse from(Campaign campaign) {
        return new CampaignResponse(
                campaign.getId(), campaign.getTitle(), campaign.getContent(),
                campaign.getTargetAmount(), campaign.getFundedAmount(), campaign.getReservedAmount(),
                campaign.getStartsAt(), campaign.getDeadlineAt(), campaign.getStatus());
    }
}
