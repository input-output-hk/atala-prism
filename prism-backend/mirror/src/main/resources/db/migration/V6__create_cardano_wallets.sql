CREATE TABLE cardano_wallets(
  id UUID NOT NULL PRIMARY KEY,
  name TEXT,
  connection_token TEXT NOT NULL,
  extended_public_key TEXT NOT NULL,
  last_generated_no INT NOT NULL,
  last_used_no INT,
  registration_date TIMESTAMPTZ NOT NULL,
  CONSTRAINT cardano_wallets_extended_public_key_unique UNIQUE (extended_public_key),
  CONSTRAINT cardano_wallets_connection_token_fk FOREIGN KEY (connection_token) REFERENCES connections (token)
);

CREATE INDEX cardano_wallets_connection_token_index ON cardano_wallets (connection_token);
