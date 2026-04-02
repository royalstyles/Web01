-- ============================================================
-- V11 : 게시글 외부 출처 URL 컬럼 추가
-- 외부 사이트에서 자동 수집된 게시글의 원문 URL 저장
-- 중복 수집 방지용 유니크 인덱스 적용 (Oracle: NULL 값은 유니크 인덱스 제외)
-- ============================================================

ALTER TABLE POSTS ADD SOURCE_URL VARCHAR2(1000);

CREATE UNIQUE INDEX IDX_POSTS_SOURCE_URL ON POSTS(SOURCE_URL);
