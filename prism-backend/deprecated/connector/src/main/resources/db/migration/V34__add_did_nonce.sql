-- An alternative version of request_nonces table that operates on DIDs instead of participant_ids
CREATE TABLE did_request_nonces (
    request_nonce BYTEA NOT NULL,
    did DID NOT NULL,
    CONSTRAINT did_request_nonces_pk PRIMARY KEY (request_nonce, did)
);
