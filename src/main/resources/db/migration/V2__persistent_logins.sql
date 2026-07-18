-- Migration V2: Create persistent_logins table for Spring Security Remember-Me support.
-- Used by JdbcTokenRepositoryImpl to store persistent remember-me tokens.
-- username stores the Spring Security principal name (user email, per PR3 migration).
CREATE TABLE IF NOT EXISTS persistent_logins (
    username   VARCHAR(64)  NOT NULL,
    series     VARCHAR(64)  PRIMARY KEY,
    token      VARCHAR(64)  NOT NULL,
    last_used  TIMESTAMP    NOT NULL
);
