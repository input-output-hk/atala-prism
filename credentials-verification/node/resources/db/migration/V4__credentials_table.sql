CREATE DOMAIN CONTENT_HASH AS BYTEA
CHECK (
  LENGTH(VALUE) = 32
);

CREATE TABLE credentials(
  credential_id ID_TYPE NOT NULL,
  last_operation OPERATION_HASH NOT NULL,
  issuer ID_TYPE NOT NULL,
  content_hash CONTENT_HASH NOT NULL,
  issued_on DATE NOT NULL,
  revoked_on DATE NULL DEFAULT NULL,

  CONSTRAINT credentials_pk PRIMARY KEY (credential_id),
  CONSTRAINT credentials_issuer_fk FOREIGN KEY (issuer) REFERENCES did_data (did_suffix)
);

CREATE INDEX credentials_issuer_index ON credentials USING BTREE (issuer);
