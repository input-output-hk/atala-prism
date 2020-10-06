
CREATE TABLE user_credentials(
  connection_token TEXT NOT NULL,
  raw_credential TEXT NOT NULL,
  issuers_did TEXT,
  message_id TEXT NOT NULL,
  message_received_date TIMESTAMPTZ NOT NULL,
  CONSTRAINT user_credentials_connection_token_fk FOREIGN KEY (connection_token) REFERENCES connections (token),
  CONSTRAINT user_credentials_message_id_unique UNIQUE (message_id)
);

CREATE INDEX user_credentials_connection_token_index ON user_credentials (connection_token);
CREATE INDEX user_credentials_message_received_date_index ON user_credentials (message_received_date);
