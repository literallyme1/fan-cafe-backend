package com.example.fan_cafe.campaign.application;

import com.example.fan_cafe.FanCafeApplication;
import com.example.fan_cafe.campaign.domain.ContributionStatus;
import com.example.fan_cafe.campaign.infrastructure.CampaignRepository;
import com.example.fan_cafe.campaign.infrastructure.ContributionRepository;
import com.example.fan_cafe.campaign.interfaces.dto.CampaignCreateRequest;
import com.example.fan_cafe.order.domain.Status;
import com.example.fan_cafe.order.infrastructure.OrderRepository;
import com.example.fan_cafe.order.saga.application.SagaTransactionService;
import com.example.fan_cafe.order.saga.domain.SagaStatus;
import com.example.fan_cafe.order.saga.infrastructure.SagaInstanceRepository;
import com.example.fan_cafe.order.saga.recovery.SagaRecoveryWorker;
import com.example.fan_cafe.user.domain.Role;
import com.example.fan_cafe.user.domain.User;
import com.example.fan_cafe.user.infrastructure.UserRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class CampaignPaymentCompletedCrashRecoveryIntegrationTest {
    @Test
    void freshContextRecoversPersistedCampaignPaymentCompletedExactlyOnce() {
        Long campaignId;
        Long contributionId;
        Long orderId;
        UUID sagaId;
        LocalDateTime approvedAt = LocalDateTime.of(2026, 8, 27, 1, 2, 3, 123456000);

        try (ConfigurableApplicationContext executionA = startContext("create")) {
            User user = executionA.getBean(UserRepository.class).save(User.of(
                    "campaign-crash@test.com", "encoded", "campaign-crash", Role.USER));
            var campaign = executionA.getBean(CampaignService.class).create(new CampaignCreateRequest(
                    "crash recovery", "content", new BigDecimal("20000"),
                    LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1)));
            campaignId = campaign.campaignId();
            CampaignContributionReservation reservation = executionA
                    .getBean(CampaignContributionReservationService.class)
                    .reserve(user, campaignId, new BigDecimal("10000"), "crash-approved-key");
            contributionId = reservation.contributionId();
            orderId = reservation.orderId();
            sagaId = executionA.getBean(SagaInstanceRepository.class)
                    .findByOrderId(orderId).orElseThrow().getSagaId();
            executionA.getBean(SagaTransactionService.class)
                    .markPaymentCompleted(sagaId, approvedAt);
        }

        try (ConfigurableApplicationContext executionB = startContext("validate")) {
            SagaRecoveryWorker worker = executionB.getBean(SagaRecoveryWorker.class);
            worker.recoverDueSagas();
            worker.recoverDueSagas();

            var saga = executionB.getBean(SagaInstanceRepository.class).findById(sagaId).orElseThrow();
            assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPLETED);
            assertThat(saga.getPaymentApprovedAt()).isEqualTo(approvedAt);
            assertThat(executionB.getBean(ContributionRepository.class)
                    .findById(contributionId).orElseThrow().getStatus())
                    .isEqualTo(ContributionStatus.CONFIRMED);
            assertThat(executionB.getBean(CampaignRepository.class)
                    .findById(campaignId).orElseThrow().getFundedAmount())
                    .isEqualByComparingTo("10000");
            assertThat(executionB.getBean(OrderRepository.class)
                    .findById(orderId).orElseThrow().getStatus()).isEqualTo(Status.PAID);
        }
    }

    private ConfigurableApplicationContext startContext(String ddlMode) {
        return new SpringApplicationBuilder(FanCafeApplication.class)
                .profiles("ci")
                .web(WebApplicationType.NONE)
                .run(
                        "--spring.jpa.hibernate.ddl-auto=" + ddlMode,
                        "--spring.sql.init.mode=never",
                        "--spring.rabbitmq.listener.simple.auto-startup=false",
                        "--spring.rabbitmq.listener.direct.auto-startup=false",
                        "--saga.recovery.enabled=false",
                        "--saga.recovery.order-completion-initial-delay=0s",
                        "--scheduler.comment-count.enabled=false",
                        "--redis.warmup.enabled=false"
                );
    }
}
