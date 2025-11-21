# 🚀 Tech Upgrade List
> 실무 기술 업그레이드를 위해 추가할 기능 목록입니다.  
> 기존 API Gap List와 동일한 포맷을 유지합니다.

---

# 🧩 Backend Domain Upgrades

## ❤️ Like & Follow Domain
| 상태 | 구분 | 항목 | 설명 |
|------|------|------|------|
| ☐ | 성능 개선 | Redis 캐싱 | 좋아요/팔로우 카운트를 Redis에 캐싱하여 실시간 성능 개선 |
| ☐ | 안정성 | Rate Limiting | 좋아요·팔로우 요청 과부하 방지 (redis-token-bucket) |

---

## 📝 Post & Comment Domain
| 상태 | 구분       | 항목 | 설명                                       |
|------|----------|------|------------------------------------------|
| ☐ | 내부 속도 측정 | RequestLoggingFilter | 내부 속도 측정을 위해 filterChain을 만듦.            |
| ☐ | 비동기 처리   | 이벤트 비동기화 | 댓글/좋아요 이벤트를 RabbitMQ로 비동기 처리하여 API 속도 개선 |
| ☐ | 알림       | Notification Queue | 댓글/좋아요 발생 시 Notification 메시지를 큐로 발행      |

---

# 🛠 Architecture / DDD

## 🧱 Domain Layer
| 상태 | 구분 | 항목 | 설명 |
|------|------|------|------|
| ☐ | 구조 개선 | DDD Layer Refactoring | domain/application/interfaces 구조 재정리 |
| ☐ | 에러 처리 | Exception 통일 | GlobalErrorCode 기반 에러 포맷 일원화 |
| ☐ | 입력 검증 | Validation 강화 | DTO validation + 공통 예외 응답 구조 정비 |

---

# 🧰 Infrastructure Upgrades

## 🐳 Docker & DevOps
| 상태 | 구분 | 항목 | 설명 |
|------|------|------|------|
| ☐ | 환경 구성 | Docker Compose | Spring + DB + Redis + RabbitMQ 통합 개발 환경 구성 |
| ☐ | 자동화 | GitHub Actions CI | PR 시 자동 빌드/테스트 실행 |
| ☐ | 배포 | AWS 배포 구성 | EC2 + RDS + S3 조합으로 배포 환경 구축 |

---

## 📡 Messaging / Caching
| 상태 | 구분 | 항목 | 설명 |
|------|------|------|------|
| ☐ | 캐싱 규칙 | Redis Key 전략 | TTL, key 네이밍 규칙 수립 |
| ☐ | 메시징 | RabbitMQ 로컬 구성 | docker 기반 MQ 설치 및 Spring AMQP 연동 |

---

# 📊 Monitoring / Logging

## 🔍 Logging & Observability
| 상태 | 구분 | 항목 | 설명 |
|------|------|------|------|
| ☐ | 로깅 | JSON 구조적 로깅 | traceId/userId 포함된 JSON Log |
| ☐ | 모니터링 | Actuator Health Check | /actuator/health 기반 상태 모니터링 |
| ☐ | 로깅 | 요청/응답 로깅 | AOP 기반 트래픽 로깅 |

---

# 🧪 Testing Layer

## 🧪 테스트 고도화
| 상태 | 구분 | 항목 | 설명 |
|------|------|------|------|
| ☐ | 단위 테스트 | Controller Test | WebMvcTest로 API 단위 검증 |
| ☐ | 단위 테스트 | Repository Test | JPA/QueryDSL 쿼리 정확성 검증 |
| ☐ | 통합 테스트 | Testcontainers | DB/Redis/MQ 통합 테스트 환경 구축 |

---

# 📄 Documentation

## 📘 API / 운영 문서화
| 상태 | 구분 | 항목 | 설명 |
|------|------|------|------|
| ☐ | API 문서 | Swagger(Springdoc) | 자동 API 문서 및 프론트 협업 지원 |
| ☐ | 운영 문서 | 운영 가이드 | 배포/롤백/장애 대응 문서화 |

---

📅 마지막 업데이트: 2025-11-18  
✍️ 작성자: Taan
