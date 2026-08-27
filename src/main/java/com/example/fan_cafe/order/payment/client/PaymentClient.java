package com.example.fan_cafe.order.payment.client;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.order.exception.OrderErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;

@Component
public class PaymentClient {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public PaymentClient(RestClient paymentRestClient, ObjectMapper objectMapper) {
        this.restClient = paymentRestClient;
        this.objectMapper = objectMapper;
    }

    public PaymentResultResponse approve(Long orderId, BigDecimal expected, BigDecimal approved, String key) {
        return executeApproval(orderId, () -> restClient.post()
                .uri("/internal/payments/{orderId}/approve", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new PaymentApproveCommand(expected, approved, key))
                .retrieve().body(PaymentResultResponse.class));
    }

    private PaymentResultResponse executeApproval(Long expectedOrderId, PaymentCall call) {
        try {
            PaymentResultResponse result = call.execute();
            if (!isConclusiveApprovalResult(expectedOrderId, result)) {
                throw new PaymentOutcomeUnknownException(OrderErrorCode.PAYMENT_SERVICE_ERROR);
            }
            return result;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().is5xxServerError()
                    || exception.getStatusCode().value() == 408) {
                throw new PaymentOutcomeUnknownException(OrderErrorCode.PAYMENT_SERVICE_ERROR);
            }
            throw mapRemoteError(exception);
        } catch (ResourceAccessException exception) {
            throw new PaymentOutcomeUnknownException(OrderErrorCode.PAYMENT_SERVICE_UNAVAILABLE);
        } catch (RestClientException exception) {
            OrderErrorCode errorCode = hasIoCause(exception)
                    ? OrderErrorCode.PAYMENT_SERVICE_UNAVAILABLE
                    : OrderErrorCode.PAYMENT_SERVICE_ERROR;
            throw new PaymentOutcomeUnknownException(errorCode);
        }
    }

    private boolean isConclusiveApprovalResult(Long expectedOrderId, PaymentResultResponse result) {
        if (result == null || result.orderId() == null || !result.orderId().equals(expectedOrderId)) {
            return false;
        }
        return (result.status() == PaymentResultStatus.APPROVED && result.approvedAt() != null)
                || result.status() == PaymentResultStatus.FAILED;
    }

    private boolean hasIoCause(Throwable failure) {
        Throwable cause = failure;
        while (cause != null) {
            if (cause instanceof IOException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    public PaymentResultResponse fail(Long orderId, BigDecimal expected, String reason) {
        return execute(() -> restClient.post()
                .uri("/internal/payments/{orderId}/fail", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new PaymentFailCommand(expected, reason))
                .retrieve().body(PaymentResultResponse.class));
    }

    public PaymentStatusResponse getStatus(Long orderId) {
        return executeStatus(() -> restClient.get()
                .uri("/internal/payments/{orderId}", orderId)
                .retrieve().body(PaymentStatusResponse.class));
    }

    public PaymentStatusResponse refund(Long orderId, UUID sagaId, String reason) {
        return executeStatus(() -> restClient.post()
                .uri("/internal/payments/{orderId}/refund", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new PaymentRefundCommand(sagaId, reason))
                .retrieve().body(PaymentStatusResponse.class));
    }

    public PaymentResultResponse forwardWebhook(
            String rawBody, String timestamp, String signature, BigDecimal expectedAmount
    ) {
        return execute(() -> restClient.post()
                .uri(builder -> builder.path("/internal/mock-pg/webhook")
                        .queryParam("expectedAmount", expectedAmount).build())
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Mock-PG-Timestamp", timestamp)
                .header("X-Mock-PG-Signature", signature)
                .body(rawBody)
                .retrieve().body(PaymentResultResponse.class));
    }

    private PaymentResultResponse execute(PaymentCall call) {
        try {
            PaymentResultResponse result = call.execute();
            if (result == null) throw new CustomException(OrderErrorCode.PAYMENT_SERVICE_ERROR);
            return result;
        } catch (RestClientResponseException exception) {
            throw mapRemoteError(exception);
        } catch (RestClientException exception) {
            throw new CustomException(OrderErrorCode.PAYMENT_SERVICE_UNAVAILABLE);
        }
    }

    private PaymentStatusResponse executeStatus(PaymentStatusCall call) {
        try {
            PaymentStatusResponse result = call.execute();
            if (result == null) throw new CustomException(OrderErrorCode.PAYMENT_SERVICE_ERROR);
            return result;
        } catch (RestClientResponseException exception) {
            throw mapRemoteError(exception);
        } catch (RestClientException exception) {
            throw new CustomException(OrderErrorCode.PAYMENT_SERVICE_UNAVAILABLE);
        }
    }

    private CustomException mapRemoteError(RestClientResponseException exception) {
        try {
            PaymentErrorResponse error = objectMapper.readValue(
                    exception.getResponseBodyAsString(), PaymentErrorResponse.class);
            if (error.code() == null) {
                return new CustomException(OrderErrorCode.PAYMENT_SERVICE_ERROR);
            }
            return new CustomException(switch (error.code()) {
                case "P001" -> OrderErrorCode.PAYMENT_KEY_REQUIRED;
                case "P003" -> OrderErrorCode.PAYMENT_NOT_FOUND;
                case "P004", "P006" -> OrderErrorCode.INVALID_PAYMENT_STATE;
                case "P005" -> OrderErrorCode.ORDER_ALREADY_PAID;
                case "P010" -> OrderErrorCode.WEBHOOK_SIGNATURE_INVALID;
                case "P011" -> OrderErrorCode.WEBHOOK_TIMESTAMP_INVALID;
                case "P012" -> OrderErrorCode.WEBHOOK_TIMESTAMP_EXPIRED;
                case "P013", "P016" -> OrderErrorCode.INVALID_WEBHOOK_EVENT_TYPE;
                case "P014" -> OrderErrorCode.WEBHOOK_APPROVAL_AMOUNT_REQUIRED;
                case "P018" -> OrderErrorCode.CANCEL_IDEMPOTENCY_KEY_REQUIRED;
                case "P019" -> OrderErrorCode.ORDER_ALREADY_REFUNDED;
                default -> OrderErrorCode.PAYMENT_SERVICE_ERROR;
            });
        } catch (JsonProcessingException parseFailure) {
            return new CustomException(OrderErrorCode.PAYMENT_SERVICE_ERROR);
        }
    }

    @FunctionalInterface
    private interface PaymentCall { PaymentResultResponse execute(); }

    @FunctionalInterface
    private interface PaymentStatusCall { PaymentStatusResponse execute(); }
}
