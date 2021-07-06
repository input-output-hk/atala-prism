CREATE DOMAIN TRANSACTION_ID AS BYTEA
CHECK (
  LENGTH(VALUE) = 32
);

-- Add optional columns to the tables
ALTER TABLE published_credentials
    ADD COLUMN transaction_id TRANSACTION_ID NULL,
    ADD COLUMN ledger VARCHAR(32) NULL;
ALTER TABLE participants
    ADD COLUMN transaction_id TRANSACTION_ID NULL,
    ADD COLUMN ledger VARCHAR(32) NULL;

-- Back-fill new columns for the existing published_credentials rows, as it's mandatory for it
UPDATE published_credentials
    SET transaction_id = DECODE(node_credential_id, 'hex'),
        ledger = 'InMemory';

-- Make new published_credentials columns no longer optional (participants columns are left optional)
ALTER TABLE published_credentials
    ALTER COLUMN transaction_id SET NOT NULL,
    ALTER COLUMN ledger SET NOT NULL;
