CREATE TYPE KEY_USAGE AS ENUM('MASTER_KEY', 'ISSUING_KEY' ,'COMMUNICATION_KEY', 'AUTHENTICATION_KEY');

CREATE DOMAIN ID_TYPE AS TEXT CHECK(
  VALUE ~ '^[0-9a-f]{64}$'
);

CREATE DOMAIN OPERATION_HASH AS BYTEA
CHECK (
  LENGTH(VALUE) = 32
);

CREATE TABLE did_data(
  did_suffix ID_TYPE NOT NULL,
  last_operation OPERATION_HASH NOT NULL,
  CONSTRAINT did_data_pk PRIMARY KEY (did_suffix)
);

CREATE TABLE public_keys(
  did_suffix ID_TYPE NOT NULL,
  key_id TEXT NOT NULL,
  key_usage KEY_USAGE NOT NULL,
  curve TEXT NOT NULL,
  x BYTEA NOT NULL,
  y BYTEA NOT NULL,
  added_on TIMESTAMPTZ NOT NULL,
  added_on_absn INTEGER NOT NULL,
  --^ Atala Block Sequence Number (absn) of the operation that added the key
  added_on_osn INTEGER NOT NULL,
  --^ Operation Sequence Number (osn) of the operation that added the key

  revoked_on TIMESTAMPTZ NULL DEFAULT NULL,
  revoked_on_absn INTEGER NULL DEFAULT NULL,
  --^ Atala Block Sequence Number (absn) of the operation that revoked the key
  revoked_on_osn INTEGER NULL DEFAULT NULL,
  --^ Operation Sequence Number (osn) of the operation that revoked the key

  CONSTRAINT public_keys_pk PRIMARY KEY (did_suffix, key_id)
  -- add constraints about congruent addition of sequence_numbers
);
