
-- As nonces are supposed to be random, it is likely that they will be unique
-- so, using the nonce as the first argument for the index speeds up the queries.
CREATE TABLE request_nonces (
  request_nonce BYTEA NOT NULL,
  participant_id UUID NOT NULL,
  CONSTRAINT request_nonces_pk PRIMARY KEY (request_nonce, participant_id),
  CONSTRAINT request_nonces_participant_id_fk FOREIGN KEY (participant_id) REFERENCES participants (id)
);

CREATE INDEX request_nonces_participant_id_index ON request_nonces USING BTREE (participant_id);
