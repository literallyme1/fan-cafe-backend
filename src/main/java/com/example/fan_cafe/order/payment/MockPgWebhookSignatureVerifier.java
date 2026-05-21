package com.example.fan_cafe.order.payment;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.order.config.MockPgProperties;
import com.example.fan_cafe.order.exception.OrderErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Mock PG 웹훅 HMAC-SHA256 서명 검증.
 * 서명 문자열: {@code timestamp + "." + rawRequestBody}
 */
@Component
@RequiredArgsConstructor
public class MockPgWebhookSignatureVerifier {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    /** 허용 시각 오차(초) — 5분 */
    private static final long TIMESTAMP_TOLERANCE_SECONDS = 300;

    private final MockPgProperties mockPgProperties;

    /**
     * 서명·타임스탬프 검증. 실패 시 예외만 던지며 주문/Outbox는 건드리지 않는다.
     */
    public void verify(String timestamp, String rawBody, String signature) {
        if (timestamp == null || timestamp.isBlank()
                || signature == null || signature.isBlank()
                || rawBody == null) {
            throw new CustomException(OrderErrorCode.WEBHOOK_SIGNATURE_INVALID);
        }

        String secret = mockPgProperties.getWebhookSecret();
        if (secret == null || secret.isBlank()) {
            throw new CustomException(OrderErrorCode.WEBHOOK_SIGNATURE_INVALID);
        }

        long requestEpochSeconds = parseEpochSeconds(timestamp);
        long now = Instant.now().getEpochSecond();
        if (Math.abs(now - requestEpochSeconds) > TIMESTAMP_TOLERANCE_SECONDS) {
            throw new CustomException(OrderErrorCode.WEBHOOK_TIMESTAMP_EXPIRED);
        }

        String signedPayload = timestamp + "." + rawBody;
        String expected = hmacSha256Hex(secret, signedPayload);
        String normalizedExpected = expected.toLowerCase();
        String normalizedProvided = signature.trim().toLowerCase();

        if (!constantTimeEquals(normalizedExpected, normalizedProvided)) {
            throw new CustomException(OrderErrorCode.WEBHOOK_SIGNATURE_INVALID);
        }
    }

    /** 테스트·문서용: 동일 규칙으로 서명 hex 생성 */
    public static String signForTest(String secret, String timestamp, String rawBody) {
        return hmacSha256Hex(secret, timestamp + "." + rawBody).toLowerCase();
    }

    private static long parseEpochSeconds(String timestamp) {
        try {
            return Long.parseLong(timestamp.trim());
        } catch (NumberFormatException e) {
            throw new CustomException(OrderErrorCode.WEBHOOK_TIMESTAMP_INVALID);
        }
    }

    private static String hmacSha256Hex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new CustomException(OrderErrorCode.WEBHOOK_SIGNATURE_INVALID);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
