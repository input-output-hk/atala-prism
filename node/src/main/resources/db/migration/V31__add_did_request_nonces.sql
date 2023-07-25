CREATE DOMAIN DID AS TEXT CHECK(
  VALUE ~ '^did:[a-z0-9]+:[a-zA-Z0-9._-]*(:[a-zA-Z0-9._-]*)*$'
);

CREATE TABLE did_request_nonces (
    request_nonce BYTEA NOT NULL,
    did DID NOT NULL,
    CONSTRAINT did_request_nonces_pk PRIMARY KEY (request_nonce, did)
);
