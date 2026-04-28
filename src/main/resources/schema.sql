DROP PROCEDURE IF EXISTS insert_bulk_data;;

CREATE PROCEDURE insert_bulk_data()
BEGIN
    DECLARE i INT DEFAULT 1;

    -- (1) 유저 500명 생성 루프
    WHILE i <= 500 DO
        INSERT INTO users (
            email, password, nickname, role,
            avatar_url,
            introduction, -- nullable = false 필드
            password_updated_at_epoch_sec,
            password_set, -- boolean 필드 (1 = true)
            follower_count,
            following_count,
            created_at, updated_at
        ) VALUES (
            CONCAT('user', i, '@test.com'),
            'password123!',
            CONCAT('닉네임_', i),
            'USER',
            NULL,               -- avatar_url (nullable 이므로 NULL 가능)
            '',                 -- introduction (nullable = false 이므로 빈 문자열이라도 입력)
            UNIX_TIMESTAMP(NOW()),
            1,                  -- password_set = true
            FLOOR(RAND() * 100),
            FLOOR(RAND() * 100),
            NOW(), NOW()
        );
        SET i = i + 1;
END WHILE;

    -- (2) 게시글 10,000개 생성 루프
    SET i = 1;
    WHILE i <= 10000 DO
        INSERT INTO posts (
            user_id, title, content,
            view_count, like_count, comment_count,
            created_at, updated_at
        ) VALUES (
            FLOOR(1 + RAND() * 500),             -- 생성된 유저 500명 중 랜덤 선택
            CONCAT('테스트 게시글 제목 - ', i),
            CONCAT(i, '번째 게시글의 상세 내용입니다. k6 부하 테스트용 데이터입니다.'),
            FLOOR(RAND() * 50),                  -- viewCount 랜덤
            FLOOR(RAND() * 20),                  -- likeCount 랜덤 (테스트용)
            0,                                   -- commentCount (Redis 테스트를 위해 0으로 시작)
            NOW(), NOW()
        );
        SET i = i + 1;
END WHILE;
END;;