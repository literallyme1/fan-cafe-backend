package com.example.fan_cafe.order.saga.recovery;

import com.example.fan_cafe.FanCafeApplication;
import com.example.fan_cafe.order.domain.Status;
import com.example.fan_cafe.order.infrastructure.OrderRepository;
import com.example.fan_cafe.order.saga.application.PaymentSagaOrchestrator;
import com.example.fan_cafe.order.saga.application.SagaCompensationService;
import com.example.fan_cafe.order.saga.application.SagaSnapshot;
import com.example.fan_cafe.order.saga.application.SagaTransactionService;
import com.example.fan_cafe.order.saga.domain.SagaStatus;
import com.example.fan_cafe.order.saga.infrastructure.SagaInstanceRepository;
import com.example.fan_cafe.order.saga.messaging.PaymentRefundedResult;
import com.example.fan_cafe.order.saga.messaging.PaymentRefundedResultConsumer;
import com.example.fan_cafe.order.saga.messaging.RefundPaymentCommand;
import com.example.fan_cafe.order.payment.client.PaymentResultStatus;
import com.example.fan_cafe.order.support.OrderIntegrationTestSupport;
import com.example.fan_cafe.order.support.OrderIntegrationTestSupport.PaymentPendingFixture;
import com.example.fan_cafe.outbox.domain.OutboxEvent;
import com.example.fan_cafe.outbox.infrastructure.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class SagaCrashRecoveryIntegrationTest {

    @Test
    void freshApplicationContextRecoversPersistedCompensationWithoutPriorMemory() throws Exception {
        Long orderId;
        UUID sagaId;
        PaymentPendingFixture detachedFixture;

        try (ConfigurableApplicationContext executionA = startContext("create")) {
            OrderIntegrationTestSupport fixtures = executionA.getBean(OrderIntegrationTestSupport.class);
            detachedFixture = fixtures.createPaymentPendingOrder();
            orderId = detachedFixture.order().getId();
            SagaTransactionService sagaTransactions = executionA.getBean(SagaTransactionService.class);
            SagaSnapshot saga = sagaTransactions.start(orderId);
            sagaTransactions.transition(saga.sagaId(), SagaStatus.PAYMENT_PENDING);
            sagaTransactions.transition(saga.sagaId(), SagaStatus.PAYMENT_COMPLETED);
            executionA.getBean(SagaCompensationService.class)
                    .start(saga.sagaId(), orderId, "completion failed before crash");
            sagaId = saga.sagaId();

            markInitialRefundCommandSent(executionA, orderId);
            assertThat(executionA.getBean(SagaInstanceRepository.class).findById(sagaId).orElseThrow().getStatus())
                    .isEqualTo(SagaStatus.COMPENSATING);
        }

        try (ConfigurableApplicationContext executionB = startContext("validate")) {
            SagaInstanceRepository sagaRepository = executionB.getBean(SagaInstanceRepository.class);
            OutboxEventRepository outboxRepository = executionB.getBean(OutboxEventRepository.class);
            ObjectMapper objectMapper = executionB.getBean(ObjectMapper.class);
            assertThat(sagaRepository.findById(sagaId).orElseThrow().getStatus())
                    .isEqualTo(SagaStatus.COMPENSATING);

            SagaRecoveryWorker restartedWorker = new SagaRecoveryWorker(
                    executionB.getBean(SagaRecoveryTransactionService.class),
                    executionB.getBean(PaymentSagaOrchestrator.class),
                    executionB.getBean(SagaRecoveryProperties.class));
            restartedWorker.recoverDueSagas();

            List<OutboxEvent> commands = paymentSagaEvents(outboxRepository, orderId);
            assertThat(commands).hasSize(2);
            assertThat(commands).extracting(OutboxEvent::getEventId).doesNotHaveDuplicates();
            List<RefundPaymentCommand> payloads = commands.stream()
                    .sorted(Comparator.comparing(OutboxEvent::getId))
                    .map(event -> readCommand(objectMapper, event.getPayload()))
                    .toList();
            assertThat(payloads).extracting(RefundPaymentCommand::idempotencyKey)
                    .containsOnly("REFUND:" + sagaId);

            executionB.getBean(PaymentRefundedResultConsumer.class).consume(new PaymentRefundedResult(
                    PaymentRefundedResult.EVENT_TYPE,
                    sagaId,
                    orderId,
                    PaymentResultStatus.REFUNDED,
                    "REFUND:" + sagaId,
                    "completion failed before crash"));

            assertThat(sagaRepository.findById(sagaId).orElseThrow().getStatus())
                    .isEqualTo(SagaStatus.COMPENSATED);
            assertThat(executionB.getBean(OrderRepository.class).findById(orderId).orElseThrow().getStatus())
                    .isEqualTo(Status.REFUNDED);
            executionB.getBean(OrderIntegrationTestSupport.class).cleanup(detachedFixture);
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
                        "--saga.recovery.refund-result-timeout=0s",
                        "--saga.recovery.order-completion-initial-delay=0s",
                        "--scheduler.comment-count.enabled=false",
                        "--redis.warmup.enabled=false"
                );
    }

    private void markInitialRefundCommandSent(ConfigurableApplicationContext context, Long orderId) {
        OutboxEventRepository repository = context.getBean(OutboxEventRepository.class);
        context.getBean(TransactionTemplate.class).executeWithoutResult(ignored -> {
            OutboxEvent initial = paymentSagaEvents(repository, orderId).getFirst();
            initial.markSent();
        });
    }

    private List<OutboxEvent> paymentSagaEvents(OutboxEventRepository repository, Long orderId) {
        return repository.findAll().stream()
                .filter(event -> "PAYMENT_SAGA".equals(event.getAggregateType()))
                .filter(event -> orderId.equals(event.getAggregateId()))
                .toList();
    }

    private RefundPaymentCommand readCommand(ObjectMapper objectMapper, String payload) {
        try {
            return objectMapper.readValue(payload, RefundPaymentCommand.class);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
