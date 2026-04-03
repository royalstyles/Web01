-- Spring Security Remember Me 기능을 위한 영속 로그인 토큰 테이블
-- JdbcTokenRepositoryImpl 이 사용하는 기본 스키마 (persistent_logins)
CREATE TABLE PERSISTENT_LOGINS (
    USERNAME    VARCHAR2(64)  NOT NULL,
    SERIES      VARCHAR2(64)  NOT NULL,
    TOKEN       VARCHAR2(64)  NOT NULL,
    LAST_USED   TIMESTAMP     NOT NULL,
    CONSTRAINT PK_PERSISTENT_LOGINS PRIMARY KEY (SERIES)
);
