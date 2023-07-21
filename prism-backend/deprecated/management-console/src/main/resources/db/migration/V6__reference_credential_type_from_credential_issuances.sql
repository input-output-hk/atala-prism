ALTER TABLE credential_issuances
DROP COLUMN credential_type_id,
ADD COLUMN credential_type_id UUID NOT NULL,
ADD CONSTRAINT credential_issuances_credential_type_id_fk FOREIGN KEY (credential_type_id) REFERENCES credential_types (credential_type_id);

CREATE INDEX credential_issuances_credential_type_id_index ON credential_issuances USING BTREE (credential_type_id);