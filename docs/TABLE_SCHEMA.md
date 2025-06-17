# ERD 실무 체크리스트

> ERD 설계 시 반드시 고려해야 할 제약 조건 및 실무 팁들을 테이블별로 정리한 문서입니다. 개발 초기에 이 문서를 기준으로 DB 구조를 점검합니다.

## ✅ ERD 사용 체크리스트

| 테이블명          | 실무 체크포인트                                                                                                                                                       |
| ------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `users`       | - email, nickname은 UNIQUE 제약 필수<br>- password는 반드시 해시 처리 (예: bcrypt)<br>- 탈퇴 시 soft delete 처리 권장 (`deleted_at`, `is_deleted`)                                  |
| `posts`       | - 좋아요 수, 댓글 수, 조회수는 캐싱 필드(counter cache) 활용<br>- `is_notice` 대신 `type` 필드로 공지글 등 분기<br>- 이미지 여러 장 첨부 시 `post_images` 테이블로 분리                                   |
| `comments`    | - `parent_comment_id`로 대댓글 구조 구현<br>- 삭제된 댓글은 "삭제된 댓글입니다" UI 처리 필요 → `is_deleted` 사용<br>- 수정, 삭제를 위한 `updated_at`, `deleted_at` 필드 권장                          |
| `likes`       | - `user_id + post_id` 조합에 UNIQUE 제약 필수<br>- 좋아요 누르면 INSERT, 취소는 DELETE 또는 `is_deleted = true`<br>- 좋아요 수는 `posts.like_count`로 별도 캐싱                            |
| `schedules`   | - `date`보단 `start_at`, `end_at`으로 정확한 일정 관리<br>- 상태 구분을 위한 `status` 필드 (`UPCOMING`, `DONE` 등)<br>- `location`, `is_published` 필드도 고려 가능                        |
| `promotions`  | - 사용자 데이터는 아니지만, 운영 데이터로 `created_at`, `updated_at` 필요<br>- 자동 노출 제어를 위한 `start_at`, `end_at`, `is_active` 권장<br>- 정렬 순서를 위한 `priority`, `type`, `link_url` 고려 |
| `merchandise` | - `stock`, `status` (`ON_SALE`, `SOLD_OUT`) 필수<br>- 가격은 `decimal`로 표현 (소수점 가능)<br>- `category`, `sale_price`, `is_active` 필드 추가 고려                             |
