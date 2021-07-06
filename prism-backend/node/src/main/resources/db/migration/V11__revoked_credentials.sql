CREATE DOMAIN CREDENTIAL_HASH AS BYTEA
CHECK (
  LENGTH(VALUE) = 32
);

CREATE TABLE revoked_credentials(
  batch_id ID_TYPE NOT NULL,
  credential_id CREDENTIAL_HASH NOT NULL,
  revoked_on TIMESTAMPTZ NOT NULL,
  -- Atala Block Sequence Number (absn) of the operation that revoked the batch
  revoked_on_absn INTEGER NOT NULL,
  -- Operation Sequence Number (osn) of the operation that revoked the batch
  revoked_on_osn INTEGER NOT NULL,
  CONSTRAINT revoked_credentials_pk PRIMARY KEY (batch_id, credential_id),
  CONSTRAINT revoked_credentials_batch_id_fk FOREIGN KEY (batch_id) REFERENCES credential_batches (batch_id)
);