import http from 'k6/http';
import { check, fail } from 'k6';
import { Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const VUS = Number(__ENV.VUS || 50);
const ORDER_COUNT = Number(__ENV.ORDER_COUNT || 20000);
const PRODUCT_ID = Number(__ENV.PRODUCT_ID || 8000001);
const APPROVAL_AMOUNT = Number(__ENV.APPROVAL_AMOUNT || 9000);
const EMAIL = __ENV.EXPERIMENT_EMAIL || 'saga-step8@fan-cafe.test';
const PASSWORD = __ENV.EXPERIMENT_PASSWORD || 'Step82026';

const createdOrders = new Counter('saga_experiment_orders_created');
const approvalRequests = new Counter('saga_experiment_approvals_requested');

export const options = {
  scenarios: {
    saga_partial_success: {
      executor: 'shared-iterations',
      vus: VUS,
      iterations: ORDER_COUNT,
      maxDuration: __ENV.MAX_DURATION || '30m',
    },
  },
  thresholds: {
    checks: ['rate>0.99'],
    http_req_failed: ['rate<0.02'],
    saga_experiment_orders_created: [`count==${ORDER_COUNT}`],
    saga_experiment_approvals_requested: [`count==${ORDER_COUNT}`],
  },
};

function jsonHeaders(token) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = `Bearer ${token}`;
  return headers;
}

function login() {
  return http.post(`${BASE_URL}/auth/login`, JSON.stringify({
    email: EMAIL,
    password: PASSWORD,
    rememberMe: false,
  }), { headers: jsonHeaders() });
}

export function setup() {
  let loginResponse = login();
  if (loginResponse.status !== 200) {
    const registerResponse = http.post(`${BASE_URL}/auth/register`, JSON.stringify({
      email: EMAIL,
      password: PASSWORD,
      confirmPassword: PASSWORD,
      nickname: 'sagaStep8',
    }), { headers: jsonHeaders() });
    check(registerResponse, { 'experiment user registered': (response) => response.status === 200 });
    loginResponse = login();
  }

  if (loginResponse.status !== 200) {
    fail(`experiment login failed: status=${loginResponse.status}, body=${loginResponse.body}`);
  }
  const token = loginResponse.json('data.accessToken');
  if (!token) fail('experiment login response has no access token');
  return { token };
}

export default function (data) {
  const createResponse = http.post(`${BASE_URL}/orders`, JSON.stringify({
    items: [{ productId: PRODUCT_ID, quantity: 1 }],
  }), {
    headers: jsonHeaders(data.token),
    timeout: '10s',
    tags: { name: 'create-order' },
  });

  const created = check(createResponse, {
    'order created': (response) => response.status === 200 && response.json('data.orderId') != null,
  });
  if (!created) return;
  createdOrders.add(1);

  const orderId = createResponse.json('data.orderId');
  const paymentKey = `STEP8-PARTIAL-${orderId}`;
  const approvalResponse = http.post(
    `${BASE_URL}/api/orders/${orderId}/mock-payment/approve`,
    JSON.stringify({ approvalAmount: APPROVAL_AMOUNT, idempotencyKey: paymentKey }),
    {
      headers: jsonHeaders(data.token),
      timeout: '15s',
      tags: { name: 'approve-payment' },
    },
  );
  approvalRequests.add(1);
  check(approvalResponse, {
    'approval request converged': (response) => response.status === 200,
  });
}
