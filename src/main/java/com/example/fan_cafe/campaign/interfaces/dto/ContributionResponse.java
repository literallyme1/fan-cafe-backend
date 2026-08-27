package com.example.fan_cafe.campaign.interfaces.dto;

import com.example.fan_cafe.campaign.domain.Contribution;
import com.example.fan_cafe.campaign.domain.ContributionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ContributionResponse(
        Long contributionId,
        Long campaignId,
        Long orderId,
        BigDecimal amount,
        ContributionStatus status,
        LocalDateTime reservedAt,
        LocalDateTime approvedAt,
        LocalDateTime confirmedAt,
        LocalDateTime refundedAt
) {
    public static ContributionResponse from(Contribution contribution) {
        return new ContributionResponse(
                contribution.getId(), contribution.getCampaign().getId(), contribution.getOrder().getId(),
                contribution.getAmount(), contribution.getStatus(), contribution.getReservedAt(),
                contribution.getApprovedAt(), contribution.getConfirmedAt(), contribution.getRefundedAt());
    }
}
