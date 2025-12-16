 # 🚀 API 성능 측정 기록 (Performance Record)

> 목적: Redis 적용 전/후 API 성능을 정량적으로 비교하고, 개선 효과를 문서화한다.

---

## 📌 1. 측정 환경(Environment)
- OS:
- Java/Spring Boot:
- DB:
- Redis:
- 측정 도구: Postman / Logging Filter
- 네트워크 환경: (ex. 로컬/외부 API 없음)

---

## 📌 2. 측정 기준 (Metrics)
- **외부 응답 시간(ms)**: Postman 기준
- **내부 처리 시간(ms)**: RequestLoggingFilter 기준
- **DB 쿼리 수**: Hibernate SQL 로그 기준

---

## 📌 3. 성능 측정 결과 (Before → After)

### 3-1. 🔵 전체 응답 속도 (Postman 기준)
| API                                         | Before(ms) | After(ms)                                      | 개선율          |
|---------------------------------------------|------------|------------------------------------------------|--------------|
| GET /posts?page=0 (latest)                  | 589ms      | Redis 저장 + Get : 248ms / Redis 에서 가져올 때 : 31ms | **94.7% 개선** |
| POST /comments/{postId}  (10개 연속 create 기준) | 1474ms     | 1238ms                                         | **16.0% 개선** |

---

### 3-2. 🔵 서버 내부 처리 시간 (Logging Filter 기준)
| API                                         | Before(ms) | After(ms)                                    | 개선율          |
|---------------------------------------------|------------|----------------------------------------------|--------------|
| GET /posts?page=0                           | 564ms      | Redis 저장 + Get : 193ms / Redis 에서 가져올 때 : 24ms | **95.7% 개선** |
| POST /comments/{postId}  (10개 연속 create 기준) | 297ms      | 196ms                                           | **34.0% 개선** |

---

### -3. 🔵 DB 쿼리 수
| API | Before | After | 감소율 |
|------|--------|--------|---------|
| GET /posts?page=0 |  |  |  |
| GET /posts/{id} |  |  |  |

---

## 📌 5. 분석 (Analysis)
- 어떤 API에서 개선 효과가 컸는가?
- DB 부하 감소가 실제로 체감되는가?
- Redis 캐싱 전략이 적절했는가?
- 추가 최적화 필요 구간?

---

## 📌 6. 결론 (Conclusion)
- Redis 적용 후 가장 큰 개선 지점:
- 향후 적용할 캐싱 후보:
- 개선 효과 요약:

> 예:  
> “GET /posts?page=0 API는 120ms → 18ms로 85% 개선되었으며,  
> DB 쿼리는 50회 → 1회로 크게 줄어 전체 성능 향상에 기여했다.”

---

## 📌 7. Raw Logs (선택)
필요 시 콘솔 로그 또는 DB 로그 일부 첨부.

