package com.example.fan_cafe.order.application;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.order.config.MockPgProperties;
import com.example.fan_cafe.order.domain.Order;
import com.example.fan_cafe.order.domain.OrderItem;
import com.example.fan_cafe.order.exception.OrderErrorCode;
import com.example.fan_cafe.order.infrastructure.OrderRepository;
import com.example.fan_cafe.order.interfaces.dto.OrderQueryResponse;
import com.example.fan_cafe.order.payment.MockPgWebhookSignatureVerifier;
import com.example.fan_cafe.user.domain.Role;
import com.example.fan_cafe.user.domain.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MockPgWebhookServiceTest {

    private static final String SECRET = "test-mock-pg-webhook-secret";

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderPaymentCommandService orderPaymentCommandService;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockPgWebhookService mockPgWebhookService;
    private Order paymentPendingOrder;

    @BeforeEach
    void setUp() {
        MockPgProperties properties = new MockPgProperties();
        properties.setWebhookSecret(SECRET);
        MockPgWebhookSignatureVerifier signatureVerifier = new MockPgWebhookSignatureVerifier(properties);

        mockPgWebhookService = new MockPgWebhookService(
                signatureVerifier,
                orderRepository,
                orderPaymentCommandService,
                objectMapper
        );

        User user = User.of("u@test.com", "pw", "u", Role.USER);
        ReflectionTestUtils.setField(user, "id", 1L);
        paymentPendingOrder = Order.paymentPending(user, BigDecimal.valueOf(20000));
        ReflectionTestUtils.setField(paymentPendingOrder, "id", 10L);
        paymentPendingOrder.addItem(OrderItem.snapshot(100L, "item", BigDecimal.TEN, 1));
    }

    @Test
    @DisplayName("서명 실패 시 결제 명령 서비스를 호출하지 않는다.")
    void receive_shouldNotInvokePayment_whenSignatureInvalid() {
        String rawBody = "{\"eventType\":\"PAYMENT_APPROVED\",\"orderId\":10,\"approvalAmount\":20000,\"idempotencyKey\":\"k1\"}";
        String timestamp = String.valueOf(Instant.now().getEpochSecond());

        assertThatThrownBy(() -> mockPgWebhookService.receive(rawBody, timestamp, "invalid-sig"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(OrderErrorCode.WEBHOOK_SIGNATURE_INVALID.getMessage());

        verify(orderPaymentCommandService, never()).approvePaymentWithPessimisticLock(any(), any(), any(), any());
        verify(orderPaymentCommandService, never()).failPayment(any(), any());
        verify(orderRepository, never()).findByIdWithItems(any());
    }

    @Test
    @DisplayName("PAYMENT_APPROVED 웹훅은 공통 승인 로직을 호출한다.")
    void receive_shouldApprove_whenPaymentApprovedWebhook() {
        String rawBody = """
                {"eventType":"PAYMENT_APPROVED","orderId":10,"approvalAmount":20000,"idempotencyKey":"wh-001"}
                """.trim();
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = MockPgWebhookSignatureVerifier.signForTest(SECRET, timestamp, rawBody);

        when(orderRepository.findByIdWithItems(10L)).thenReturn(Optional.of(paymentPendingOrder));
        Order paidResult = paymentPendingOrder;
        paidResult.markPaid("wh-001");
        when(orderPaymentCommandService.approvePaymentWithPessimisticLock(
                eq(paymentPendingOrder),
                eq(BigDecimal.valueOf(20000)),
                eq("wh-001"),
                eq("mock pg webhook approved")
        )).thenReturn(paidResult);

        OrderQueryResponse response = mockPgWebhookService.receive(rawBody, timestamp, signature);

        assertThat(response.getStatus()).isEqualTo(com.example.fan_cafe.order.domain.Status.PAID);
        verify(orderPaymentCommandService).approvePaymentWithPessimisticLock(
                paymentPendingOrder,
                BigDecimal.valueOf(20000),
                "wh-001",
                "mock pg webhook approved"
        );
    }

    @Test
    @DisplayName("PAYMENT_FAILED 웹훅은 공통 실패 로직을 호출한다.")
    void receive_shouldFail_whenPaymentFailedWebhook() {
        String rawBody = """
                {"eventType":"PAYMENT_FAILED","orderId":10,"reason":"card declined"}
                """.trim();
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = MockPgWebhookSignatureVerifier.signForTest(SECRET, timestamp, rawBody);

        when(orderRepository.findByIdWithItems(10L)).thenReturn(Optional.of(paymentPendingOrder));
        paymentPendingOrder.markPaymentFailed();
        when(orderPaymentCommandService.failPayment(paymentPendingOrder, "card declined"))
                .thenReturn(paymentPendingOrder);

        OrderQueryResponse response = mockPgWebhookService.receive(rawBody, timestamp, signature);

        assertThat(response.getStatus()).isEqualTo(com.example.fan_cafe.order.domain.Status.PAYMENT_FAILED);
        verify(orderPaymentCommandService).failPayment(paymentPendingOrder, "card declined");
        verify(orderPaymentCommandService, never()).approvePaymentWithPessimisticLock(any(), any(), any(), any());
    }
}
