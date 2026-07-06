-- =====================================================================
-- V2 인덱스 정비 (원본: script/migration/20260706_add_indexes.sql, PR #43)
-- 멱등(IF NOT EXISTS): 신규 DB(V1 에서 인덱스 생성 완료)에선 no-op,
--                      인덱스 없는 기존 DB(baseline 후 진입)에선 실제 추가.
-- MariaDB 10.0.2+ 의 ADD INDEX ... IF NOT EXISTS 문법 사용.
-- FK 미사용 규칙: PK·INDEX 만 다룬다.
-- =====================================================================

-- 0. 좋아요 중복 row 정리 — 유니크 인덱스 적용 전 선행 (빈 테이블에선 무해)
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
    ADD UNIQUE INDEX IF NOT EXISTS idx_post_like_post_id_member_id (POST_ID, MEMBER_ID);
ALTER TABLE COMMUNITY_COMMENT_LIKES
    ADD UNIQUE INDEX IF NOT EXISTS idx_comment_like_comment_id_member_id (COMMENT_ID, MEMBER_ID);

-- 2. 핫패스 일반 인덱스
ALTER TABLE MEMBERS
    ADD INDEX IF NOT EXISTS idx_member_phone_number (PHONE_NUMBER);
ALTER TABLE COMMUNITY_COMMENTS
    ADD INDEX IF NOT EXISTS idx_community_comment_post_id (POST_ID),
    ADD INDEX IF NOT EXISTS idx_community_comment_parent_id (PARENT_COMMENT_ID);
ALTER TABLE XROOMS
    ADD INDEX IF NOT EXISTS idx_xroom_owner_id (OWNER_ID),
    ADD INDEX IF NOT EXISTS idx_xroom_target_info_id (TARGET_INFO_ID);
ALTER TABLE MEMORIES
    ADD INDEX IF NOT EXISTS idx_memory_room_id (ROOM_ID);
ALTER TABLE MEMORY_EMOTION_TAGS
    ADD INDEX IF NOT EXISTS idx_emotion_tag_memory_id (MEMORY_ID);
ALTER TABLE MEMORY_MEDIA
    ADD INDEX IF NOT EXISTS idx_media_memory_id (MEMORY_ID);
ALTER TABLE POINT_HISTORIES
    ADD INDEX IF NOT EXISTS idx_point_history_owner_id (OWNER_ID);
ALTER TABLE MEMBER_PHOTOS
    ADD INDEX IF NOT EXISTS idx_member_photo_member_id (MEMBER_ID);

-- 3. 매칭 관련 인덱스
ALTER TABLE TARGET_INFOS
    ADD INDEX IF NOT EXISTS idx_target_info_register_id (REGISTER_ID);
ALTER TABLE MATCHING_RESULTS
    ADD INDEX IF NOT EXISTS idx_matching_register_id (REGISTER_ID),
    ADD INDEX IF NOT EXISTS idx_matching_target_info_id (TARGET_INFO_ID),
    ADD INDEX IF NOT EXISTS idx_matching_target_id (TARGET_ID);

-- 참고 (자동화에서 제외): 일부 레거시 DB 에는 TARGET_INFOS/MATCHING_RESULTS 에 FK 가 남아 있을 수 있다.
--   FK 제약명은 환경마다 달라 결정적 실행이 불가하므로 Flyway 마이그레이션에 포함하지 않는다.
--   해당 레거시 DB 에 한해 아래로 확인 후 수동 제거한다:
--     SELECT CONSTRAINT_NAME, TABLE_NAME FROM information_schema.TABLE_CONSTRAINTS
--      WHERE CONSTRAINT_TYPE = 'FOREIGN KEY' AND TABLE_NAME IN ('TARGET_INFOS', 'MATCHING_RESULTS');
--     ALTER TABLE TARGET_INFOS DROP FOREIGN KEY <제약명>;   (MATCHING_RESULTS 3건 동일)
