-- ============================================================
-- V13 : 알림 (좋아요 / 댓글)
-- ============================================================

CREATE SEQUENCE NOTIFICATIONS_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE TABLE NOTIFICATIONS (
    ID           NUMBER        DEFAULT NOTIFICATIONS_SEQ.NEXTVAL PRIMARY KEY,
    RECIPIENT_ID NUMBER        NOT NULL,                              -- 알림 받는 사람 (글 작성자)
    ACTOR_ID     NUMBER,                                              -- 알림 발생자 (탈퇴 시 NULL)
    POST_ID      NUMBER,                                              -- 관련 게시글 (삭제 시 NULL)
    TYPE         VARCHAR2(20)  NOT NULL,                             -- 'LIKE' | 'COMMENT'
    MESSAGE      VARCHAR2(500) NOT NULL,                             -- 표시 메시지
    IS_READ      NUMBER(1,0)   DEFAULT 0 NOT NULL,                   -- 읽음 여부
    CREATED_AT   TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT FK_NOTIF_RECIPIENT FOREIGN KEY (RECIPIENT_ID) REFERENCES USERS(ID)  ON DELETE CASCADE,
    CONSTRAINT FK_NOTIF_ACTOR     FOREIGN KEY (ACTOR_ID)     REFERENCES USERS(ID)  ON DELETE SET NULL,
    CONSTRAINT FK_NOTIF_POST      FOREIGN KEY (POST_ID)      REFERENCES POSTS(ID)  ON DELETE CASCADE
);

-- 수신자별 최신순 조회 인덱스
CREATE INDEX IDX_NOTIF_RECIPIENT_DATE ON NOTIFICATIONS(RECIPIENT_ID, CREATED_AT DESC);
