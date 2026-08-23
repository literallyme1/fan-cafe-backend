# Saga Step 8 Experiment Harness

이 디렉터리는 실험 결과가 아니라 동일한 로컬 Docker/MySQL 조건을 반복 구성하기 위한 harness다.
Order DB와 Payment DB는 실험 전용 데이터베이스를 사용하고 다른 부하를 함께 실행하지 않는다.

## 공통 준비

PowerShell 기준 명령이다. 프로젝트 루트에서 실행한다.

```powershell
docker compose -f docker-compose.yml -f docker-compose.experiment.yml up -d db payment-db redis rabbitmq

Get-Content -Raw docs/db/step8-add-saga-observability.sql |
  docker compose exec -T db sh -lc 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" fan_cafe'
```

Migration은 기존 `saga_instance`에 적용하며, 이미 컬럼이 있는 경우에도 안전하게 재실행할 수 있다.
Order 애플리케이션은 experiment profile에서 `ddl-auto=validate`를 사용하므로 migration 적용 후
시작해야 한다. Step 1~7 스키마가 없는 빈 DB를 생성하는 migration은 이 harness의 범위가 아니다.

## 실험 1: Payment Partial Success 수렴

상품 ID는 `8000001`이다. 주문은 실험 전용 사용자(`saga-step8@fan-cafe.test`)로 식별하며,
Payment 데이터는 `STEP8-PARTIAL-*` payment key로 식별한다. 따라서 다른 실험이 더 높은 Order ID를
사용한 뒤에도 reset/result SQL이 실제 실험 주문을 놓치지 않는다. 장애 대상은 실제 생성된 orderId의
hash로 결정되므로 같은 100개 연속 ID 구간마다 20개가 선택된다.

### 1. Reset

```powershell
docker compose stop app payment-service

Get-Content -Raw docs/experiments/saga-step8/experiment1-reset-order.sql |
  docker compose exec -T db sh -lc 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" fan_cafe'

Get-Content -Raw docs/experiments/saga-step8/experiment1-reset-payment.sql |
  docker compose exec -T payment-db sh -lc 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" payment_db'
```

### 2. 서비스 시작

실험 1에서만 partial-success 지연을 활성화한다.

```powershell
$env:EXPERIMENT_PAYMENT_PARTIAL_SUCCESS_ENABLED = "true"
$env:EXPERIMENT_PAYMENT_PARTIAL_SUCCESS_PERCENT = "20"
$env:EXPERIMENT_PAYMENT_APPROVAL_RESPONSE_DELAY = "5s"
$env:SAGA_RECOVERY_CONCURRENCY = "1"

docker compose -f docker-compose.yml -f docker-compose.experiment.yml up -d payment-service app
```

Order의 Payment read timeout은 experiment profile에서 2초다. Payment는 APPROVED 트랜잭션을
commit한 뒤 선택된 승인 응답만 5초 지연한다.

### 3. k6

```powershell
k6 run -e VUS=50 -e BASE_URL=http://localhost:8080 k6/saga-partial-success.js
```

`VUS`는 자유롭게 변경할 수 있고 총 iteration은 항상 20,000건이다. 스크립트는 회원가입/로그인,
주문 생성, 결제 승인이라는 실제 HTTP API 흐름을 사용한다.

### 4. 결과 조회

진행 중 요청과 Recovery가 끝난 뒤 실행한다.

```powershell
Get-Content -Raw docs/experiments/saga-step8/experiment1-result.sql |
  docker compose exec -T db sh -lc 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" fan_cafe'
```

결과 SQL은 UNKNOWN 발생 수, 미수렴 수, 최종 상태, 자동 수렴률, 개별 convergence time과
`ROW_NUMBER()` 기반 p50/p95를 조회한다. 결과 수치는 이 저장소에 기록하지 않는다.

## 실험 2: Recovery Worker 동시성

고정 범위는 Order ID `8100001..8104000`이다. Order에는 due `PAYMENT_UNKNOWN` Saga 4,000건,
Payment에는 동일 Order ID의 APPROVED 결제 4,000건을 만든다.

실험 2에서는 반드시 다음 값을 유지한다.

```powershell
$env:EXPERIMENT_PAYMENT_PARTIAL_SUCCESS_ENABLED = "false"
```

Response delay는 승인 POST에만 적용되지만, 실험 2에서는 기능 자체도 끈다. 따라서 모든 concurrency
실험에서 Payment status GET 조건과 latency가 동일하다.

### 1. 각 concurrency 실행 전 reset/seed

```powershell
docker compose stop app payment-service

Get-Content -Raw docs/experiments/saga-step8/experiment2-reset-order.sql |
  docker compose exec -T db sh -lc 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" fan_cafe'
Get-Content -Raw docs/experiments/saga-step8/experiment2-reset-payment.sql |
  docker compose exec -T payment-db sh -lc 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" payment_db'

Get-Content -Raw docs/experiments/saga-step8/experiment2-seed-order.sql |
  docker compose exec -T db sh -lc 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" fan_cafe'
Get-Content -Raw docs/experiments/saga-step8/experiment2-seed-payment.sql |
  docker compose exec -T payment-db sh -lc 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" payment_db'
```

### 2. Payment 시작 및 BEFORE snapshot

아래 `run_id`는 실행마다 고유하게 정한다.

```powershell
$env:EXPERIMENT_PAYMENT_PARTIAL_SUCCESS_ENABLED = "false"
docker compose -f docker-compose.yml -f docker-compose.experiment.yml up -d payment-service

$runId = "concurrency-1-run-1"
$env:SAGA_RECOVERY_EXPERIMENT_RUN_ID = $runId
$beforeSql = "SET @run_id='$runId'; SET @snapshot_phase='BEFORE';`n" +
  (Get-Content -Raw docs/experiments/saga-step8/mysql-lock-snapshot.sql)
$beforeSql | docker compose exec -T db sh -lc 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" fan_cafe'
```

### 3. 지정 concurrency로 Order 시작

허용 비교값은 `1`, `2`, `4`, `8`, `10`이다.

```powershell
$env:SAGA_RECOVERY_CONCURRENCY = "1"
$env:SAGA_RECOVERY_EXPERIMENT_RUN_ID = $runId
docker compose -f docker-compose.yml -f docker-compose.experiment.yml up -d app
```

experiment 전용 coordinator가 지정한 수의 고정 worker lane을 만들고, 각 lane이 기존
`claimNext()`를 동시에 호출한다. 기본 production scheduler는 experiment profile에서 생성되지 않는다.

완료 여부는 다음처럼 확인한다.

```powershell
"SELECT status, COUNT(*) FROM saga_instance WHERE order_id BETWEEN 8100001 AND 8104000 GROUP BY status;" |
  docker compose exec -T db sh -lc 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" fan_cafe'
```

### 4. AFTER snapshot과 delta

4,000건이 처리된 시점에 즉시 snapshot을 남긴다.

```powershell
$afterSql = "SET @run_id='$runId'; SET @snapshot_phase='AFTER';`n" +
  (Get-Content -Raw docs/experiments/saga-step8/mysql-lock-snapshot.sql)
$afterSql | docker compose exec -T db sh -lc 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" fan_cafe'

$resultSql = "SET @run_id='$runId';`n" +
  (Get-Content -Raw docs/experiments/saga-step8/experiment2-result.sql)
$resultSql | docker compose exec -T db sh -lc 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" fan_cafe'
```

처리시간은 최초 성공 claim 시각부터 대상 Saga의 `MAX(resolved_at)`까지 계산한다. BEFORE/AFTER
`captured_at`은 InnoDB lock counter delta에만 사용한다. 결과에는 처리 Saga 수, 순수 Recovery 처리시간,
Sagas/sec, InnoDB row lock waits와 row lock wait time 차이 및 평균 wait가 포함된다.
MySQL status 값은 전역 누적 counter이므로
같은 서버의 다른 workload가 없어야 비교가 유효하다.

동일 순서를 concurrency `1/2/4/8/10`에 반복한다. Before용 비교 구현이나 `FOR UPDATE`
모드는 이 harness에 포함하지 않는다.
