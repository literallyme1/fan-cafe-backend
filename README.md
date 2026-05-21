# Fan-Cafe Backend

## Mock 결제 상태 전이 (포트폴리오용)

실제 PG 연동 없이, 주문 생성 → Mock PG 승인/실패 → `PAID` 전이 후 **Transactional Outbox** 알림이 이어지는 흐름을 얇게 보완한 구간입니다.

### 상태 전이 다이어그램

```mermaid
stateDiagram-v2
    [*] --> PAYMENT_PENDING: POST /orders (주문 생성)
    PAYMENT_PENDING --> PAID: POST .../mock-payment/approve (금액 일치)
    PAYMENT_PENDING --> PAYMENT_FAILED: POST .../mock-payment/fail
    PAYMENT_PENDING --> PAYMENT_FAILED: approve 금액 불일치
    PAID --> CANCELLED: PATCH /orders/{id}/cancel
    PAYMENT_PENDING --> CANCELLED: PATCH /orders/{id}/cancel
```

| 전이 | Outbox `ORDER_CREATED` | `order_status_history` |
|------|------------------------|-------------------------|
| 생성 → `PAYMENT_PENDING` | 저장 안 함 | 저장 안 함 |
| 승인 → `PAID` | **저장** (주문 상태와 동일 트랜잭션) | 저장 |
| 실패 / 금액 불일치 → `PAYMENT_FAILED` | 저장 안 함 | 저장 |
| 동일 키로 재승인 (`PAID` + 같은 key) | 저장 안 함 | 저장 안 함 |

### API 예시

기본 주문 API는 `POST /orders`, Mock 결제는 `POST /api/orders/...` 입니다.  
모든 요청에 JWT(`Authorization: Bearer <token>`)가 필요합니다 (local/dev 프로필).

#### 1) 주문 생성 → `PAYMENT_PENDING`

```http
POST http://localhost:8080/orders
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "items": [
    { "productId": 1, "quantity": 2 }
  ]
}
```

응답 예: `status` = `PAYMENT_PENDING`, Outbox 미저장.

#### 2) Mock PG 승인 → `PAID` + Outbox

```http
POST http://localhost:8080/api/orders/{orderId}/mock-payment/approve
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "approvalAmount": 20000,
  "idempotencyKey": "mock-pay-20260521-001"
}
```

- `approvalAmount`는 주문 `totalPrice`와 **반드시 일치**해야 합니다.
- `idempotencyKey` 또는 `mockPaymentKey` 중 하나는 필수입니다.
- 이미 `PAID`이고 **같은 키**면 200 응답만 반환 (Outbox·이력 중복 없음).
- 금액 불일치 시 `PAYMENT_FAILED`로 바뀐 뒤 `O008` 예외.

#### 3) Mock PG 실패 → `PAYMENT_FAILED`

```http
POST http://localhost:8080/api/orders/{orderId}/mock-payment/fail
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "reason": "user cancelled mock checkout"
}
```

Outbox는 저장하지 않습니다.

#### 4) 주문 조회

```http
GET http://localhost:8080/orders/{orderId}
Authorization: Bearer <access_token>
```

### `order_status_history` 테이블

| 컬럼 | 설명 |
|------|------|
| `id` | PK |
| `order_id` | 주문 FK |
| `from_status` | 이전 상태 |
| `to_status` | 변경 후 상태 |
| `reason` | 예: `mock payment approved`, `approval amount mismatch` |
| `created_at` | 기록 시각 |

JPA `ddl-auto=update`(local) 시 자동 생성됩니다.

### 로컬 검증 순서

1. 로그인 → 토큰 발급 (`POST /auth/login`)
2. 주문 생성 → `PAYMENT_PENDING` 확인
3. 승인 API → `PAID` + DB `outbox_events`에 `ORDER_CREATED` 1건
4. 동일 `idempotencyKey`로 재승인 → Outbox 추가 없음
5. (새 주문) 실패 API 또는 잘못된 `approvalAmount` → `PAYMENT_FAILED`, Outbox 없음

기존 Outbox Poller / Consumer / Retry / DLQ / Slack / TraceID 구조는 변경하지 않았습니다.
