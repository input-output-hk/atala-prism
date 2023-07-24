CREATE DOMAIN CONTENT_HASH AS BYTEA
CHECK (
  LENGTH(VALUE) = 32
);

CREATE TABLE credentials(
  credential_id ID_TYPE NOT NULL,
  last_operation OPERATION_HASH NOT NULL,
  issuer ID_TYPE NOT NULL,
  content_hash CONTENT_HASH NOT NULL,
  issued_on TIMESTAMPTZ NOT NULL,
  issued_on_absn INTEGER NOT NULL,
  --^ Atala Block Sequence Number (absn) of the operation that issued the credential
  issued_on_osn INTEGER NOT NULL,
  --^ Operation Sequence Number (osn) of the operation that issued the credential
  revoked_on TIMESTAMPTZ NULL DEFAULT NULL,
  revoked_on_absn INTEGER NULL DEFAULT NULL,
  --^ Atala Block Sequence Number (absn) of the operation that revoked the credential
  revoked_on_osn INTEGER NULL DEFAULT NULL,
  --^ Operation Sequence Number (osn) of the operation that revoked the credential

  CONSTRAINT credentials_pk PRIMARY KEY (credential_id),
  CONSTRAINT credentials_issuer_fk FOREIGN KEY (issuer) REFERENCES did_data (did_suffix)
  -- Add constraint so that revoked_on and revoke_on_sequence_number match in terms on not being null
);

CREATE INDEX credentials_issuer_index ON credentials USING BTREE (issuer);
