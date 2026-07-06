-- =====================================================================
-- 2026-07-06 인덱스 정비 마이그레이션 (PR #43)
-- 대상: 현재 코드(회원 식별자 BIGINT ID 스키마)와 일치하는 기존 DB.
-- 새로 DB를 만드는 경우에는 이 파일 대신 script/ddl.sql만 실행하면 된다.
-- =====================================================================

-- 0. 좋아요 중복 row 정리 — 유니크 인덱스 적용 전 반드시 선행
DELETE pl FROM COMMUNITY_POST_LIKES pl
    JOIN COMMUNITY_POST_LIKES dup
      ON pl.POST_ID = dup.POST_ID
     AND pl.MEMBER_ID = dup.MEMBER_ID
     AND pl.COMMUNITY_POST_LIKE_ID > dup.COMMUNITY_POST_LIKE_ID;

DELETE cl FROM COMMUNITY_COMMENT_LIKES cl
    JOIN COMMUNITY_COMMENT_LIKES dup
      ON cl.COMMENT_ID = dup.COMMENT_ID
     AND cl.MEMBER_ID = dup.MEMBER_ID
     AND cl.COMMUNITY_COMMENT_LIKE_ID > dup.COMMUNITY_COMMENT_LIKE_ID;

-- 1. 좋아요 복합 유니크
ALTER TABLE COMMUNITY_POST_LIKES
    ADD UNIQUE INDEX idx_post_like_post_id_member_id (POST_ID, MEMBER_ID);
ALTER TABLE COMMUNITY_COMMENT_LIKES
    ADD UNIQUE INDEX idx_comment_like_comment_id_member_id (COMMENT_ID, MEMBER_ID);

-- 2. 핫패스 일반 인덱스
ALTER TABLE MEMBERS
    ADD INDEX idx_member_phone_number (PHONE_NUMBER);
ALTER TABLE COMMUNITY_COMMENTS
    ADD INDEX idx_community_comment_post_id (POST_ID),
    ADD INDEX idx_community_comment_parent_id (PARENT_COMMENT_ID);
ALTER TABLE XROOMS
    ADD INDEX idx_xroom_owner_id (OWNER_ID),
    ADD INDEX idx_xroom_target_info_id (TARGET_INFO_ID);
ALTER TABLE MEMORIES
    ADD INDEX idx_memory_room_id (ROOM_ID);
ALTER TABLE MEMORY_EMOTION_TAGS
    ADD INDEX idx_emotion_tag_memory_id (MEMORY_ID);
ALTER TABLE MEMORY_MEDIA
    ADD INDEX idx_media_memory_id (MEMORY_ID);
ALTER TABLE POINT_HISTORIES
    ADD INDEX idx_point_history_owner_id (OWNER_ID);
ALTER TABLE MEMBER_PHOTOS
    ADD INDEX idx_member_photo_member_id (MEMBER_ID);

-- 3. FK 제거 (FK 금지 규칙) + 대체 인덱스
--    기존 DB에 FK가 있는 경우에만 해당. 제약 이름은 환경마다 다르므로 아래로 확인 후 DROP:
--    SELECT CONSTRAINT_NAME, TABLE_NAME FROM information_schema.TABLE_CONSTRAINTS
--     WHERE CONSTRAINT_TYPE = 'FOREIGN KEY' AND TABLE_NAME IN ('TARGET_INFOS', 'MATCHING_RESULTS');
--    ALTER TABLE TARGET_INFOS DROP FOREIGN KEY <제약명>;  (MATCHING_RESULTS 3건 동일)
ALTER TABLE TARGET_INFOS
    ADD INDEX idx_target_info_register_id (REGISTER_ID);
ALTER TABLE MATCHING_RESULTS
    ADD INDEX idx_matching_register_id (REGISTER_ID),
    ADD INDEX idx_matching_target_info_id (TARGET_INFO_ID),
    ADD INDEX idx_matching_target_id (TARGET_ID);
