CREATE TABLE contacts (
    contact_id UUID NOT NULL,
    CONSTRAINT contacts_contact_id_pk PRIMARY KEY (contact_id)
);

-- https://w3c.github.io/did-core/#did-syntax
CREATE DOMAIN DID AS TEXT CHECK(
    VALUE ~ '^did:[a-z0-9]+:[a-zA-Z0-9._-]*(:[a-zA-Z0-9._-]*)*$'
);

CREATE DOMAIN TRANSACTION_ID AS BYTEA
CHECK (
    LENGTH(VALUE) = 32
);

CREATE TABLE participants(
    participant_id UUID NOT NULL,
    name TEXT NOT NULL,
    did DID NOT NULL,
    logo BYTEA NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT participants_participant_id_pk PRIMARY KEY (participant_id),
    CONSTRAINT participants_did_unique UNIQUE (did)
);

-- As nonces are supposed to be random, it is likely that they will be unique
-- so, using the nonce as the first argument for the index speeds up the queries.
CREATE TABLE request_nonces (
    request_nonce BYTEA NOT NULL,
    participant_id UUID NOT NULL,
    CONSTRAINT request_nonces_pk PRIMARY KEY (request_nonce, participant_id),
    CONSTRAINT request_nonces_participant_id_fk FOREIGN KEY (participant_id) REFERENCES participants (participant_id)
);
