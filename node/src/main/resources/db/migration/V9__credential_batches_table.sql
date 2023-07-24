CREATE DOMAIN MERKLE_ROOT AS BYTEA
CHECK (
  LENGTH(VALUE) = 32
);

CREATE TABLE credential_batches(
  batch_id ID_TYPE NOT NULL,
  last_operation OPERATION_HASH NOT NULL,
  issuer_did_suffix ID_TYPE NOT NULL,
  merkle_root MERKLE_ROOT NOT NULL,
  issued_on TIMESTAMPTZ NOT NULL,
  -- Atala Block Sequence Number (absn) of the operation that issued the batch
  issued_on_absn INTEGER NOT NULL,
  -- Operation Sequence Number (osn) of the operation that issued the batch
  issued_on_osn INTEGER NOT NULL,
  revoked_on TIMESTAMPTZ NULL DEFAULT NULL,
  -- Atala Block Sequence Number (absn) of the operation that revoked the batch
  revoked_on_absn INTEGER NULL DEFAULT NULL,
  -- Operation Sequence Number (osn) of the operation that revoked the batch
  revoked_on_osn INTEGER NULL DEFAULT NULL,

  CONSTRAINT credential_batches_pk PRIMARY KEY (batch_id),
  CONSTRAINT credential_batches_issuer_did_suffix_fk FOREIGN KEY (issuer_did_suffix) REFERENCES did_data (did_suffix),
  CONSTRAINT revoke_on_check CHECK (
     (revoked_on IS NULL AND revoked_on_absn IS NULL AND revoked_on_osn IS NULL)
     OR
     (revoked_on IS NOT NULL AND revoked_on_absn IS NOT NULL AND revoked_on_osn IS NOT NULL)
  )
);

CREATE INDEX credential_batches_issuer_did_suffix_index ON credential_batches USING BTREE (issuer_did_suffix);
