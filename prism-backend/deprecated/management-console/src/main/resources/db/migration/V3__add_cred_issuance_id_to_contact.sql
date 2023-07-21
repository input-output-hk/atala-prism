-- Link contacts to the credential_issuance they belong to, to avoid expensive queries
ALTER TABLE credential_issuance_contacts
    ADD COLUMN credential_issuance_id UUID NOT NULL,
    ADD CONSTRAINT credential_issuance_contacts_issuance_id_fk
        FOREIGN KEY (credential_issuance_id)
        REFERENCES credential_issuances (credential_issuance_id);
