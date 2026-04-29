-- 프로시저 실행
CALL insert_bulk_data();;

-- 실행 후 깔끔하게 프로시저 삭제 (선택사항)
DROP PROCEDURE IF EXISTS insert_bulk_data;;