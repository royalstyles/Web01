CREATE SEQUENCE EVT_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE TABLE EMAIL_VERIFICATION_TOKENS (
                                           ID         NUMBER        PRIMARY KEY,
                                           TOKEN      VARCHAR2(255) NOT NULL UNIQUE,
                                           USER_ID    NUMBER        NOT NULL,
                                           EXPIRES_AT TIMESTAMP     NOT NULL,
                                           CONSTRAINT FK_EVT_USER FOREIGN KEY (USER_ID) REFERENCES USERS(ID)
);