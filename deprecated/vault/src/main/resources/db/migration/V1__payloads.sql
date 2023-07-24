-- https://w3c-ccg.github.io/did-spec/#generic-did-syntax
CREATE DOMAIN DID AS TEXT CHECK(
  VALUE ~ '^did:[a-z0-9]+:[a-zA-Z0-9._-]*(:[a-zA-Z0-9._-]*)*$'
);

CREATE TABLE payloads (
  payload_id UUID,
  -- Client-generated identifier provided by the author, which is used to avoid creating two
  -- semantically equivalent payloads. As a result, the pair (did, external_id) should be unique.
  external_id UUID,
  -- Hash from the unencrypted content supplied by the author
  hash TEXT NOT NULL,
  -- DID representing the author of this payload
  did DID NOT NULL,
  -- Data encrypted by the author
  content BYTEA NOT NULL,
  -- Serial number representing the ordered linear creation of the payloads
  creation_order BIGSERIAL,
  -- Timestamp of when the payload was registered by the vault
  created_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT payloads_payload_id_pk PRIMARY KEY (payload_id),
  CONSTRAINT payloads_external_id_unique_per_did UNIQUE (external_id, did)
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
