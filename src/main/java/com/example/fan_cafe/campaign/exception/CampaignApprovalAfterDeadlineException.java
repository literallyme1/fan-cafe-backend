package com.example.fan_cafe.campaign.exception;

import java.time.LocalDateTime;

public class CampaignApprovalAfterDeadlineException extends RuntimeException {
    private final LocalDateTime approvedAt;

    public CampaignApprovalAfterDeadlineException(LocalDateTime approvedAt) {
        super(CampaignErrorCode.APPROVAL_AFTER_DEADLINE.getMessage());
        this.approvedAt = approvedAt;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }
}
