 # 🧩 API Gap List
> 프론트 개발 중 발견된 백엔드 API 누락·불일치 목록  
> 수정은 하지 않고, 먼저 **현황을 전수조사**하는 단계입니다.

---

# 🧭 Domain Layer Gaps (도메인별 Gap)


## 📘 Posts (게시글)

| 상태 | 구분 | 항목 | 설명 |
|------|------|------|------|
| ☐ | 응답 필드 | `authorId`, `authorAvatarUrl` | 프론트에서 게시글 작성자 프로필 표시 시 필요. `PostResponse`에 필드 추가 |
| ☑ | 목록 구조 | `nextCursor` | 현재 `PostListResponse`는 page 기반. 커서 기반 응답으로 변경 필요 → `{ content: Post[], nextCursor: { id, at } }` 형태로 반환 |
| ☑ | 요청 파라미터 | `cursorId`, `cursorAt` | 현재 `page` 기반으로 댓글 목록 요청을 처리 중. 커서 기반으로 변경 필요 (`@RequestParam(required = false) Long cursorId, String cursorAt`) |

---

---

## 💬 Comments (댓글)
| 상태 | 구분 | 항목 | 설명 |
|------|------|------|------|
| ☐ | 응답 필드 | `authorId`, `authorAvatarUrl`, `likeCount`, `liked` | 프론트에서 댓글 작성자 정보 및 좋아요 상태 표시 시 필요. `CommentResponse`에 필드 추가 필요 |
| ☑ | 목록 구조 | `nextCursor` | 현재 `CommentListResponse`는 page 기반. 커서 기반 응답으로 변경 필요 → `{ content: Comment[], nextCursor: { id, at } }` 형태로 반환 |
| ☑ | 요청 파라미터 | `cursorId`, `cursorAt` | 현재 `page` 기반으로 댓글 목록 요청을 처리 중. 커서 기반으로 변경 필요 (`@RequestParam(required = false) Long cursorId, String cursorAt`) |

---

## 🧾 Like Domain


| 상태 | 구분 | 항목 | 설명                                                                                               | 이슈 번호                                                        |
|------|------|------|--------------------------------------------------------------------------------------------------|--------------------------------------------------------------|
| ☑ | 도메인 구조 | `post.like`, `comment.like` | 현재 `/like` 통합 도메인 사용 중. 독립 도메인은 유지하되 각 `post`, `comment` 도메인 `service`에 가져와서 구현                  | [#45](https://github.com/literallyme1/fan-cafe-backend/issues/45) |
| ☑ | 요청 방식 | Path Parameter 기반 | 현재 body `{ targetId, targetType }` 형식. 도메인별 경로(`/posts/{id}/like`, `/comments/{id}/like`)로 변경 필요 | [#3](https://github.com/literallyme1/fan-cafe-backend/issues/3) |
| ☑ | 응답 구조 | `void (200 OK)` | 현재 `LikeResponse` 반환 중. 상태코드만 반환하도록 수정 필요                                                        | [#3](https://github.com/literallyme1/fan-cafe-backend/issues/3) |
| ☑ | 페이징 | 제거 | 좋아요 목록(`LikeListResponse`)은 페이지 기반. 단순 토글용 API에서는 제거 권장                                          |
| ☑ | 조회 구조 | 댓글/대댓글 분리 조회 | 모든 댓글을 한 번에 조회하는 구조 개선 필요 | [#38](https://github.com/literallyme1/fan-cafe-backend/issues/38) |

---

## 👤 Profiles (프로필 / 팔로우) //TODO - 팔로우와 분리 예정
| 상태 | 기능 | Method | Endpoint | 설명                                             | 이슈번호 |
|------|------|-------|-----------|------------------------------------------------| [#53](https://github.com/literallyme1/fan-cafe-backend/issues/53)
| ☐ | 내 프로필 조회 | PUT   | `/users/me` | 기존 `avatarUrl`이 존재 X, 추가 후 put 시 업데이트 가능하도록 설정 |
| ☐ | 내 프로필 조회 | GET   | `/users/me` | JWT 기반 인증 연동 필요                                |
| ☐ | 사용자 프로필 조회 | GET   | `/users/{userId}` | 프론트에서 필요                                       |
| ☐ | 팔로우 등록 | POST  | `/users/{userId}/follow` |                                                |
| ☐ | 팔로우 취소 | DELETE | `/users/{userId}/follow` | toggle API 필요                                  |
| ☐ | 팔로워 목록 조회 | GET   | `/users/{userId}/followers` |                                                |
| ☐ | 팔로잉 목록 조회 | GET   | `/users/{userId}/following` |                                                |

---

## 🛍️ Merchandise (굿즈)
| 상태 | 기능 | Method | Endpoint | 비고 |
|------|------|---------|-----------|------|
| ☐ | 카테고리 목록 조회 | GET | `/merch/categories` |  |
| ☐ | 상품 목록 조회 | GET | `/merch/products` | size, cursor 확인 |
| ⚠️ | 상품 상세 조회 | GET | `/merch/products/{id}` | 옵션 데이터 포함 여부 |
| ☐ | 장바구니 추가 | POST | `/merch/cart` |  |
| ☐ | 장바구니 삭제 | DELETE | `/merch/cart/{id}` |  |

---

# 🧰 Tool & Policy Layer Gaps (도구 / 기술 정책)

## 🔄 Cursor System

| 상태 | 구분 | 항목                                                     | 설명 | 이슈                                                                |
|------|------|--------------------------------------------------------|------|-------------------------------------------------------------------|
| ☑ | 공통 유틸 구현 | `resolveCursor()` 전역 함수                                | 여러 도메인에서 공통으로 사용할 수 있는 커서 해석 함수 구현. `global/common/CursorResolver.java`에 정의, `HasCreatedAt` 인터페이스 상속 도메인만 적용 가능 | [#71](https://github.com/literallyme1/fan-cafe-backend/issues/71) |
| ☑ | 코드 통합 | 커서 생성 로직 통합                                            | 각 서비스에 흩어진 커서 생성 로직을 공통 `resolveCursor`로 통일. `PostService` 및 기타 서비스에서 중복 코드 제거 | [#12](https://github.com/literallyme1/fan-cafe-backend/issues/12) |
| ☑ | 커서 방향 확장 | `CursorUtils.fromFirst()`, `CursorUtils.fromLast()` 추가 | 첫 페이지 진입 시 afterCursor 계산을 위해 `CursorUtils.fromFirst()` 함수 추가 | [#31](https://github.com/literallyme1/fan-cafe-backend/issues/31) |
| ☑ | DTO 개선 | `AfterCursor`, `BeforeCursor` DTO 분리                   | `PostListResponse` 내 커서 정보를 before/after로 구분해 커서 방향 명확화 | [#29](https://github.com/literallyme1/fan-cafe-backend/issues/29) |
| ☑ | 페이지 계산 수정 | `limit(size + 1)` 적용                                   | hasNext 계산 버그 수정. 기존 size 그대로 가져오던 문제를 해결하기 위해 size + 1로 조회 변경 | [#22](https://github.com/literallyme1/fan-cafe-backend/issues/22) |
| ☑ | 최신글 로직 개선 | `findNewPosts()` 커서 방향 구분                              | 기존 `beforeDesc`만 사용하던 문제 해결. `CursorUtils.afterDesc()`를 추가하여 최신글 기준으로 커서 조회 동작 수정 | [#25](https://github.com/literallyme1/fan-cafe-backend/issues/25) |

## ⚙️ 공통 정책 / 기술 검토
| 상태 | 항목 | 설명 |
|----|------|------|
| ☑ | 인증 (JWT) | Access / Refresh 구조 |
| ⚠️ | 캐시 (Redis) | Refresh Token만 적용 |
| ⚠️ | Soft Delete | Post만 적용 |
| ☐ | Notification | RabbitMQ 사용 |
| ☑ | S3 업로드 | `S3Uploader` 적용 |
| ⚠️ | 에러 코드 통일 | GlobalErrorCode 정리 필요 |

---

## 🗂 수정 우선순위 (Phase Plan)
| 상태 | 단계 | 목표 | 주요 항목 |
|------|------|------|------------|
| ☐ | Phase 1 | 필수 API 완성 | 댓글 좋아요 취소, 게시글 상세, 프로필 조회 |
| ☐ | Phase 2 | 통일성 리팩토링 | DTO 네이밍, soft-delete, 정책 클래스 정리 |
| ☐ | Phase 3 | 문서화 및 Swagger | Springdoc 적용, Swagger UI 구축 |

---

## 🧾 작성 가이드
- ✅ 완료: 완성된 기능 (테스트 확인됨)
- ⚠️ 확인 필요: 구조는 있으나 스펙 불일치 가능성 있음
- ❌ 없음: 백엔드에 엔드포인트 자체가 없음
- **비고** 칸에는 DTO, Response 구조 등 주석 달기

---

📅 마지막 업데이트: YYYY-MM-DD  
✍️ 작성자: Taan
