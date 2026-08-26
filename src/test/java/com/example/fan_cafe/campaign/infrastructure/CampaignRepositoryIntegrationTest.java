package com.example.fan_cafe.campaign.infrastructure;

import com.example.fan_cafe.campaign.domain.Campaign;
import com.example.fan_cafe.campaign.domain.CampaignStatus;
import com.example.fan_cafe.campaign.domain.Contribution;
import com.example.fan_cafe.campaign.domain.ContributionStatus;
import com.example.fan_cafe.order.domain.Order;
import com.example.fan_cafe.order.domain.OrderType;
import com.example.fan_cafe.order.infrastructure.OrderRepository;
import com.example.fan_cafe.user.domain.Role;
import com.example.fan_cafe.user.domain.User;
import com.example.fan_cafe.user.infrastructure.UserRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@SpringBootTest
@ActiveProfiles("ci")
@Transactional
class CampaignRepositoryIntegrationTest {
    @Autowired private CampaignRepository campaignRepository;
    @Autowired private ContributionRepository contributionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void campaignContributionOrderRelationsAndRecoveryQueriesArePersisted() {
        LocalDateTime now = LocalDateTime.now();
        User user = userRepository.save(User.of(
                "campaign-" + UUID.randomUUID() + "@test.com",
                "encoded-password",
                "campaign-supporter",
                Role.USER));
        Campaign campaign = campaignRepository.save(Campaign.open(
                "birthday support",
                "campaign content",
                new BigDecimal("100000"),
                now.minusDays(2),
                now.minusDays(1)));
        Order order = orderRepository.save(Order.campaignContributionPaymentPending(
                user, new BigDecimal("10000")));
        Contribution contribution = contributionRepository.saveAndFlush(Contribution.reserved(
                campaign, user, order, new BigDecimal("10000"), now.minusHours(2)));

        Contribution reloaded = contributionRepository.findByOrderIdForUpdate(order.getId()).orElseThrow();

        assertThat(reloaded.getCampaign().getId()).isEqualTo(campaign.getId());
        assertThat(reloaded.getUser().getId()).isEqualTo(user.getId());
        assertThat(reloaded.getOrder().getOrderType()).isEqualTo(OrderType.CAMPAIGN_CONTRIBUTION);
        assertThat(campaignRepository.findNextExpiredOpenCampaignForUpdateSkipLocked(now))
                .map(Campaign::getId)
                .contains(campaign.getId());
        assertThat(contributionRepository.existsByCampaignIdAndStatusIn(
                campaign.getId(), List.of(ContributionStatus.RESERVED, ContributionStatus.REFUNDING)))
                .isTrue();
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.OPEN);

        assertThat(indexColumnCount("campaigns", "idx_campaign_deadline")).isEqualTo(3);
        assertThat(indexColumnCount("contributions", "idx_contribution_campaign_status")).isEqualTo(3);
        assertThat(indexColumnCount("contributions", "idx_contribution_reserved_recovery")).isEqualTo(3);
        assertThat(columnCount("orders", "order_type")).isEqualTo(1);
    }

    private Integer indexColumnCount(String tableName, String indexName) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND index_name = ?
                """, Integer.class, tableName, indexName);
    }

    private Integer columnCount(String tableName, String columnName) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """, Integer.class, tableName, columnName);
    }
}
