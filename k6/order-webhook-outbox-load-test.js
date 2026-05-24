import http from 'k6/http';
import { check, sleep } from 'k6';
import crypto from 'k6/crypto';

/**
 * Mock PG Webhook → Outbox NEW 생성 → Poller SENT 처리 흐름 부하 테스트
 *
 * 사전 준비 (test profile)
 *   load-test.seed.payment-pending-orders.enabled=true 로 PAYMENT_PENDING 주문 10,000건 시드
 *
 * 실행 예
 *   k6 run k6/order-webhook-outbox-load-test.js
 *   k6 run -e BASE_URL=http://localhost:8080 -e MOCK_PG_WEBHOOK_SECRET=test-mock-pg-webhook-secret k6/order-webhook-outbox-load-test.js
 */

export const options = {
  stages: [
    { duration: '1m', target: 100 },
    { duration: '2m', target: 300 },
    { duration: '2m', target: 500 },
    { duration: '1m', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<1000'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const WEBHOOK_SECRET = __ENV.MOCK_PG_WEBHOOK_SECRET || 'test-mock-pg-webhook-secret';
const WEBHOOK_PATH = '/api/mock-pg/webhook';

const HEADER_TIMESTAMP = 'X-Mock-PG-Timestamp';
const HEADER_SIGNATURE = 'X-Mock-PG-Signature';

// seed-payment-pending-orders.sql 과 동일
const ORDER_ID_START = 900001;
const ORDER_COUNT = 10000;
const APPROVAL_AMOUNT = 9000;

function buildIdempotencyKey(orderId) {
  return `K6-MOCK-PG-${orderId}`;
}

/**
 * OrderIntegrationTestSupport.signedPaymentApprovedWebhook 과 동일한 compact JSON
 * (서명은 수신한 raw body 문자열 기준이므로 공백 없이 고정 포맷 사용)
 */
function buildRawBody(orderId, approvalAmount, idempotencyKey) {
  return `{"eventType":"PAYMENT_APPROVED","orderId":${orderId},"approvalAmount":${approvalAmount},"idempotencyKey":"${idempotencyKey}"}`;
}

/**
 * MockPgWebhookSignatureVerifier: HMAC-SHA256( secret, timestamp + "." + rawBody ) → hex lowercase
 */
function signWebhook(secret, timestamp, rawBody) {
  const signedPayload = `${timestamp}.${rawBody}`;
  return crypto.hmac('sha256', secret, signedPayload, 'hex').toLowerCase();
}

function pickRandomOrderId() {
  return ORDER_ID_START + Math.floor(Math.random() * ORDER_COUNT);
}

export default function () {
  const orderId = pickRandomOrderId();
  const idempotencyKey = buildIdempotencyKey(orderId);
  const rawBody = buildRawBody(orderId, APPROVAL_AMOUNT, idempotencyKey);
  const timestamp = String(Math.floor(Date.now() / 1000));
  const signature = signWebhook(WEBHOOK_SECRET, timestamp, rawBody);

  const res = http.post(`${BASE_URL}${WEBHOOK_PATH}`, rawBody, {
    headers: {
      'Content-Type': 'application/json',
      [HEADER_TIMESTAMP]: timestamp,
      [HEADER_SIGNATURE]: signature,
    },
    tags: { name: 'mock-pg-webhook' },
  });

  check(res, {
    'webhook status 200': (r) => r.status === 200,
    'webhook success code': (r) => {
      try {
        const body = r.json();
        return body.status === 200 && body.code === 'S001';
      } catch (_) {
        return false;
      }
    },
    'webhook returns orderId': (r) => {
      try {
        const body = r.json();
        return body.data && body.data.orderId === orderId;
      } catch (_) {
        return false;
      }
    },
  });

  sleep(Math.random() * 0.2 + 0.1);
}
