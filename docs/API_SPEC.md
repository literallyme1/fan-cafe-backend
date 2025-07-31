# FanCafe REST API 명세서
# example11

## 🔐 Auth - 인증/회원가입

### 1. 회원가입

* `POST /api/auth/register`

```json
Request:
{
  "email": "test@example.com",
  "password": "12345678",
  "nickname": "팬123"
}
Response: 201 Created
```

### 2. 로그인

* `POST /api/auth/login`

```json
Request:
{
  "email": "test@example.com",
  "password": "12345678"
}
Response:
{
  "accessToken": "JWT",
  "refreshToken": "JWT"
}
```

### 3. 내 정보 조회 (주소 포함)

* `GET /api/users/me`

> Header: Authorization: Bearer {token}

---

## 📝 Post - 게시글

### 4. 게시글 목록 조회 (틀림)

* `GET /api/posts?page=0&size=10&sort=created_at,desc`

```json
Response:
[
  {
    "id": 1,
    "title": "안녕하세요!",
    "content": "첫 글이에요.",
    "nickname": "팬123",
    "likeCount": 3,
    "commentCount": 5,
    "createdAt": "2024-06-17T10:00:00Z"
  }
]
```

### 5. 게시글 작성

* `POST /api/posts`

```json
Request:
{
  "title": "굿즈 후기",
  "content": "정말 좋았어요!",
  "imageUrls": ["https://..."]
}
Response: 201 Created
```

### 6. 게시글 상세 조회

* `GET /api/posts/{id}`

### 7. 게시글 수정

* `PUT /api/posts/{id}`

> 본인 작성자만 가능

### 8. 게시글 삭제

* `DELETE /api/posts/{id}`

> Soft Delete

---

## 💬 Comment - 댓글/대댓글

### 9. 댓글 등록

* `POST /api/posts/{postId}/comments`

```json
Request:
{
  "content": "좋아요!",
  "parentCommentId": null
}
```

### 10. 댓글 삭제

* `DELETE /api/comments/{id}`

> 본인 or 운영자만 가능

---

## ❤️ Like - 좋아요

### 11. 좋아요 토글

* `POST /api/posts/{postId}/likes`

> 좋아요 → 취소 반복

---

## 🛍 Merchandise & Promotion (프로모션 추가 상세 이미지)

### 12. 굿즈 목록 조회

* `GET /api/merchandise`

### 13. 프로모션 조회

* `GET /api/promotions`

---

## 📅 Schedule - 일정

### 14. 일정 조회 (월별)

* `GET /api/schedules?month=2024-06`

---

## 🛡 Admin (운영자)

### 15. 공지사항 작성

* `POST /api/posts`

> is\_notice = true, `role = ADMIN`만 가능

### 16. 일정 등록

* `POST /api/schedules`
