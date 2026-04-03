-- ============================================================
-- V15 : 공지에 게시판(카테고리) 지정 컬럼 추가
--       NULL = 전체 게시판 / 값 있음 = 해당 카테고리 전용
-- ============================================================

ALTER TABLE NOTICES ADD CATEGORY_ID NUMBER;

ALTER TABLE NOTICES ADD CONSTRAINT FK_NOTICE_CATEGORY
    FOREIGN KEY (CATEGORY_ID) REFERENCES CATEGORIES(ID) ON DELETE SET NULL;

CREATE INDEX IDX_NOTICES_CATEGORY ON NOTICES(CATEGORY_ID);
