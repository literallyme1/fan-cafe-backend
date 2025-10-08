# 🧩 API Gap List
> 프론트 개발 중 발견된 백엔드 API 누락·불일치 목록  
> 수정은 하지 않고, 먼저 **현황을 전수조사**하는 단계입니다.

---

## 📘 Posts (게시글)
| 기능 | Method | Endpoint | 상태 | 비고 |
|------|---------|-----------|--------|------|
| 게시글 목록 조회 | GET | `/posts` | ✅ 완료 | 커서 기반 페이징 확인 필요 |
| 게시글 상세 조회 | GET | `/posts/{postId}` | ❌ 없음 | 프론트에서 필요 |
| 게시글 작성 | POST | `/posts` | ✅ 완료 | DTO 확인 필요 |
| 게시글 수정 | PUT | `/posts/{postId}` | ⚠️ 확인 필요 | 권한 체크 로직 누락 가능 |
| 게시글 삭제 | DELETE | `/posts/{postId}` | ⚠️ 확인 필요 | soft-delete 적용 여부 |
| 게시글 좋아요 등록 | POST | `/posts/{postId}/like` | ✅ 완료 | 정상 작동 |
| 게시글 좋아요 취소 | DELETE | `/posts/{postId}/like` | ❌ 없음 | toggle 로직은 프론트만 존재 |

---

## 💬 Comments (댓글)
| 기능 | Method | Endpoint | 상태 | 비고 |
|------|---------|-----------|--------|------|
| 댓글 목록 조회 | GET | `/posts/{postId}/comments` | ✅ 완료 | 커서 기반 페이징 |
| 댓글 작성 | POST | `/posts/{postId}/comments` | ✅ 완료 | DTO 이름 확인 |
| 댓글 좋아요 등록 | POST | `/comments/{commentId}/like` | ✅ 완료 | 정상 작동 |
| 댓글 좋아요 취소 | DELETE | `/comments/{commentId}/like` | ❌ 없음 | 백엔드 미구현 |

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
