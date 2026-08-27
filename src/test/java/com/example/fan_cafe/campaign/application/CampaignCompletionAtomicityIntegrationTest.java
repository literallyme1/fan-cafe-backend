package com.example.fan_cafe.campaign.application;

import com.example.fan_cafe.campaign.domain.Campaign;
import com.example.fan_cafe.campaign.domain.CampaignStatus;
import com.example.fan_cafe.campaign.domain.ContributionStatus;
import com.example.fan_cafe.campaign.infrastructure.CampaignRepository;
import com.example.fan_cafe.campaign.infrastructure.ContributionRepository;
import com.example.fan_cafe.order.domain.Status;
import com.example.fan_cafe.order.infrastructure.OrderRepository;
import com.example.fan_cafe.order.saga.application.SagaOrderCompletionService;
import com.example.fan_cafe.order.saga.application.SagaTransactionService;
import com.example.fan_cafe.order.saga.domain.SagaStatus;
import com.example.fan_cafe.order.saga.exception.OrderCompletionFailedException;
import com.example.fan_cafe.order.saga.infrastructure.SagaInstanceRepository;
import com.example.fan_cafe.outbox.domain.OutboxEvent;
import com.example.fan_cafe.outbox.infrastructure.OutboxEventRepository;
import com.example.fan_cafe.user.domain.Role;
import com.example.fan_cafe.user.domain.User;
import com.example.fan_cafe.user.infrastructure.UserRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;

@Tag("integration")
@ActiveProfiles("ci")
@SpringBootTest(properties = "spring.jpa.open-in-view=false")
class CampaignCompletionAtomicityIntegrationTest {
    private static final BigDecimal AMOUNT = new BigDecimal("10000");

    @Autowired private CampaignContributionReservationService reservationService;
    @Autowired private SagaTransactionService sagaTransactionService;
    @Autowired private SagaOrderCompletionService completionService;
    @Autowired private CampaignRepository campaignRepository;
    @Autowired private ContributionRepository contributionRepository;
    @Autowired private SagaInstanceRepository sagaRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;
    @SpyBean private OutboxEventRepository outboxRepository;

    @Test
    void campaignSuccessOutboxFailureRollsBackWholeOrderCompletionTransaction() {
        User user = userRepository.save(User.of(
                "atomic-" + UUID.randomUUID() + "@test.com", "encoded", "atomic", Role.USER));
        LocalDateTime now = LocalDateTime.now();
        Campaign campaign = campaignRepository.save(Campaign.open(
                "atomic campaign", "content", AMOUNT, now.minusDays(1), now.plusDays(1)));
        var reservation = reservationService.reserve(
                user, campaign.getId(), AMOUNT, "atomic-payment-key");
        var saga = sagaRepository.findByOrderId(reservation.orderId()).orElseThrow();
        LocalDateTime approvedAt = now.minusSeconds(1);
        sagaTransactionService.markPaymentCompleted(saga.getSagaId(), approvedAt);
        doThrow(new IllegalStateException("campaign outbox unavailable"))
                .when(outboxRepository).save(argThat(this::isCampaignEvent));

        assertThatThrownBy(() -> completionService.complete(
                saga.getSagaId(), reservation.orderId(), "approved", approvedAt))
                .isInstanceOf(OrderCompletionFailedException.class)
                .hasRootCauseMessage("campaign outbox unavailable");

        Campaign reloaded = campaignRepository.findById(campaign.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(CampaignStatus.OPEN);
        assertThat(reloaded.getFundedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(reloaded.getReservedAmount()).isEqualByComparingTo(AMOUNT);
        assertThat(contributionRepository.findById(reservation.contributionId()).orElseThrow().getStatus())
                .isEqualTo(ContributionStatus.RESERVED);
        assertThat(orderRepository.findById(reservation.orderId()).orElseThrow().getStatus())
                .isEqualTo(Status.PAYMENT_PENDING);
        assertThat(sagaRepository.findById(saga.getSagaId()).orElseThrow().getStatus())
                .isEqualTo(SagaStatus.PAYMENT_COMPLETED);
        assertThat(outboxRepository.findAll().stream().filter(this::isCampaignEvent)).isEmpty();
    }

    private boolean isCampaignEvent(OutboxEvent event) {
        return event != null && "CAMPAIGN".equals(event.getAggregateType());
    }
}
