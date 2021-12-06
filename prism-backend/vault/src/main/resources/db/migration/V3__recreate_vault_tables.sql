DROP TABLE payloads;
DROP TABLE request_nonces;

CREATE TABLE records (
  record_type BYTEA NOT NULL,
  record_id BYTEA NOT NULL,
  payload BYTEA NOT NULL,
    -- Timestamp of when the payload was registered by the vault
  created_at TIMESTAMPTZ NOT NULL,

  CONSTRAINT records_record_id_pk PRIMARY KEY (record_id)
);

CREATE INDEX records_type_index ON records USING BTREE (record_type);
CREATE INDEX records_created_at_index ON records USING BTREE (created_at);
