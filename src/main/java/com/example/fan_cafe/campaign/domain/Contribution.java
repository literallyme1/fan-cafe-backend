package com.example.fan_cafe.campaign.domain;

import com.example.fan_cafe.campaign.exception.CampaignErrorCode;
import com.example.fan_cafe.global.common.BaseTimeEntity;
import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.order.domain.Order;
import com.example.fan_cafe.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "contributions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_contribution_order_id",
                columnNames = "order_id"
        ),
        indexes = {
                @Index(name = "idx_contribution_campaign_status", columnList = "campaign_id, status, id"),
                @Index(name = "idx_contribution_reserved_recovery", columnList = "status, reserved_at, id")
        }
)
@Check(constraints = "amount >= 1000 AND MOD(amount, 1000) = 0")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Contribution extends BaseTimeEntity {
    private static final BigDecimal AMOUNT_UNIT = BigDecimal.valueOf(1_000);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ContributionStatus status;

    @Column(name = "reserved_at", nullable = false)
    private LocalDateTime reservedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "refund_started_at")
    private LocalDateTime refundStartedAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    private Contribution(
            Campaign campaign,
            User user,
            Order order,
            BigDecimal amount,
            LocalDateTime reservedAt
    ) {
        validateAmount(amount);
        if (campaign == null || user == null || order == null || reservedAt == null) {
            throw new IllegalArgumentException("Contribution relationships and reservedAt are required");
        }
        this.campaign = campaign;
        this.user = user;
        this.order = order;
        this.amount = amount;
        this.status = ContributionStatus.RESERVED;
        this.reservedAt = reservedAt;
    }

    public static Contribution reserved(
            Campaign campaign,
            User user,
            Order order,
            BigDecimal amount,
            LocalDateTime reservedAt
    ) {
        return new Contribution(campaign, user, order, amount, reservedAt);
    }

    public void confirm(LocalDateTime approvedAt, LocalDateTime confirmedAt) {
        requireStatus(ContributionStatus.RESERVED);
        if (approvedAt == null || confirmedAt == null) {
            throw new IllegalArgumentException("Approval and confirmation time are required");
        }
        this.status = ContributionStatus.CONFIRMED;
        this.approvedAt = approvedAt;
        this.confirmedAt = confirmedAt;
    }

    public void fail(LocalDateTime failedAt) {
        requireStatus(ContributionStatus.RESERVED);
        this.status = ContributionStatus.FAILED;
        this.failedAt = failedAt;
    }

    public void startRefund(LocalDateTime refundStartedAt) {
        requireStatus(ContributionStatus.CONFIRMED);
        this.status = ContributionStatus.REFUNDING;
        this.refundStartedAt = refundStartedAt;
    }

    public void startLateApprovalRefund(LocalDateTime approvedAt, LocalDateTime refundStartedAt) {
        requireStatus(ContributionStatus.RESERVED);
        if (approvedAt == null) {
            throw new IllegalArgumentException("Approval time is required");
        }
        this.status = ContributionStatus.REFUNDING;
        this.approvedAt = approvedAt;
        this.refundStartedAt = refundStartedAt;
    }

    public void completeRefund(LocalDateTime refundedAt) {
        requireStatus(ContributionStatus.REFUNDING);
        this.status = ContributionStatus.REFUNDED;
        this.refundedAt = refundedAt;
    }

    private void requireStatus(ContributionStatus expected) {
        if (status != expected) {
            throw new CustomException(CampaignErrorCode.INVALID_CONTRIBUTION_STATE);
        }
    }

    private static void validateAmount(BigDecimal amount) {
        if (amount == null
                || amount.compareTo(AMOUNT_UNIT) < 0
                || amount.remainder(AMOUNT_UNIT).compareTo(BigDecimal.ZERO) != 0) {
            throw new CustomException(CampaignErrorCode.INVALID_AMOUNT);
        }
    }
}
