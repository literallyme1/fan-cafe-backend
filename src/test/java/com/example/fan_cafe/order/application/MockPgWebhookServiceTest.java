package com.example.fan_cafe.order.application;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.order.domain.Order;
import com.example.fan_cafe.order.domain.OrderItem;
import com.example.fan_cafe.order.exception.OrderErrorCode;
import com.example.fan_cafe.order.infrastructure.OrderRepository;
import com.example.fan_cafe.order.interfaces.dto.OrderQueryResponse;
import com.example.fan_cafe.order.payment.client.PaymentClient;
import com.example.fan_cafe.order.payment.client.PaymentResultResponse;
import com.example.fan_cafe.order.payment.client.PaymentResultStatus;
import com.example.fan_cafe.user.domain.Role;
import com.example.fan_cafe.user.domain.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MockPgWebhookServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderPaymentResultService resultService;
    @Mock private PaymentClient paymentClient;

    private MockPgWebhookService webhookService;
    private Order paymentPendingOrder;

    @BeforeEach
    void setUp() {
        webhookService = new MockPgWebhookService(
                orderRepository, resultService, paymentClient, new ObjectMapper());

        User user = User.of("u@test.com", "pw", "u", Role.USER);
        ReflectionTestUtils.setField(user, "id", 1L);
        paymentPendingOrder = Order.paymentPending(user, BigDecimal.valueOf(20000));
        ReflectionTestUtils.setField(paymentPendingOrder, "id", 10L);
        paymentPendingOrder.addItem(OrderItem.snapshot(100L, "item", BigDecimal.TEN, 1));
    }

    @Test
    @DisplayName("Payment가 웹훅 서명을 거부하면 주문 결과를 반영하지 않는다")
    void receive_shouldNotApplyResult_whenPaymentRejectsSignature() {
        String rawBody = "{\"eventType\":\"PAYMENT_APPROVED\",\"orderId\":10,\"approvalAmount\":20000}";
        when(orderRepository.findByIdWithItems(10L)).thenReturn(Optional.of(paymentPendingOrder));
        when(paymentClient.forwardWebhook(rawBody, "100", "invalid", BigDecimal.valueOf(20000)))
                .thenThrow(new CustomException(OrderErrorCode.WEBHOOK_SIGNATURE_INVALID));

        assertThatThrownBy(() -> webhookService.receive(rawBody, "100", "invalid"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(OrderErrorCode.WEBHOOK_SIGNATURE_INVALID.getMessage());

        verifyNoInteractions(resultService);
    }

    @Test
    @DisplayName("승인 웹훅 원문을 Payment에 전달하고 승인 결과를 Order에 반영한다")
    void receive_shouldForwardAndApplyApprovedResult() {
        String rawBody = "{\"eventType\":\"PAYMENT_APPROVED\",\"orderId\":10,\"approvalAmount\":20000}";
        PaymentResultResponse result = new PaymentResultResponse(
                10L, PaymentResultStatus.APPROVED, "wh-001", null, null);
        paymentPendingOrder.markPaid();
        OrderQueryResponse paid = OrderQueryResponse.from(paymentPendingOrder);

        when(orderRepository.findByIdWithItems(10L)).thenReturn(Optional.of(paymentPendingOrder));
        when(paymentClient.forwardWebhook(rawBody, "100", "signature", BigDecimal.valueOf(20000)))
                .thenReturn(result);
        when(resultService.apply(10L, result,
                "mock pg webhook approved", "mock pg webhook payment failed"))
                .thenReturn(paid);

        var response = webhookService.receive(rawBody, "100", "signature");

        assertThat(response.getStatus()).isEqualTo(com.example.fan_cafe.order.domain.Status.PAID);
        verify(resultService).apply(10L, result,
                "mock pg webhook approved", "mock pg webhook payment failed");
    }
}
