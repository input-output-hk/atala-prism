-- ATA-4345
-- In this migration we want to add a column to the stored_credentials table and
-- store the merkle inclusion proof associated to a credential

-- Given that no credential currently exist, we just clean the table for sake of sanity
DELETE FROM stored_credentials;

-- We now add the column
ALTER TAbLE stored_credentials
    ADD COLUMN inclusion_proof TEXT NOT NULL;

