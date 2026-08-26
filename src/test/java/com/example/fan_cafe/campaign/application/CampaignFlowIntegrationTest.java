package com.example.fan_cafe.campaign.application;

import com.example.fan_cafe.campaign.domain.Campaign;
import com.example.fan_cafe.campaign.domain.CampaignStatus;
import com.example.fan_cafe.campaign.domain.ContributionStatus;
import com.example.fan_cafe.campaign.exception.CampaignErrorCode;
import com.example.fan_cafe.campaign.infrastructure.CampaignRepository;
import com.example.fan_cafe.campaign.infrastructure.ContributionRepository;
import com.example.fan_cafe.campaign.interfaces.dto.ContributionCreateRequest;
import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.order.domain.Status;
import com.example.fan_cafe.order.infrastructure.OrderRepository;
import com.example.fan_cafe.order.payment.client.PaymentClient;
import com.example.fan_cafe.order.payment.client.PaymentOutcomeUnknownException;
import com.example.fan_cafe.order.payment.client.PaymentResultResponse;
import com.example.fan_cafe.order.payment.client.PaymentResultStatus;
import com.example.fan_cafe.order.payment.client.PaymentStatusResponse;
import com.example.fan_cafe.order.saga.application.SagaCompensationService;
import com.example.fan_cafe.order.saga.domain.SagaStatus;
import com.example.fan_cafe.order.saga.infrastructure.SagaInstanceRepository;
import com.example.fan_cafe.order.saga.messaging.PaymentRefundedResult;
import com.example.fan_cafe.order.saga.recovery.SagaRecoveryWorker;
import com.example.fan_cafe.outbox.domain.OutboxEvent;
import com.example.fan_cafe.outbox.infrastructure.OutboxEventRepository;
import com.example.fan_cafe.user.domain.Role;
import com.example.fan_cafe.user.domain.User;
import com.example.fan_cafe.user.infrastructure.UserRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("integration")
@SpringBootTest(properties = "spring.jpa.open-in-view=false")
@ActiveProfiles("ci")
class CampaignFlowIntegrationTest {
    private static final BigDecimal TEN_THOUSAND = new BigDecimal("10000");

    @Autowired private CampaignService campaignService;
    @Autowired private CampaignContributionService contributionService;
    @Autowired private CampaignContributionReservationService reservationService;
    @Autowired private CampaignRefundService refundService;
    @Autowired private CampaignDeadlineWorker deadlineWorker;
    @Autowired private SagaCompensationService compensationService;
    @Autowired private SagaRecoveryWorker sagaRecoveryWorker;
    @Autowired private CampaignRepository campaignRepository;
    @Autowired private ContributionRepository contributionRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private SagaInstanceRepository sagaRepository;
    @Autowired private OutboxEventRepository outboxRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @MockBean private PaymentClient paymentClient;

    @Test
    void approvedContributionReachesSuccessAndCreatesParticipantNotificationAtomically() {
        User user = user("success");
        Campaign campaign = campaign(TEN_THOUSAND, 2);
        LocalDateTime approvedAt = LocalDateTime.now();
        approveSuccessfully(approvedAt);

        var response = contributionService.contribute(
                user, campaign.getId(), request(TEN_THOUSAND, "campaign-success"));

        Campaign reloaded = campaignRepository.findById(campaign.getId()).orElseThrow();
        assertThat(response.status()).isEqualTo(ContributionStatus.CONFIRMED);
        assertThat(reloaded.getStatus()).isEqualTo(CampaignStatus.SUCCESS);
        assertThat(reloaded.getFundedAmount()).isEqualByComparingTo(TEN_THOUSAND);
        assertThat(reloaded.getReservedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(orderRepository.findById(response.orderId()).orElseThrow().getStatus())
                .isEqualTo(Status.PAID);
        assertThat(sagaRepository.findByOrderId(response.orderId()).orElseThrow().getStatus())
                .isEqualTo(SagaStatus.COMPLETED);

        List<OutboxEvent> campaignEvents = outboxRepository.findAll().stream()
                .filter(event -> "CAMPAIGN".equals(event.getAggregateType()))
                .filter(event -> event.getAggregateId().equals(campaign.getId()))
                .toList();
        assertThat(campaignEvents).hasSize(1);
        assertThat(campaignEvents.getFirst().getPayload())
                .contains("CAMPAIGN_SUCCEEDED")
                .contains("\"receiverId\":" + user.getId());

        assertThatThrownBy(() -> reservationService.reserve(user, campaign.getId(), TEN_THOUSAND))
                .isInstanceOf(CustomException.class)
                .extracting(failure -> ((CustomException) failure).getErrorCode())
                .isEqualTo(CampaignErrorCode.CAMPAIGN_NOT_OPEN);
        assertThatThrownBy(() -> refundService.requestUserRefund(
                user, campaign.getId(), response.contributionId()))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void definitivePaymentFailureReleasesReservation() {
        User user = user("failed");
        Campaign campaign = campaign(new BigDecimal("20000"), 2);
        when(paymentClient.approve(anyLong(), any(), any(), anyString()))
                .thenAnswer(invocation -> new PaymentResultResponse(
                        invocation.getArgument(0), PaymentResultStatus.FAILED,
                        null, "declined", null, null));

        var response = contributionService.contribute(
                user, campaign.getId(), request(TEN_THOUSAND, "campaign-failed"));

        Campaign reloaded = campaignRepository.findById(campaign.getId()).orElseThrow();
        assertThat(response.status()).isEqualTo(ContributionStatus.FAILED);
        assertThat(reloaded.getFundedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(reloaded.getReservedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(orderRepository.findById(response.orderId()).orElseThrow().getStatus())
                .isEqualTo(Status.PAYMENT_FAILED);
        assertThat(sagaRepository.findByOrderId(response.orderId()).orElseThrow().getStatus())
                .isEqualTo(SagaStatus.CANCELLED);
    }

    @Test
    void timeoutUsesStatusLookupWithoutExecutingApprovalTwice() {
        User user = user("timeout");
        Campaign campaign = campaign(TEN_THOUSAND, 2);
        LocalDateTime approvedAt = LocalDateTime.now();
        when(paymentClient.approve(anyLong(), any(), any(), anyString()))
                .thenThrow(new PaymentOutcomeUnknownException(
                        com.example.fan_cafe.order.exception.OrderErrorCode.PAYMENT_SERVICE_UNAVAILABLE));
        when(paymentClient.getStatus(anyLong())).thenAnswer(invocation -> approvedStatus(
                invocation.getArgument(0), approvedAt));

        var response = contributionService.contribute(
                user, campaign.getId(), request(TEN_THOUSAND, "campaign-timeout"));

        assertThat(response.status()).isEqualTo(ContributionStatus.CONFIRMED);
        assertThat(sagaRepository.findByOrderId(response.orderId()).orElseThrow().getStatus())
                .isEqualTo(SagaStatus.COMPLETED);
        verify(paymentClient, times(1)).approve(anyLong(), any(), any(), anyString());
        verify(paymentClient, times(1)).getStatus(response.orderId());
    }

    @Test
    void crashBeforePaymentExecuteIsRecoveredFromPersistedPendingSaga() {
        User user = user("pre-execute-crash");
        Campaign campaign = campaign(new BigDecimal("20000"), 2);
        CampaignContributionReservation reservation = reservationService.reserve(
                user, campaign.getId(), TEN_THOUSAND);
        jdbcTemplate.update("UPDATE saga_instance SET next_retry_at = ? WHERE order_id = ?",
                LocalDateTime.of(2000, 1, 1, 0, 0), reservation.orderId());
        when(paymentClient.getStatus(reservation.orderId()))
                .thenThrow(new CustomException(
                        com.example.fan_cafe.order.exception.OrderErrorCode.PAYMENT_NOT_FOUND));

        sagaRecoveryWorker.recoverDueSagas();

        assertThat(contributionRepository.findById(reservation.contributionId()).orElseThrow().getStatus())
                .isEqualTo(ContributionStatus.FAILED);
        assertThat(campaignRepository.findById(campaign.getId()).orElseThrow().getReservedAmount())
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(orderRepository.findById(reservation.orderId()).orElseThrow().getStatus())
                .isEqualTo(Status.PAYMENT_FAILED);
        assertThat(sagaRepository.findByOrderId(reservation.orderId()).orElseThrow().getStatus())
                .isEqualTo(SagaStatus.CANCELLED);
        verify(paymentClient, never()).approve(anyLong(), any(), any(), anyString());
    }

    @Test
    void approvalAfterDeadlineStartsExistingCompensationWithoutFundingCampaign() {
        User user = user("late");
        Campaign campaign = campaign(TEN_THOUSAND, 1);
        LocalDateTime approvedAt = campaign.getDeadlineAt().plusSeconds(1);
        approveSuccessfully(approvedAt);

        var response = contributionService.contribute(
                user, campaign.getId(), request(TEN_THOUSAND, "campaign-late"));
        var saga = sagaRepository.findByOrderId(response.orderId()).orElseThrow();

        assertThat(response.status()).isEqualTo(ContributionStatus.REFUNDING);
        assertThat(campaignRepository.findById(campaign.getId()).orElseThrow().getFundedAmount())
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(campaignRepository.findById(campaign.getId()).orElseThrow().getReservedAmount())
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPENSATING);
        assertRefundCommand(response.orderId(), saga.getSagaId(), "LATE_APPROVAL_AFTER_DEADLINE");

        compensationService.complete(refunded(saga.getSagaId(), response.orderId()));

        assertThat(contributionRepository.findById(response.contributionId()).orElseThrow().getStatus())
                .isEqualTo(ContributionStatus.REFUNDED);
        assertThat(sagaRepository.findById(saga.getSagaId()).orElseThrow().getStatus())
                .isEqualTo(SagaStatus.COMPENSATED);
        assertThat(orderRepository.findById(response.orderId()).orElseThrow().getStatus())
                .isEqualTo(Status.REFUNDED);
    }

    @Test
    void openCampaignUserRefundReusesSagaOutboxAndIsIdempotent() {
        User user = user("user-refund");
        Campaign campaign = campaign(new BigDecimal("20000"), 2);
        approveSuccessfully(LocalDateTime.now());
        var confirmed = contributionService.contribute(
                user, campaign.getId(), request(TEN_THOUSAND, "campaign-user-refund"));
        clearInvocations(paymentClient);

        var first = refundService.requestUserRefund(user, campaign.getId(), confirmed.contributionId());
        var second = refundService.requestUserRefund(user, campaign.getId(), confirmed.contributionId());
        var saga = sagaRepository.findByOrderId(confirmed.orderId()).orElseThrow();

        assertThat(first.status()).isEqualTo(ContributionStatus.REFUNDING);
        assertThat(second.status()).isEqualTo(ContributionStatus.REFUNDING);
        assertThat(outboxRepository.countByAggregateTypeAndAggregateId(
                "PAYMENT_SAGA", confirmed.orderId())).isEqualTo(1);
        verify(paymentClient, never()).refund(anyLong(), any(), anyString());

        compensationService.complete(refunded(saga.getSagaId(), confirmed.orderId()));
        assertThat(campaignRepository.findById(campaign.getId()).orElseThrow().getFundedAmount())
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(campaignRepository.findById(campaign.getId()).orElseThrow().getStatus())
                .isEqualTo(CampaignStatus.OPEN);
    }

    @Test
    void deadlineCollectiveRefundKeepsPartialProgressAndConvergesAfterRestartLikeRerun() {
        User firstUser = user("deadline-a");
        User secondUser = user("deadline-b");
        Campaign campaign = campaign(new BigDecimal("30000"), 2);
        approveSuccessfully(LocalDateTime.now());
        var first = contributionService.contribute(
                firstUser, campaign.getId(), request(TEN_THOUSAND, "deadline-a"));
        var second = contributionService.contribute(
                secondUser, campaign.getId(), request(TEN_THOUSAND, "deadline-b"));
        jdbcTemplate.update("UPDATE campaigns SET deadline_at = ? WHERE id = ?",
                LocalDateTime.now().minusSeconds(1), campaign.getId());

        deadlineWorker.processDueCampaigns(10);

        assertThat(campaignRepository.findById(campaign.getId()).orElseThrow().getStatus())
                .isEqualTo(CampaignStatus.REFUNDING);
        assertThat(contributionRepository.findAllByCampaignIdOrderById(campaign.getId()))
                .extracting(contribution -> contribution.getStatus())
                .containsOnly(ContributionStatus.REFUNDING);
        long commandCount = refundCommandCount(first.orderId()) + refundCommandCount(second.orderId());
        assertThat(commandCount).isEqualTo(2);

        var firstSaga = sagaRepository.findByOrderId(first.orderId()).orElseThrow();
        compensationService.complete(refunded(firstSaga.getSagaId(), first.orderId()));
        assertThat(campaignRepository.findById(campaign.getId()).orElseThrow().getStatus())
                .isEqualTo(CampaignStatus.REFUNDING);

        deadlineWorker.processDueCampaigns(10);
        assertThat(refundCommandCount(first.orderId()) + refundCommandCount(second.orderId()))
                .isEqualTo(commandCount);

        var secondSaga = sagaRepository.findByOrderId(second.orderId()).orElseThrow();
        compensationService.complete(refunded(secondSaga.getSagaId(), second.orderId()));
        assertThat(campaignRepository.findById(campaign.getId()).orElseThrow().getStatus())
                .isEqualTo(CampaignStatus.REFUNDED);
        assertThat(contributionRepository.findAllByCampaignIdOrderById(campaign.getId()))
                .extracting(contribution -> contribution.getStatus())
                .containsOnly(ContributionStatus.REFUNDED);
    }

    @Test
    void pessimisticReservationAllowsOnlyOneLastAmountWinner() throws Exception {
        User first = user("race-a");
        User second = user("race-b");
        Campaign campaign = campaign(TEN_THOUSAND, 2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        try (var executor = Executors.newFixedThreadPool(2)) {
            for (User user : List.of(first, second)) {
                executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await(5, TimeUnit.SECONDS);
                        reservationService.reserve(user, campaign.getId(), TEN_THOUSAND);
                        successes.incrementAndGet();
                    } catch (CustomException failure) {
                        if (failure.getErrorCode() == CampaignErrorCode.TARGET_AMOUNT_EXCEEDED) {
                            rejected.incrementAndGet();
                        }
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            executor.shutdown();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }

        Campaign reloaded = campaignRepository.findById(campaign.getId()).orElseThrow();
        assertThat(successes).hasValue(1);
        assertThat(rejected).hasValue(1);
        assertThat(reloaded.getFundedAmount().add(reloaded.getReservedAmount()))
                .isEqualByComparingTo(TEN_THOUSAND);
    }

    private User user(String prefix) {
        return userRepository.save(User.of(
                prefix + "-" + UUID.randomUUID() + "@test.com",
                "encoded", prefix, Role.USER));
    }

    private Campaign campaign(BigDecimal target, int deadlineDays) {
        LocalDateTime now = LocalDateTime.now();
        return campaignRepository.save(Campaign.open(
                "campaign-" + UUID.randomUUID(), "content", target,
                now.minusDays(1), now.plusDays(deadlineDays)));
    }

    private ContributionCreateRequest request(BigDecimal amount, String key) {
        return new ContributionCreateRequest(amount, key + "-" + UUID.randomUUID());
    }

    private void approveSuccessfully(LocalDateTime approvedAt) {
        when(paymentClient.approve(anyLong(), any(), any(), anyString()))
                .thenAnswer(invocation -> new PaymentResultResponse(
                        invocation.getArgument(0), PaymentResultStatus.APPROVED,
                        invocation.getArgument(3), null, null, approvedAt));
    }

    private PaymentStatusResponse approvedStatus(Long orderId, LocalDateTime approvedAt) {
        return new PaymentStatusResponse(
                orderId, PaymentResultStatus.APPROVED, TEN_THOUSAND, TEN_THOUSAND,
                approvedAt, "recovered", null, null, null, null);
    }

    private PaymentRefundedResult refunded(UUID sagaId, Long orderId) {
        return new PaymentRefundedResult(
                PaymentRefundedResult.EVENT_TYPE, sagaId, orderId,
                PaymentResultStatus.REFUNDED, "REFUND:" + sagaId, "campaign refund");
    }

    private void assertRefundCommand(Long orderId, UUID sagaId, String reason) {
        List<String> payloads = outboxRepository.findAll().stream()
                .filter(event -> "PAYMENT_SAGA".equals(event.getAggregateType()))
                .filter(event -> event.getAggregateId().equals(orderId))
                .map(OutboxEvent::getPayload)
                .toList();
        assertThat(payloads).singleElement()
                .asString()
                .contains("REFUND:" + sagaId)
                .contains(reason);
    }

    private long refundCommandCount(Long orderId) {
        return outboxRepository.countByAggregateTypeAndAggregateId("PAYMENT_SAGA", orderId);
    }
}
