
CREATE TYPE CREDENTIAL_TYPE_STATE AS ENUM ('DRAFT', 'READY', 'ARCHIVED');

CREATE TABLE credential_types(
    credential_type_id UUID NOT NULL,
    name TEXT NOT NULL,
    institution_id UUID NOT NULL,
    state CREDENTIAL_TYPE_STATE NOT NULL,
    template TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT credential_types_pk PRIMARY KEY (credential_type_id),
    CONSTRAINT credential_types_name_non_empty CHECK (TRIM(name) <> ''),
    CONSTRAINT credential_types_unique_institution_name UNIQUE (institution_id, name),
    CONSTRAINT credential_types_institution_id_fk FOREIGN KEY (institution_id) REFERENCES participants (participant_id)
);

CREATE INDEX credential_types_institution_id_index ON credential_types USING BTREE (credential_type_id);

CREATE TABLE credential_type_fields(
    credential_type_field_id UUID NOT NULL,
    credential_type_id UUID NOT NULL,
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    CONSTRAINT credential_type_fields_pk PRIMARY KEY (credential_type_field_id),
    CONSTRAINT credential_type_fields_credential_type_id_fk FOREIGN KEY (credential_type_id) REFERENCES credential_types (credential_type_id),
    CONSTRAINT credential_type_fields_unique_credential_type_id_name UNIQUE (credential_type_id, name)
);

CREATE INDEX credential_type_fields_credential_type_id_index ON credential_type_fields USING BTREE (credential_type_id);

ALTER TABLE draft_credentials
ADD COLUMN credential_type_id UUID,
ADD CONSTRAINT draft_credentials_credential_type_id_fk FOREIGN KEY (credential_type_id) REFERENCES credential_types (credential_type_id);

CREATE INDEX draft_credentials_credential_type_id_index ON draft_credentials USING BTREE (credential_type_id);