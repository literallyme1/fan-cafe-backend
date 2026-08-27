package com.example.fan_cafe.campaign.domain;

import com.example.fan_cafe.order.domain.Order;
import com.example.fan_cafe.order.domain.OrderType;
import com.example.fan_cafe.user.domain.Role;
import com.example.fan_cafe.user.domain.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ContributionTest {

    @Test
    void reservedContributionKeepsCampaignOrderAndOwnLifecycleState() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 26, 10, 0);
        User user = User.of("campaign@test.com", "encoded", "supporter", Role.USER);
        Campaign campaign = Campaign.open(
                "birthday support", "content", new BigDecimal("100000"), now, now.plusDays(7));
        Order order = Order.campaignContributionPaymentPending(user, new BigDecimal("10000"));

        Contribution contribution = Contribution.reserved(
                campaign, user, order, new BigDecimal("10000"), now.plusMinutes(1));

        assertThat(contribution.getStatus()).isEqualTo(ContributionStatus.RESERVED);
        assertThat(contribution.getOrder().getOrderType()).isEqualTo(OrderType.CAMPAIGN_CONTRIBUTION);
        assertThat(contribution.getApprovedAt()).isNull();
    }

    @Test
    void confirmationRecordsPaymentApprovalTimeSeparatelyFromConfirmationTime() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 26, 10, 0);
        User user = User.of("campaign-confirm@test.com", "encoded", "supporter", Role.USER);
        Campaign campaign = Campaign.open(
                "birthday support", "content", new BigDecimal("100000"), now, now.plusDays(7));
        Order order = Order.campaignContributionPaymentPending(user, new BigDecimal("10000"));
        Contribution contribution = Contribution.reserved(
                campaign, user, order, new BigDecimal("10000"), now.plusMinutes(1));
        LocalDateTime approvedAt = now.plusMinutes(2);
        LocalDateTime confirmedAt = now.plusMinutes(3);

        contribution.confirm(approvedAt, confirmedAt);

        assertThat(contribution.getStatus()).isEqualTo(ContributionStatus.CONFIRMED);
        assertThat(contribution.getApprovedAt()).isEqualTo(approvedAt);
        assertThat(contribution.getConfirmedAt()).isEqualTo(confirmedAt);
    }
}
