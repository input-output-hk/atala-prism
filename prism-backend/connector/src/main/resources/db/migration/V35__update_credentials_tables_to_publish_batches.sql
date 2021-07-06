-- ATA-3100
-- We want to issue credentials using Slayer v0.3. This implies the use of credential batches.
-- Before this story, we were storing in the published_credentials table:
-- + the encoded signed credential
-- + the management console credential id
-- + the hash of the operation that issued the credential (needed for revocation)
-- + the protocol-generated credential id for the credential
-- + the ledger transaction data associated to the issuance event

-- Now, credentials will be published in batches using a single transaction. This means that:
-- + each credential will have associated a proof of inclusion and a batch id
-- + the transaction data is associated to a batch (so it will be the same data for all credentials
--   in a same batch)
-- + the same as above occurs with the previous operation hash

-- Hence we will now have 2 tables:
-- + published_credentials as before, and also
-- + published_batches table

-- the first table will store:
-- + the encoded signed credential
-- + the console associated id
-- + the merkle proof of inclusion associated to the credential
-- + the batch id
-- + the shared_at timestamp

-- The later table will store
-- + the batch id
-- + the ledger transaction data
-- + the previous operation hash
-- + the stored_at timestamp

-- Given that we do not have any credential in the real world, we will simply clean the
-- published_credentials table
DELETE FROM published_credentials;

ALTER TABLE published_credentials
    DROP COLUMN node_credential_id,
    DROP COLUMN operation_hash,
    DROP COLUMN transaction_id,
    DROP COLUMN ledger,
    DROP COLUMN stored_at;

ALTER TABLE published_credentials
    ADD COLUMN batch_id TEXT NOT NULL,
    ADD COLUMN inclusion_proof_hash TEXT NOT NULL,
    ADD COLUMN inclusion_proof_index INTEGER NOT NULL,
    ADD COLUMN inclusion_proof_siblings TEXT ARRAY NOT NULL;

CREATE TABLE published_batches (
    batch_id TEXT NOT NULL,
    issued_on_transaction_id TRANSACTION_ID NOT NULL,
    ledger VARCHAR(32) NOT NULL,
    issuance_operation_hash TEXT NOT NULL,
    stored_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT published_batches_pk PRIMARY KEY (batch_id)
);

-- we add a foreign key reference and proper indexes
ALTER TABLE published_credentials
    ADD CONSTRAINT published_credentials_batch_id_fk FOREIGN KEY (batch_id) REFERENCES published_batches (batch_id);

CREATE INDEX published_credentials_batch_id_index ON published_credentials USING BTREE (batch_id);
-- this index was missing
CREATE INDEX published_credentials_credential_id_index ON published_credentials USING BTREE (credential_id);
