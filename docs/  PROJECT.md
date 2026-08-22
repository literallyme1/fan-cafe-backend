# Fan-Cafe Project Context

## 1. Project Goal
팬카페 굿즈 주문/결제 백엔드 프로젝트.

결제 과정에서 발생할 수 있는
- 중복 처리
- 이벤트 유실
- 일시적 장애
- 성능 병목
- 운영 실패

를 통제해 결제 흐름의 신뢰성을 높이는 것을 주요 목표로 한다.

현재 Saga 확장을 통해
독립된 트랜잭션 경계에서 발생하는 부분 성공과 보상 문제까지 다루려 한다.


## 2. Current Architecture

현재는 하나의 Spring Boot 애플리케이션과 하나의 MySQL DB를 중심으로 구성되어 있다.

주요 책임:

### Order
- 주문 생성 및 상태 관리
- 주문 처리의 핵심 비즈니스 상태 보유

### Payment
- 결제 처리
- PG Webhook 검증
- 결제 결과 반영
- 현재는 Order와 동일 애플리케이션/DB 내부에 존재

### Outbox
- 비즈니스 상태 변경과 이벤트 저장을 동일 로컬 트랜잭션으로 처리
- DB commit 이후 MQ 발행 과정에서 발생할 수 있는 이벤트 유실 방지

### Poller
- Outbox 이벤트 조회
- RabbitMQ 발행
- SKIP LOCKED를 통한 병렬 처리
- 복합 인덱스를 통한 조회 성능 개선

### RabbitMQ
- 비동기 이벤트 전달
- 알림 등의 후속 처리 연결

### Notification
- RabbitMQ 이벤트를 받아 알림 처리


## 3. Tech Stack

- Java
- Spring Boot
- Spring Data JPA
- MySQL
- Redis
- RabbitMQ
- Docker
- AWS EC2
- Micrometer / Actuator
- CloudWatch
- k6


## 4. Existing Reliability Rules

### Transaction
- 핵심 비즈니스 상태 변경과 Outbox 저장은 하나의 로컬 트랜잭션으로 처리한다.

### Message Delivery
- MQ는 중복 전달 가능성을 전제로 한다.
- 이벤트 소비 로직은 가능한 한 멱등하게 처리한다.

### Payment
- Webhook은 HMAC, 금액, timestamp 등을 검증한다.
- 중복 결제/요청을 방지하기 위한 멱등성 처리를 유지한다.

### Failure Handling
- 일시적 실패는 Retry + Backoff/Jitter로 처리한다.
- 반복 실패는 DLQ 및 운영 복구 대상으로 넘긴다.

### Observability
- TraceId를 이용해 요청과 이벤트 흐름을 추적한다.


## 5. Current Extension: Saga

이번 작업에서는 전체 시스템을 MSA로 전환하지 않는다.

기존 Order 영역은 유지하고,
Payment 책임만 독립된 서비스와 DB 경계로 최소 분리한다.

목적은 MSA 자체가 아니라,
독립된 트랜잭션 경계에서 발생하는 부분 성공과 보상 문제를
Saga를 통해 설계하고 검증하는 것이다.


## 6. Constraints

- Order / Inventory 등을 추가로 서비스 분리하지 않는다.
- Kafka로 전환하지 않는다.
- Temporal / Camunda 같은 외부 Workflow Engine을 도입하지 않는다.
- API Gateway, Service Registry 등 전체 MSA 인프라를 구축하지 않는다.
- 기존 RabbitMQ / Outbox / 멱등성 구조는 가능한 한 재사용한다.
- 불필요한 신규 의존성은 추가하지 않는다.
- 현재 Saga 작업과 관계없는 기존 기능은 대규모 리팩터링하지 않는다.


## 7. Development Rule

새 기능은 다음 순서로 진행한다.

문제/책임/범위
→ 요구사항/제약
→ 설계 및 선택 근거
→ 작은 구현 Step
→ AI 구현
→ 테스트
→ 인간 리뷰

AI는 구현을 담당하지만,
아키텍처 경계와 주요 기술적 결정은 개발자가 직접 결정한다.

## Code / Review Rules

- 책임이 이름에서 드러나는 코드를 우선한다.
- 불필요한 추상화와 패턴 추가를 피한다.
- 하나의 메서드는 하나의 주요 책임을 갖도록 한다.
- 상태 전이 로직을 여러 위치에 흩뜨리지 않는다.
- 서비스는 다른 서비스의 DB에 직접 접근하지 않는다.
- 트랜잭션 경계를 명확하게 유지한다.
- 실패 원인을 하나의 catch로 뭉개지 않는다.
- 중요한 정상/실패 시나리오는 테스트로 증명한다.
- 코드 변경은 현재 Issue/Step의 범위를 벗어나지 않는다.
- 구현 후 다음을 설명할 수 있어야 한다:
    1. 데이터 흐름
    2. 책임 경계
    3. 트랜잭션 경계
    4. 실패 시 상태 변화
    5. 해당 설계를 선택한 이유