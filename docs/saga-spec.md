# Fan-Cafe Saga Spec

## 1. 목표

Payment를 독립 서비스/DB 경계로 분리하고,
Order와 Payment 사이에서 발생하는 부분 성공을 안전하게 복구한다.

핵심 문제:
- 결제 성공 후 주문 확정 실패
- 결제 결과를 알 수 없는 Timeout
- 보상(환불) 자체의 실패
- 서버 중단 후 미완료 Saga 복구

최종적으로 모든 Saga가 일관된 상태로 수렴하도록 한다.


## 2. 범위

### 포함
- Payment 서비스 / DB 분리
- 결제 승인
- 결제 상태 조회
- 환불
- 경량 Orchestration Saga
- Saga 상태 영속화
- Recovery Worker
- 환불 멱등성
- Timeout / Late Success 처리
- Crash Recovery
- 장기 미결 건 Slack 알림

### 비범위
- Order / Inventory 추가 서비스 분리
- 전체 MSA 전환
- Kafka 전환
- Temporal / Camunda
- API Gateway
- Service Registry / Discovery
- Saga와 무관한 대규모 리팩터링


## 3. 설계 선택

### 2PC 대신 Saga
- 독립된 로컬 트랜잭션을 장시간 Lock으로 묶지 않는다.
- 이미 승인된 결제는 DB Rollback이 아니라 환불이라는 비즈니스 보상이 필요하다.
- 강한 원자성 대신 최종 일관성으로 수렴한다.

### Choreography 대신 Orchestration
- 결제 Timeout / 보상 / Crash Recovery 상태를 명시적으로 추적해야 한다.
- 전체 진행 상태와 다음 행동을 한 곳에서 관리한다.

### 외부 Workflow Engine 미사용
Order Service 내부에 경량 Orchestrator를 구현한다.

구성:
- saga_instance
- FSM 상태 전이 로직
- Recovery Worker


## 4. Saga States

- STARTED
- PAYMENT_PENDING
- PAYMENT_UNKNOWN
- PAYMENT_COMPLETED
- COMPENSATING
- COMPLETED
- CANCELLED
- COMPENSATED
- RECONCILIATION_REQUIRED


## 5. 정상 흐름

STARTED
→ PAYMENT_PENDING
→ PAYMENT_COMPLETED
→ COMPLETED


## 6. 실패 / 보상 흐름

### 결제 실패 확정

PAYMENT_PENDING
→ CANCELLED


### 결제 Timeout

PAYMENT_PENDING
→ PAYMENT_UNKNOWN
→ Payment 상태 조회

조회 결과:

- APPROVED
  → PAYMENT_COMPLETED
  → 주문 확정

- FAILED / 결제되지 않음이 확정
  → CANCELLED

- 조회 불가
  → PAYMENT_UNKNOWN 유지
  → Recovery Worker가 재조회


### 결제 성공 후 주문 확정 실패

PAYMENT_COMPLETED
→ COMPENSATING
→ 환불
→ COMPENSATED


### 환불 실패 / Timeout

COMPENSATING
→ Recovery Worker 재시도

재시도 한도 초과:
→ RECONCILIATION_REQUIRED
→ Slack Alert


### Late Success

주문을 더 이상 진행할 수 없는 상태에서
뒤늦게 결제 성공이 확인되면:

→ COMPENSATING
→ 환불
→ COMPENSATED


## 7. 핵심 규칙

### Timeout != Failure
응답을 받지 못했다는 이유만으로 결제 실패로 판단하지 않는다.

결제 결과가 불확실하면 PAYMENT_UNKNOWN으로 관리하고
Payment 서비스의 상태조회 기능을 통해 실제 상태를 확인한다.


### Compensation Idempotency

환불 요청은 멱등해야 한다.

Idempotency Key:

REFUND:{sagaId}

동일한 보상 요청이 여러 번 도착해도
실제 환불은 한 번만 수행한다.


### Saga State + Command Atomicity

Saga 상태 변경과 다음 Saga Command의 Outbox 저장은
동일한 Order 로컬 트랜잭션에서 처리한다.

예:

Transaction
- saga_instance → COMPENSATING
- Outbox → REFUND_PAYMENT 저장
- COMMIT

이후 Poller가 RabbitMQ로 REFUND_PAYMENT를 발행한다.


### Crash Recovery

Saga 진행 상태는 메모리가 아니라 DB에 영속화한다.

서버 재시작 후에도 미완료 Saga를 조회하여
중단된 처리를 재개할 수 있어야 한다.


### Recovery

Recovery Worker는
PAYMENT_UNKNOWN / COMPENSATING 등의 미완료 Saga 중

next_retry_at <= NOW()

인 대상을 조회하여 다시 처리한다.

재시도 한도를 초과해도 Saga 레코드를 삭제하지 않는다.

RECONCILIATION_REQUIRED 상태로 전환하고
Slack으로 운영자에게 알린다.


## 8. saga_instance

- saga_id
- order_id
- status
- current_step
- retry_count
- next_retry_at
- last_error
- created_at
- updated_at

Recovery Worker 조회를 위해
status + next_retry_at 기반 인덱스를 둔다.


## 9. 사용자 상태

결제 결과나 보상 결과가 아직 확정되지 않은 경우
사용자에게 성공/실패를 섣불리 확정해서 보여주지 않는다.

필요 시 PROCESSING 상태로 노출하고,
Saga 종료 후 최종 상태로 전환한다.


## 10. 완료 조건

다음 시나리오를 테스트로 검증하면 이번 Saga 확장을 종료한다.

1. 정상 결제 + 주문 완료
2. 결제 실패
3. 결제 Timeout → 상태조회 → 정상 복구
4. 결제 성공 → 주문 확정 실패 → 환불
5. 동일 환불 요청 중복 → 실제 환불 1회
6. 환불 실패 / Timeout → 재시도
7. 재시도 초과 → RECONCILIATION_REQUIRED + Slack
8. Saga 처리 중 Order 서버 종료 → 재시작 후 복구
9. Late Payment Success → 자동 환불