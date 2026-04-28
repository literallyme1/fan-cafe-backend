-- 대량 데이터 삽입 프로시저
DELIMITER $$

CREATE PROCEDURE insert_bulk_data()
BEGIN
    DECLARE i INT DEFAULT 1;

    -- (1) 유저 500명 생성 루프
    WHILE i <= 500 DO
        INSERT INTO users (
            email, password, nickname, role,
            password_updated_at_epoch_sec, password_set,
            follower_count, created_at, updated_at
        ) VALUES (
            CONCAT('user', i, '@test.com'),      -- 이메일: user1@test.com 형태
            'password123!',                       -- 비밀번호 (고정)
            CONCAT('닉네임_', i),                 -- 닉네임_1 형태
            'USER',                              -- Role: 일반 유저 고정
            UNIX_TIMESTAMP(NOW()),               -- 현재 시간을 초 단위 Long으로 변환
            1,                                   -- passwordSet: true(1)
            FLOOR(RAND() * 100),                 -- 팔로워 수: 0~100 사이 랜덤
            NOW(), NOW()                         -- 생성/수정 시간 수동 입력
        );
        SET i = i + 1;
END WHILE;

    -- (2) 게시글 10,000개 생성 루프
    SET i = 1;
    WHILE i <= 10000 DO
        INSERT INTO post (
            user_id, title, content,
            view_count, like_count, comment_count,
            created_at, updated_at
        ) VALUES (
            FLOOR(1 + RAND() * 500),             -- 1~500번 유저 중 한 명을 작성자로 랜덤 선택
            CONCAT('테스트 게시글 제목 - ', i),
            CONCAT(i, '번째 게시글의 상세 내용입니다. 이 데이터는 k6 부하 테스트용입니다.'),
            FLOOR(RAND() * 50),                  -- 조회수: 0~50 사이 랜덤
            0, 0,                                -- 좋아요/댓글은 0으로 시작
            NOW(), NOW()                         -- 생성/수정 시간 수동 입력
        );
        SET i = i + 1;
END WHILE;
END$$

DELIMITER ;

-- [5] 프로시저 실행 및 삭제
CALL insert_bulk_data();
DROP PROCEDURE IF EXISTS insert_bulk_data;