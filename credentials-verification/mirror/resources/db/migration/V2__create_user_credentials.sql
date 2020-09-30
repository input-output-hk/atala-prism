
CREATE TABLE user_credentials(
  connection_token TEXT NOT NULL,
  raw_credential TEXT NOT NULL,
  issuers_did TEXT NOT NULL,
  CONSTRAINT user_credentials_connection_token_fk FOREIGN KEY (connection_token) REFERENCES connections (token)
);

CREATE INDEX user_credentials_connection_token_index ON user_credentials (connection_token);
