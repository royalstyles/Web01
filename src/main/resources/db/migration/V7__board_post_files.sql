-- ============================================================
-- V7 : 게시글 첨부파일 (이미지 · 동영상)
-- ============================================================

CREATE SEQUENCE POST_FILES_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE TABLE POST_FILES (
                            ID              NUMBER          PRIMARY KEY,
                            POST_ID         NUMBER,                              -- NULL 허용: 임시 업로드 후 연결
                            UPLOADER_ID     NUMBER          NOT NULL,            -- 업로드한 사용자
                            ORIGINAL_NAME   VARCHAR2(500)   NOT NULL,            -- 원본 파일명
                            STORED_NAME     VARCHAR2(500)   NOT NULL UNIQUE,     -- UUID 기반 저장 파일명
                            FILE_PATH       VARCHAR2(1000)  NOT NULL,            -- 서버 내 절대 경로
                            FILE_URL        VARCHAR2(1000)  NOT NULL,            -- 브라우저 접근 URL
                            CONTENT_TYPE    VARCHAR2(100)   NOT NULL,            -- MIME 타입 (image/jpeg, video/mp4 등)
                            FILE_SIZE       NUMBER          NOT NULL,            -- 바이트 단위
                            FILE_TYPE       VARCHAR2(10)    NOT NULL,            -- 'IMAGE' 또는 'VIDEO'
                            CREATED_AT      TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
                            CONSTRAINT FK_FILES_POST     FOREIGN KEY (POST_ID)     REFERENCES POSTS(ID) ON DELETE CASCADE,
                            CONSTRAINT FK_FILES_UPLOADER FOREIGN KEY (UPLOADER_ID) REFERENCES USERS(ID),
                            CONSTRAINT CHK_FILE_TYPE     CHECK (FILE_TYPE IN ('IMAGE', 'VIDEO'))
);

CREATE INDEX IDX_POST_FILES_POST_ID ON POST_FILES(POST_ID);
CREATE INDEX IDX_POST_FILES_UPLOADER ON POST_FILES(UPLOADER_ID);

-- 고아 파일 정리용: POST_ID가 NULL이고 1시간 이상 된 임시 파일 식별에 활용
CREATE INDEX IDX_POST_FILES_CREATED ON POST_FILES(CREATED_AT);