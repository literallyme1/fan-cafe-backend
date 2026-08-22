package com.example.fan_cafe.order.payment.client;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.order.exception.OrderErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import org.springframework.http.HttpMethod;

class PaymentClientTest {
    private MockRestServiceServer server;
    private PaymentClient paymentClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        paymentClient = new PaymentClient(builder.baseUrl("http://payment-service").build(), new ObjectMapper());
    }

    @Test
    void knownRemoteErrorCode_isMappedToExistingOrderError() {
        server.expect(requestTo("http://payment-service/internal/payments/10/approve"))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"P005\",\"message\":\"already approved\"}"));

        assertThatThrownBy(() -> approve())
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(OrderErrorCode.ORDER_ALREADY_PAID);
        server.verify();
    }

    @Test
    void malformedRemoteErrorBody_isMappedToPaymentServiceError() {
        server.expect(requestTo("http://payment-service/internal/payments/10/approve"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("not-json"));

        assertThatThrownBy(() -> approve())
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(OrderErrorCode.PAYMENT_SERVICE_ERROR);
        server.verify();
    }

    @Test
    void missingRemoteErrorCode_isMappedToPaymentServiceError() {
        server.expect(requestTo("http://payment-service/internal/payments/10/approve"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"missing code\"}"));

        assertThatThrownBy(() -> approve())
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(OrderErrorCode.PAYMENT_SERVICE_ERROR);
        server.verify();
    }

    @Test
    void getStatus_readsPaymentStateThroughPaymentApi() {
        server.expect(requestTo("http://payment-service/internal/payments/10"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"orderId\":10,\"status\":\"APPROVED\",\"approvedAmount\":10}",
                        MediaType.APPLICATION_JSON));

        PaymentStatusResponse result = paymentClient.getStatus(10L);

        assertThat(result.status()).isEqualTo(PaymentResultStatus.APPROVED);
        assertThat(result.approvedAmount()).isEqualByComparingTo("10");
        server.verify();
    }

    @Test
    void refund_sendsSagaIdAndReadsRefundResult() {
        UUID sagaId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        server.expect(requestTo("http://payment-service/internal/payments/10/refund"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"sagaId":"550e8400-e29b-41d4-a716-446655440000","reason":"customer request"}
                        """))
                .andRespond(withSuccess("""
                        {"orderId":10,"status":"REFUNDED",
                         "refundIdempotencyKey":"REFUND:550e8400-e29b-41d4-a716-446655440000"}
                        """, MediaType.APPLICATION_JSON));

        PaymentStatusResponse result = paymentClient.refund(10L, sagaId, "customer request");

        assertThat(result.status()).isEqualTo(PaymentResultStatus.REFUNDED);
        assertThat(result.refundIdempotencyKey()).isEqualTo("REFUND:" + sagaId);
        server.verify();
    }

    private void approve() {
        paymentClient.approve(10L, BigDecimal.TEN, BigDecimal.TEN, "key-1");
    }
}
