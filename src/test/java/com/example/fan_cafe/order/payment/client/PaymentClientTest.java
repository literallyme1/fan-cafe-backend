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
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.UUID;
import com.sun.net.httpserver.HttpServer;

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
    void approvalServerError_isClassifiedAsUnknownOutcome() {
        server.expect(requestTo("http://payment-service/internal/payments/10/approve"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"P999\",\"message\":\"internal error\"}"));

        assertThatThrownBy(this::approve)
                .isInstanceOf(PaymentOutcomeUnknownException.class)
                .extracting(exception -> ((PaymentOutcomeUnknownException) exception).getErrorCode())
                .isEqualTo(OrderErrorCode.PAYMENT_SERVICE_ERROR);
        server.verify();
    }

    @Test
    void emptyApprovalResponse_isClassifiedAsUnknownOutcome() {
        assertUnknownApprovalResponse("{}");
    }

    @Test
    void nullApprovalStatus_isClassifiedAsUnknownOutcome() {
        assertUnknownApprovalResponse("{\"orderId\":10,\"status\":null}");
    }

    @Test
    void wrongApprovalOrderId_isClassifiedAsUnknownOutcome() {
        assertUnknownApprovalResponse("{\"orderId\":11,\"status\":\"APPROVED\"}");
    }

    @Test
    void unexpectedApprovalStatus_isClassifiedAsUnknownOutcome() {
        assertUnknownApprovalResponse("{\"orderId\":10,\"status\":\"PENDING\"}");
    }

    @Test
    void approvalReadTimeout_isClassifiedAsUnknownOutcome() throws Exception {
        HttpServer delayedServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        delayedServer.createContext("/internal/payments/10/approve", exchange -> {
            try {
                Thread.sleep(150);
                byte[] response = "{\"orderId\":10,\"status\":\"APPROVED\"}".getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(HttpStatus.OK.value(), response.length);
                exchange.getResponseBody().write(response);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        delayedServer.start();

        try {
            PaymentClientProperties properties = new PaymentClientProperties();
            properties.setBaseUrl("http://127.0.0.1:" + delayedServer.getAddress().getPort());
            properties.setConnectTimeout(Duration.ofMillis(100));
            properties.setReadTimeout(Duration.ofMillis(20));
            RestClient timeoutClient = new PaymentClientConfig().paymentRestClient(properties);
            PaymentClient client = new PaymentClient(timeoutClient, new ObjectMapper());

            assertThatThrownBy(() -> client.approve(
                    10L, BigDecimal.TEN, BigDecimal.TEN, "key-1"))
                    .isInstanceOf(PaymentOutcomeUnknownException.class)
                    .extracting(exception -> ((PaymentOutcomeUnknownException) exception).getErrorCode())
                    .isEqualTo(OrderErrorCode.PAYMENT_SERVICE_UNAVAILABLE);
        } finally {
            delayedServer.stop(0);
        }
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

    private void assertUnknownApprovalResponse(String body) {
        server.expect(requestTo("http://payment-service/internal/payments/10/approve"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        assertThatThrownBy(this::approve)
                .isInstanceOf(PaymentOutcomeUnknownException.class)
                .extracting(exception -> ((PaymentOutcomeUnknownException) exception).getErrorCode())
                .isEqualTo(OrderErrorCode.PAYMENT_SERVICE_ERROR);
        server.verify();
    }
}
