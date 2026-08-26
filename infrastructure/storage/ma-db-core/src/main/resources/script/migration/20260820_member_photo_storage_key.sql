-- =====================================================================
-- 2026-08-20 프로필 사진 저장 규약 전환 + 닉네임 유니크 (REQ-016)
-- 대상: 현재 코드와 일치하는 기존 DB.
-- 새로 DB를 만드는 경우에는 이 파일 대신 script/ddl.sql만 실행하면 된다.
--
-- ⚠ 3번은 되돌리기 어렵다. 실행 전에 반드시 백업하고, 1번 확인 결과를 먼저 볼 것.
-- 순서대로 실행한다 (1 → 2 → 3 → 4).
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. 닉네임 중복 사전 확인 — 4번 유니크 인덱스 적용 전 반드시 선행
--    아래 쿼리가 0건이어야 4번이 성공한다.
--    중복이 나오면 어느 회원의 닉네임을 어떻게 바꿀지가 운영 판단 사항이므로,
--    이 스크립트에는 정리 문을 넣지 않는다. 결과를 보고 정리 방식을 먼저 정할 것.
--
--    ⚠ DELETED 로 거르지 않는다. 앱 레벨 existsByNickname 은 활성 회원만 보지만
--    DB 유니크 인덱스는 소프트 삭제된 행까지 포함해 전체 행에 걸린다.
--    탈퇴 완료 배치가 익명화한 행은 '탈퇴한회원_<id>' 라 서로 부딪히지 않지만,
--    익명화를 거치지 않고 삭제된 행이 있으면 활성 회원과 충돌할 수 있다.
-- ---------------------------------------------------------------------
SELECT NICKNAME, COUNT(*) AS CNT
  FROM MEMBERS
 GROUP BY NICKNAME
HAVING COUNT(*) > 1;

-- ---------------------------------------------------------------------
-- 2. MEMBER_PHOTOS 컬럼 리네임 — 값의 의미가 "서버 파일 경로"에서
--    "상대 storageKey"로 바뀌므로 이름도 신형(COMMUNITY_POST_IMAGES)에 맞춘다.
--    FILE_PATH      -> STORAGE_KEY
--    THUMBNAIL_PATH -> THUMBNAIL_KEY
-- ---------------------------------------------------------------------
ALTER TABLE MEMBER_PHOTOS
    CHANGE COLUMN FILE_PATH STORAGE_KEY VARCHAR(512) NOT NULL,
    CHANGE COLUMN THUMBNAIL_PATH THUMBNAIL_KEY VARCHAR(512) NULL;

-- ---------------------------------------------------------------------
-- 3. ⚠ 되돌리기 어려움 — 기존 값에서 basePath 접두사를 제거한다.
--    저장값 예: 'uploads/member/thumbnail/1/thumb_x.jpg'
--          ->  'member/thumbnail/1/thumb_x.jpg'
--
--    운영 basePath 가 'uploads' 가 아니면 아래 두 리터럴만 바꾼다.
--      - LIKE 'uploads/%'  : 접두사 + '/'
--      - SUBSTRING(.., 9)  : 접두사 길이 + 2  ('uploads' 7자 + '/' 1자 = 8자를 벗기므로 9부터)
--    접두사가 붙지 않은 행은 WHERE 로 걸러져 두 번 벗겨지지 않는다(재실행 안전).
-- ---------------------------------------------------------------------
UPDATE MEMBER_PHOTOS
   SET STORAGE_KEY = SUBSTRING(STORAGE_KEY, 9)
 WHERE STORAGE_KEY LIKE 'uploads/%';

UPDATE MEMBER_PHOTOS
   SET THUMBNAIL_KEY = SUBSTRING(THUMBNAIL_KEY, 9)
 WHERE THUMBNAIL_KEY LIKE 'uploads/%';

-- ---------------------------------------------------------------------
-- 4. MEMBERS.NICKNAME 유니크 인덱스 — 1번 확인이 0건일 때만 실행한다.
--    앱 레벨 existsByNickname 검사만으로는 동시 요청에 뚫리므로 DB 백스톱을 건다.
-- ---------------------------------------------------------------------
ALTER TABLE MEMBERS
    ADD UNIQUE INDEX idx_member_nickname (NICKNAME);
