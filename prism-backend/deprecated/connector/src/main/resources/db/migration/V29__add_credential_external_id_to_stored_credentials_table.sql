-- ATA-3250:
-- In order to move credentials received in the connector to the management console,
-- we need to know which is the last message we processed.
-- We will add a column to the stored credentials table that contains an external id
ALTER TABLE stored_credentials
  ADD COLUMN credential_external_id TEXT;

UPDATE stored_credentials
  SET credential_external_id = storage_id::TEXT;

-- we add a NOT NULL constraint
ALTER TABLE stored_credentials
  ALTER COLUMN credential_external_id SET NOT NULL;

ALTER TABLE stored_credentials
  ADD CONSTRAINT stored_credentials_credential_external_id_unique UNIQUE (credential_external_id);