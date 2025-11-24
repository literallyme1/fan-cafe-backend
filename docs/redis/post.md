# Redis 설계 – Post 도메인

## 🎯 1. 목적 (Purpose)
- Post 도메인의 **조회 성능 개선**
- DB read 부하 감소
- likeCount / commentCount 같은 **변화가 잦은 데이터의 실시간 관리**
- Post 본문 캐싱을 통한 리스트/상세 조회 속도 향상

---

## 🔍 2. 적용 범위 (Scope)
- **정적 데이터:** Post 본문(title, content, imageUrl…)
- **동적 데이터:** likeCount, commentCount
- **부가 데이터:** 좋아요 중복 방지 Set (필요 시)

---

## 📌 3. Redis Key / 자료구조 / TTL 설계

| 구분 | Key 패턴 | 자료구조 | Value 예시 | TTL | 설명 |
|------|-----------|-----------|------------|------|-------|
| 게시글 본문 캐싱 | `post:{id}:data` | String(JSON) | `{"title": "...", "content": "..."}` | **1~5분** | Post 정적 데이터 캐싱 (Cache Aside) |
| 좋아요 수 | `post:{id}:like_count` | String(Integer) | `42` | **없음** | 좋아요 수 실시간 증가(INCR) |
| 댓글 수 | `post:{id}:comment_count` | String(Integer) | `7` | **없음** | 댓글 수 실시간 증가(INCR) |
| **좋아요 유저** | `post:{id}:likes` | **Set** | 없음 | 중복 클릭 방지용 유저 ID 집합.<br>`SISMEMBER`로 체크. |

---

# 4. 정합성 및 장애 대응 (Consistency & Fallback)

### ✔ Redis 장애 시
- Redis 연결 오류 발생 시, 서비스는 멈추지 않고 **DB 직접 조회/갱신**으로 우회한다.
- Redis는 성능 개선용이므로, 장애 시 **핵심 기능은 DB를 통해 보장한다.**

### ✔ 데이터 불일치 시나리오
- Redis의 count와 DB의 count가 다를 가능성이 있기 때문에  
  이벤트 기반 DB 업데이트를 기준 데이터로 삼는다.
- 필요 시, 주기적 스케줄러(선택적)를 통해 Redis → DB로 동기화 가능.

### ✔ 정책 요약
- Redis = 실시간 성능용 / 임시 저장소
- DB = 최종 데이터 소스(Source of Truth)