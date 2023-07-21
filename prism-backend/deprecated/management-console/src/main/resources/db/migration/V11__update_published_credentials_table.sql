-- ATA-4503
DELETE FROM published_credentials;

ALTER TABLE published_credentials
    DROP COLUMN node_credential_id,
    DROP COLUMN operation_hash,
    DROP COLUMN transaction_id,
    DROP COLUMN ledger,
    DROP COLUMN stored_at;

ALTER TABLE published_credentials
    ADD COLUMN batch_id TEXT NOT NULL,
    ADD COLUMN inclusion_proof TEXT NOT NULL;

-- we add a foreign key reference and proper indexes
ALTER TABLE published_credentials
    ADD CONSTRAINT published_credentials_batch_id_fk FOREIGN KEY (batch_id) REFERENCES published_batches (batch_id);

CREATE INDEX published_credentials_batch_id_index ON published_credentials USING BTREE (batch_id);