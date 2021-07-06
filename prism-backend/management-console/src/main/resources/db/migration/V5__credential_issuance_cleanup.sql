-- Drop columns tracking status and its update times
ALTER TABLE credential_issuances
  DROP COLUMN status,
  DROP COLUMN ready_at,
  DROP COLUMN completed_at;

-- Drop the type as it's not needed anymore
DROP TYPE CREDENTIAL_ISSUANCE_STATUS_TYPE;

-- Add missing foreign key
ALTER TABLE draft_credentials
  ADD CONSTRAINT draft_credentials_credential_issuance_contact_id_fk FOREIGN KEY (credential_issuance_contact_id)
      REFERENCES credential_issuance_contacts (credential_issuance_contact_id)
