CREATE DOMAIN TRANSACTION_ID AS BYTEA
CHECK (
  LENGTH(VALUE) = 32
);

-- Add optional columns to the table
ALTER TABLE atala_objects
    ADD COLUMN transaction_id TRANSACTION_ID NULL,
    ADD COLUMN ledger VARCHAR(32) NULL;

-- Back-fill new columns for the existing rows
UPDATE atala_objects
    SET transaction_id = atala_object_id,
        ledger = 'InMemory';

-- Make new columns no longer optional
ALTER TABLE atala_objects
    ALTER COLUMN transaction_id SET NOT NULL,
    ALTER COLUMN ledger SET NOT NULL;
