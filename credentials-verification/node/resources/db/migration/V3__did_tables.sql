CREATE TYPE KEY_USAGE AS ENUM('MASTER_KEY', 'ISSUING_KEY' ,'COMMUNICATION_KEY', 'AUTHENTICATION_KEY');

CREATE DOMAIN ID_TYPE AS TEXT CHECK(
  VALUE ~ '^[0-9a-f]{64}$'
);

CREATE TABLE did_data(
  did_suffix ID_TYPE NOT NULL,
  last_operation BYTEA NOT NULL,
  CONSTRAINT did_data_pk PRIMARY KEY (did_suffix)
);

CREATE TABLE public_keys(
  did_suffix ID_TYPE NOT NULL,
  key_id TEXT NOT NULL,
  key_usage KEY_USAGE NOT NULL,
  curve TEXT NOT NULL,
  x BYTEA NOT NULL,
  y BYTEA NOT NULL,
  CONSTRAINT public_keys_pk PRIMARY KEY (did_suffix, key_id)
);
