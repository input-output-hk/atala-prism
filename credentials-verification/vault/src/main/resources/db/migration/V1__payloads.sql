-- https://w3c-ccg.github.io/did-spec/#generic-did-syntax
CREATE DOMAIN DID AS TEXT CHECK(
  VALUE ~ '^did:[a-z0-9]+:[a-zA-Z0-9._-]*(:[a-zA-Z0-9._-]*)*$'
);

CREATE TABLE payloads (
  payload_id UUID,
  did DID NOT NULL,
  content BYTEA NOT NULL,
  creation_order BIGSERIAL,
  created_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT payloads_payload_id_pk PRIMARY KEY (payload_id)
);

CREATE INDEX payloads_did_index ON payloads USING BTREE (did);
CREATE INDEX payloads_creation_order_index ON payloads USING BTREE (creation_order);
CREATE INDEX payloads_created_at_index ON payloads USING BTREE (created_at);

-- As nonces are supposed to be random, it is likely that they will be unique
-- so, using the nonce as the first argument for the index speeds up the queries.
CREATE TABLE request_nonces (
  request_nonce BYTEA NOT NULL,
  did DID NOT NULL,
  CONSTRAINT request_nonces_pk PRIMARY KEY (request_nonce, did)
);
