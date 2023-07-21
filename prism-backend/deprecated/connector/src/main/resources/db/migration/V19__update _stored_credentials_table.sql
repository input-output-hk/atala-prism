DROP INDEX stored_credentials_individual_id_index;

ALTER TABLE stored_credentials
   DROP CONSTRAINT signed_credentials_pk,
   DROP CONSTRAINT signed_credentials_individual_id,
   DROP COLUMN issuer_did,
   DROP COLUMN proof_id,
   DROP COLUMN content,
   DROP COLUMN signature,
   ADD COLUMN storage_id UUID NOT NULL,
   ADD COLUMN encoded_signed_credential TEXT NOT NULL;

ALTER TABLE stored_credentials
   RENAME COLUMN individual_id TO connection_id;

ALTER TABLE verifier_holders
  DROP CONSTRAINT verifier_holders_token_per_user_unique,
  DROP CONSTRAINT verifier_holders_id_per_user_unique,
  ADD CONSTRAINT verifier_holders_connection_token_unique UNIQUE (connection_token),
  ADD CONSTRAINT verifier_holders_connection_id_unique UNIQUE (connection_id);

ALTER TABLE stored_credentials
   ADD CONSTRAINT stored_credentials_pk PRIMARY KEY (storage_id),
   ADD CONSTRAINT stored_credentials_connection_id_fk FOREIGN KEY (connection_id) REFERENCES verifier_holders(connection_id);

ALTER TABLE published_credentials
   ADD COLUMN stored_at TIMESTAMPTZ NOT NULL;

CREATE INDEX stored_credentials_connection_id_index ON stored_credentials USING BTREE (connection_id);
