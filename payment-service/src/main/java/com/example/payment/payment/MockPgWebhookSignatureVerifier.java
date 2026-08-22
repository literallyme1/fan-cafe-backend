package com.example.payment.payment;

import com.example.payment.config.MockPgProperties;
import com.example.payment.exception.PaymentErrorCode;
import com.example.payment.exception.PaymentException;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

@Component
public class MockPgWebhookSignatureVerifier {
    private static final long ALLOWED_SKEW_SECONDS = 300;
    private final MockPgProperties properties;

    public MockPgWebhookSignatureVerifier(MockPgProperties properties) {
        this.properties = properties;
    }

    public void verify(String timestamp, String rawBody, String signature) {
        if (timestamp == null || rawBody == null || signature == null) {
            throw new PaymentException(PaymentErrorCode.WEBHOOK_SIGNATURE_INVALID);
        }
        long requestTime = parseTimestamp(timestamp);
        if (Math.abs(Instant.now().getEpochSecond() - requestTime) > ALLOWED_SKEW_SECONDS) {
            throw new PaymentException(PaymentErrorCode.WEBHOOK_TIMESTAMP_EXPIRED);
        }

        String expected = sign(properties.getWebhookSecret(), timestamp, rawBody);
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.trim().toLowerCase().getBytes(StandardCharsets.UTF_8))) {
            throw new PaymentException(PaymentErrorCode.WEBHOOK_SIGNATURE_INVALID);
        }
    }

    private long parseTimestamp(String timestamp) {
        try {
            return Long.parseLong(timestamp.trim());
        } catch (NumberFormatException exception) {
            throw new PaymentException(PaymentErrorCode.WEBHOOK_TIMESTAMP_INVALID);
        }
    }

    public static String sign(String secret, String timestamp, String rawBody) {
        if (secret == null || secret.isBlank()) {
            throw new PaymentException(PaymentErrorCode.WEBHOOK_SIGNATURE_INVALID);
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(
                    (timestamp + "." + rawBody).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new PaymentException(PaymentErrorCode.WEBHOOK_SIGNATURE_INVALID);
        }
    }
}
