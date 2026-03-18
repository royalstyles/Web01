CREATE SEQUENCE ECT_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE TABLE EMAIL_CHANGE_TOKENS (
                                     ID         NUMBER        PRIMARY KEY,
                                     TOKEN      VARCHAR2(255) NOT NULL UNIQUE,
                                     USER_ID    NUMBER        NOT NULL,
                                     NEW_EMAIL  VARCHAR2(100) NOT NULL,
                                     EXPIRES_AT TIMESTAMP     NOT NULL,
                                     CONSTRAINT FK_ECT_USER FOREIGN KEY (USER_ID) REFERENCES USERS(ID)
);

CREATE INDEX IDX_ECT_TOKEN ON EMAIL_CHANGE_TOKENS(TOKEN);