package com.example.fan_cafe.campaign.domain;

import com.example.fan_cafe.campaign.exception.CampaignErrorCode;
import com.example.fan_cafe.global.common.BaseTimeEntity;
import com.example.fan_cafe.global.exception.CustomException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "campaigns",
        indexes = @Index(
                name = "idx_campaign_deadline",
                columnList = "status, deadline_at, id"
        )
)
@Check(constraints = "target_amount >= 1000 AND MOD(target_amount, 1000) = 0 "
        + "AND funded_amount >= 0 AND reserved_amount >= 0 "
        + "AND funded_amount + reserved_amount <= target_amount")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Campaign extends BaseTimeEntity {
    private static final BigDecimal AMOUNT_UNIT = BigDecimal.valueOf(1_000);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "target_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal targetAmount;

    @Column(name = "funded_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal fundedAmount;

    @Column(name = "reserved_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal reservedAmount;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "deadline_at", nullable = false)
    private LocalDateTime deadlineAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CampaignStatus status;

    @Column(name = "succeeded_at")
    private LocalDateTime succeededAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    private Campaign(
            String title,
            String content,
            BigDecimal targetAmount,
            LocalDateTime startsAt,
            LocalDateTime deadlineAt
    ) {
        this.title = title;
        this.content = content;
        this.targetAmount = targetAmount;
        this.fundedAmount = BigDecimal.ZERO;
        this.reservedAmount = BigDecimal.ZERO;
        this.startsAt = startsAt;
        this.deadlineAt = deadlineAt;
        this.status = CampaignStatus.OPEN;
    }

    public static Campaign open(
            String title,
            String content,
            BigDecimal targetAmount,
            LocalDateTime startsAt,
            LocalDateTime deadlineAt
    ) {
        validateAmount(targetAmount);
        if (startsAt == null || deadlineAt == null || !startsAt.isBefore(deadlineAt)) {
            throw new CustomException(CampaignErrorCode.INVALID_PERIOD);
        }
        return new Campaign(title, content, targetAmount, startsAt, deadlineAt);
    }

    public void reserve(BigDecimal amount, LocalDateTime requestedAt) {
        validateAmount(amount);
        if (status != CampaignStatus.OPEN
                || requestedAt == null
                || requestedAt.isBefore(startsAt)
                || !requestedAt.isBefore(deadlineAt)) {
            throw new CustomException(CampaignErrorCode.CAMPAIGN_NOT_OPEN);
        }
        if (fundedAmount.add(reservedAmount).add(amount).compareTo(targetAmount) > 0) {
            throw new CustomException(CampaignErrorCode.TARGET_AMOUNT_EXCEEDED);
        }
        reservedAmount = reservedAmount.add(amount);
    }

    public void confirmReservation(BigDecimal amount) {
        validateAmount(amount);
        if (reservedAmount.compareTo(amount) < 0) {
            throw new CustomException(CampaignErrorCode.INVALID_CAMPAIGN_STATE);
        }
        reservedAmount = reservedAmount.subtract(amount);
        fundedAmount = fundedAmount.add(amount);
    }

    public void releaseReservation(BigDecimal amount) {
        validateAmount(amount);
        if (reservedAmount.compareTo(amount) < 0) {
            throw new CustomException(CampaignErrorCode.INVALID_CAMPAIGN_STATE);
        }
        reservedAmount = reservedAmount.subtract(amount);
    }

    public void subtractRefundedAmount(BigDecimal amount) {
        validateAmount(amount);
        if (fundedAmount.compareTo(amount) < 0) {
            throw new CustomException(CampaignErrorCode.INVALID_CAMPAIGN_STATE);
        }
        fundedAmount = fundedAmount.subtract(amount);
    }

    public boolean isTargetReached() {
        return fundedAmount.compareTo(targetAmount) == 0;
    }

    public void markSuccess(LocalDateTime succeededAt) {
        if (status != CampaignStatus.OPEN || !isTargetReached()) {
            throw new CustomException(CampaignErrorCode.INVALID_CAMPAIGN_STATE);
        }
        this.status = CampaignStatus.SUCCESS;
        this.succeededAt = succeededAt;
    }

    public void markFailed(LocalDateTime failedAt) {
        if (status != CampaignStatus.OPEN || isTargetReached()) {
            throw new CustomException(CampaignErrorCode.INVALID_CAMPAIGN_STATE);
        }
        this.status = CampaignStatus.FAILED;
        this.failedAt = failedAt;
    }

    public void startRefunding() {
        if (status != CampaignStatus.FAILED) {
            throw new CustomException(CampaignErrorCode.INVALID_CAMPAIGN_STATE);
        }
        this.status = CampaignStatus.REFUNDING;
    }

    public void failAndStartRefunding(LocalDateTime failedAt) {
        markFailed(failedAt);
        startRefunding();
    }

    public void completeRefunding(LocalDateTime refundedAt) {
        if (status != CampaignStatus.REFUNDING) {
            throw new CustomException(CampaignErrorCode.INVALID_CAMPAIGN_STATE);
        }
        this.status = CampaignStatus.REFUNDED;
        this.refundedAt = refundedAt;
    }

    private static void validateAmount(BigDecimal amount) {
        if (amount == null
                || amount.compareTo(AMOUNT_UNIT) < 0
                || amount.remainder(AMOUNT_UNIT).compareTo(BigDecimal.ZERO) != 0) {
            throw new CustomException(CampaignErrorCode.INVALID_AMOUNT);
        }
    }
}
