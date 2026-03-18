CREATE SEQUENCE USERS_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE TABLE USERS (
                       ID             NUMBER        PRIMARY KEY,
                       USERNAME       VARCHAR2(50)  NOT NULL UNIQUE,
                       PASSWORD       VARCHAR2(255) NOT NULL,
                       EMAIL          VARCHAR2(100) NOT NULL UNIQUE,
                       ROLE           VARCHAR2(20)  NOT NULL,
                       EMAIL_VERIFIED NUMBER(1,0)   DEFAULT 0 NOT NULL,
                       CREATED_AT     TIMESTAMP     DEFAULT SYSTIMESTAMP
);