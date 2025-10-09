# 🧩 API Gap List
> 프론트 개발 중 발견된 백엔드 API 누락·불일치 목록  
> 수정은 하지 않고, 먼저 **현황을 전수조사**하는 단계입니다.

---

## 📘 Posts (게시글)
| 구분 | 항목 | 상태 | 설명 |
|------|------|--------|------|
| 응답 필드 | `authorId`, `authorAvatarUrl` | ❌ 없음 | 프론트에서 게시글 작성자 프로필 표시 시 필요. `PostResponse`에 필드 추가 |
| 목록 구조 | `nextCursor` | ❌ 없음 | 현재 `PostListResponse`는 page 기반. 커서 기반 응답으로 변경 필요 → `{ content: Post[], nextCursor: { id, at } }` 형태로 반환 |
| 요청 파라미터 | `cursorId`, `cursorAt` | ❌ 없음 | 현재 `page` 기반으로 댓글 목록 요청을 처리 중. 커서 기반으로 변경 필요 (`@RequestParam(required = false) Long cursorId, String cursorAt`) |


---

## 💬 Comments (댓글)
| 구분 | 항목 | 상태 | 설명 |
|------|------|--------|------|
| 응답 필드 | `authorId`, `authorAvatarUrl`, `likeCount`, `liked` | ❌ 없음 | 프론트에서 댓글 작성자 정보 및 좋아요 상태 표시 시 필요. `CommentResponse`에 필드 추가 필요 |
| 목록 구조 | `nextCursor` | ❌ 없음 | 현재 `CommentListResponse`는 page 기반. 커서 기반 응답으로 변경 필요 → `{ content: Comment[], nextCursor: { id, at } }` 형태로 반환 |
| 요청 파라미터 | `cursorId`, `cursorAt` | ❌ 없음 | 현재 `page` 기반으로 댓글 목록 요청을 처리 중. 커서 기반으로 변경 필요 (`@RequestParam(required = false) Long cursorId, String cursorAt`) |

---

### 🧾 Like Domain

| 구분 | 항목 | 상태 | 설명                                                                                                 |
|------|------|--------|----------------------------------------------------------------------------------------------------|
| 도메인 구조 | `post.like`, `comment.like` | ❌ 없음 | 현재 `/like` 통합 도메인 사용 중. DDD 기준 Post/Comment 각각의 하위 도메인으로 분리 필요                                     |
| 요청 방식 | Path Parameter 기반 | ❌ 불일치 | 현재 body `{ targetId, targetType }` 형식. 도메인별 경로(`/posts/{id}/like`, `/comments/{id}/like`)로 변경 필요   |
| 응답 구조 | `void (200 OK)` | ❌ 불일치 | 현재 `LikeResponse` 반환 중. 상태코드만 반환하도록 수정 필요                                                          |
| 페이징 | 제거 | ❌ 불필요 | 좋아요 목록(`LikeListResponse`)은 페이지 기반. 단순 토글용 API에서는 제거 권장                                            |
| 공통 필드 구조 | `BaseLike` 추가 | ❌ 없음 | `user`, `createdAt` 등의 공통 필드를 like domain 에 공통 맵퍼 클래스  `@MappedSuperclass BaseLike`로 분리하여 중복 제거 필요 |

#### 예상 폴더 구조 

```
src/
┗ features/
┣ post/
┃ ┣ domain/
┃ ┃ ┣ Post.java
┃ ┃ ┣ PostLike.java
┃ ┣ application/
┃ ┃ ┣ PostService.java
┃ ┃ ┗ PostLikeService.java
┣ comment/
┃ ┣ domain/
┃ ┃ ┣ Comment.java
┃ ┃ ┣ CommentLike.java
┃ ┣ application/
┃ ┃ ┣ CommentService.java
┃ ┃ ┗ CommentLikeService.java
┣ like/
┃ ┣ domain/
┃ ┃ ┗ BaseLike.java ← 공통 필드 전용 (도메인 아님)
```
---

## 👤 Profiles (프로필 / 팔로우)
| 기능 | Method | Endpoint | 상태 | 비고 |
|------|---------|-----------|--------|------|
| 내 프로필 조회 | GET | `/users/me` | ⚠️ 확인 필요 | JWT 기반 인증 연동 필요 |
| 사용자 프로필 조회 | GET | `/users/{userId}` | ❌ 없음 | 프론트에서 필요 |
| 팔로우 등록 | POST | `/users/{userId}/follow` | ✅ 완료 | |
| 팔로우 취소 | DELETE | `/users/{userId}/follow` | ❌ 없음 | toggle API 필요 |
| 팔로워 목록 조회 | GET | `/users/{userId}/followers` | ✅ 완료 | |
| 팔로잉 목록 조회 | GET | `/users/{userId}/following` | ✅ 완료 | |

---

## 🛍️ Merchandise (굿즈)
| 기능 | Method | Endpoint | 상태 | 비고 |
|------|---------|-----------|--------|------|
| 카테고리 목록 조회 | GET | `/merch/categories` | ✅ 완료 | |
| 상품 목록 조회 | GET | `/merch/products` | ✅ 완료 | size, cursor 확인 |
| 상품 상세 조회 | GET | `/merch/products/{id}` | ⚠️ 확인 필요 | 옵션 데이터 포함 여부 |
| 장바구니 추가 | POST | `/merch/cart` | ❌ 없음 | |
| 장바구니 삭제 | DELETE | `/merch/cart/{id}` | ❌ 없음 | |

---

## ⚙️ 공통 정책 / 기술 검토
| 항목 | 상태 | 설명 |
|------|--------|------|
| 인증 (JWT) | ✅ 완료 | Access / Refresh 구조 |
| 캐시 (Redis) | ⚠️ 부분 적용 | Refresh Token만 |
| Soft Delete | ⚠️ 일부 적용 | Post만 적용 |
| Notification | ✅ | RabbitMQ 사용 |
| S3 업로드 | ✅ | `S3Uploader` 적용 |
| 에러 코드 통일 | ⚠️ 미흡 | GlobalErrorCode 정리 필요 |

---

## 🗂 수정 우선순위 (Phase Plan)
| 단계 | 목표 | 주요 항목 |
|------|------|------------|
| Phase 1 | 필수 API 완성 | 댓글 좋아요 취소, 게시글 상세, 프로필 조회 |
| Phase 2 | 통일성 리팩토링 | DTO 네이밍, soft-delete, 정책 클래스 정리 |
| Phase 3 | 문서화 및 Swagger | Springdoc 적용, Swagger UI 구축 |

---

## 🧾 작성 가이드
- ✅ 완료: 완성된 기능 (테스트 확인됨)
- ⚠️ 확인 필요: 구조는 있으나 스펙 불일치 가능성 있음
- ❌ 없음: 백엔드에 엔드포인트 자체가 없음
- **비고** 칸에는 DTO, Response 구조 등 주석 달기

---

📅 마지막 업데이트: YYYY-MM-DD  
✍️ 작성자: Taan
