-- ─────────────────────────────────────────────────────────────────
-- V17: 커스텀 역할 및 세부 기능 권한 테이블 추가
-- 관리자가 "일반/관리자" 외에 추가 역할을 생성하고
-- 각 역할에 세부 기능 권한(게시글 삭제, 공지 관리 등)을 부여할 수 있도록 지원
-- ─────────────────────────────────────────────────────────────────

-- 커스텀 역할 시퀀스
CREATE SEQUENCE CUSTOM_ROLES_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- 커스텀 역할 테이블
CREATE TABLE CUSTOM_ROLES (
    ID          NUMBER        PRIMARY KEY,
    NAME        VARCHAR2(50)  NOT NULL UNIQUE,       -- 역할명 (예: 모더레이터)
    DESCRIPTION VARCHAR2(200),                        -- 역할 설명 (선택)
    CREATED_AT  TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL
);

-- 역할별 세부 기능 권한 테이블 (ROLE_ID + PERMISSION 복합 PK)
-- PERMISSION 컬럼 값: POST_DELETE_OTHERS, COMMENT_DELETE_OTHERS,
--                     NOTICE_WRITE, NOTICE_DELETE, CATEGORY_MANAGE
CREATE TABLE CUSTOM_ROLE_PERMISSIONS (
    ROLE_ID     NUMBER       NOT NULL,
    PERMISSION  VARCHAR2(50) NOT NULL,
    CONSTRAINT PK_CRP        PRIMARY KEY (ROLE_ID, PERMISSION),
    CONSTRAINT FK_CRP_ROLE   FOREIGN KEY (ROLE_ID) REFERENCES CUSTOM_ROLES(ID) ON DELETE CASCADE
);

-- USERS 테이블에 커스텀 역할 FK 컬럼 추가 (nullable — 없으면 기본 ROLE_USER/ROLE_ADMIN만 적용)
ALTER TABLE USERS ADD CUSTOM_ROLE_ID NUMBER;
ALTER TABLE USERS ADD CONSTRAINT FK_USERS_CUSTOM_ROLE
    FOREIGN KEY (CUSTOM_ROLE_ID) REFERENCES CUSTOM_ROLES(ID) ON DELETE SET NULL;
