CREATE TABLE store_users (
  user_id UUID NOT NULL,

  CONSTRAINT store_users_pk PRIMARY KEY (user_id)
);

CREATE TYPE INDIVIDUAL_CONNECTION_STATUS_TYPE AS ENUM(
  'CREATED', -- the party data is stored but he hasn't been invited to connect on the app
  'INVITED', -- the party has been invited to use the app but he hasn't accepted yet
  'CONNECTED', -- the party has accepted the connection
  'REVOKED' -- the party revoked the connection
);

CREATE TABLE store_individuals (
  user_id UUID NOT NULL,
  individual_id UUID NOT NULL,
  status INDIVIDUAL_CONNECTION_STATUS_TYPE NOT NULL DEFAULT 'CREATED',
  connection_token TEXT NULL DEFAULT NULL,
  connection_id UUID NULL DEFAULT NULL,
  full_name TEXT NOT NULL,
  email TEXT NULL,
  created_at TIMESTAMPTZ NOT NULL,

  CONSTRAINT store_individuals_pk PRIMARY KEY (individual_id),
  CONSTRAINT store_individuals_token_per_user_unique UNIQUE (user_id, connection_token),
  CONSTRAINT store_individuals_id_per_user_unique UNIQUE (user_id, connection_id),
  CONSTRAINT store_individuals_user FOREIGN KEY (user_id) REFERENCES store_users (user_id)
);

CREATE INDEX store_individuals_connection_token_index ON store_individuals USING BTREE (connection_token, individual_id);
CREATE INDEX store_individuals_user_id_connection_token_index ON store_individuals USING BTREE (user_id, connection_token, individual_id);
CREATE INDEX store_individuals_user_id_connection_id_index ON store_individuals USING BTREE (user_id, connection_id, individual_id);
CREATE INDEX store_individuals_user_id_created_at_index ON store_individuals USING BTREE (user_id, created_at, individual_id);

CREATE TABLE stored_credentials (
  individual_id UUID NOT NULL,
  issuer_did TEXT NOT NULL,
  proof_id TEXT NOT NULL, -- credential id on ledger
  content BYTEA NOT NULL,
  signature BYTEA NOT NULL,
  stored_at TIMESTAMPTZ NOT NULL,

  CONSTRAINT signed_credentials_pk PRIMARY KEY (individual_id, proof_id),
  CONSTRAINT signed_credentials_individual_id FOREIGN KEY (individual_id) REFERENCES store_individuals (individual_id)
);

CREATE INDEX stored_credentials_individual_id_index ON stored_credentials USING BTREE (individual_id, stored_at);

INSERT INTO store_users (user_id) VALUES ('f424f42c-2097-4b66-932d-b5e53c734eff');
INSERT INTO store_individuals (user_id, individual_id, full_name, created_at) VALUES ('f424f42c-2097-4b66-932d-b5e53c734eff', '249a6818-bd8d-4ad2-b73b-ea80ae4231c3', 'Holder One', now());
