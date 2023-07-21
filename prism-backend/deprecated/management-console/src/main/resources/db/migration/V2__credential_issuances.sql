-- Status of credential issuances
CREATE TYPE CREDENTIAL_ISSUANCE_STATUS_TYPE AS ENUM(
    'DRAFT', -- the credential issuance was created but some data is missing
    'READY', -- the credential issuance has all the data needed, but some credentials have not been published yet
    'COMPLETED' -- the credential issuance has all its credentials published
);

-- Group of credentials that can be issued together
CREATE TABLE credential_issuances(
    credential_issuance_id UUID NOT NULL,
    name TEXT NOT NULL,
    status CREDENTIAL_ISSUANCE_STATUS_TYPE NOT NULL,
    created_by UUID NOT NULL,
    -- ID of the credential template to use (these are hardcoded, but eventually will reference a table)
    credential_type_id INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    ready_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,

    CONSTRAINT credential_issuances_pk PRIMARY KEY (credential_issuance_id),
    -- Name within an institution is unique
    CONSTRAINT credential_issuances_name_unique UNIQUE (created_by, name),
    CONSTRAINT credential_issuances_created_by_fk FOREIGN KEY (created_by) REFERENCES participants (participant_id)
);

CREATE INDEX credential_issuances_filter_index ON credential_issuances USING BTREE (created_by, status, created_at);

-- Copy of `institution_groups` when it was added to the credential issuance
CREATE TABLE credential_issuance_groups(
    credential_issuance_group_id UUID NOT NULL,
    -- credential_issuance this group belongs to
    credential_issuance_id UUID NOT NULL,
    -- contact_group this group comes from
    contact_group_id UUID NOT NULL,

    CONSTRAINT credential_issuance_groups_pk PRIMARY KEY (credential_issuance_group_id),
    -- A contact_group can be added to a credential_issuance only once
    CONSTRAINT credential_issuance_groups_unique UNIQUE (credential_issuance_id, contact_group_id),
    CONSTRAINT credential_issuance_groups_issuance_id_fk FOREIGN KEY (credential_issuance_id)
        REFERENCES credential_issuances (credential_issuance_id),
    CONSTRAINT credential_issuance_groups_contact_group_id_fk FOREIGN KEY (contact_group_id)
        REFERENCES institution_groups (group_id)
);

CREATE INDEX credential_issuance_groups_issuance_index
    ON credential_issuance_groups USING BTREE (credential_issuance_id, contact_group_id);

-- Contacts that belong directly or indirectly (via a group) to a credential issuance
CREATE TABLE credential_issuance_contacts(
    credential_issuance_contact_id UUID NOT NULL,
    contact_id UUID NOT NULL,
    credential_data JSONB,

    CONSTRAINT credential_issuance_contacts_pk PRIMARY KEY (credential_issuance_contact_id),
    CONSTRAINT credential_issuance_contacts_id_fk FOREIGN KEY (contact_id)
        REFERENCES contacts (contact_id)
);

CREATE INDEX credential_issuance_contacts_contact_index
    ON credential_issuance_contacts USING BTREE (contact_id);

-- 1:N mapping to associate one group to many contacts
CREATE TABLE contacts_per_credential_issuance_group(
    credential_issuance_group_id UUID NOT NULL,
    credential_issuance_contact_id UUID NOT NULL,

    CONSTRAINT contacts_per_credential_issuance_group_pk
        PRIMARY KEY (credential_issuance_group_id, credential_issuance_contact_id),
    CONSTRAINT contacts_per_credential_issuance_group_id_fk FOREIGN KEY (credential_issuance_group_id)
        REFERENCES credential_issuance_groups (credential_issuance_group_id),
    CONSTRAINT contacts_per_credential_issuance_group_contact_id_fk FOREIGN KEY (credential_issuance_contact_id)
        REFERENCES credential_issuance_contacts (credential_issuance_contact_id)
);

CREATE INDEX contacts_per_credential_issuance_group_group_index
    ON contacts_per_credential_issuance_group USING BTREE (credential_issuance_group_id);
CREATE INDEX contacts_per_credential_issuance_group_contact_index
    ON contacts_per_credential_issuance_group USING BTREE (credential_issuance_contact_id);

-- Contacts directly added to the credential issuance (not contained in a group)
CREATE TABLE contacts_per_credential_issuance(
    credential_issuance_id UUID NOT NULL,
    credential_issuance_contact_id UUID NOT NULL,

    CONSTRAINT contacts_per_credential_issuance_pk
        PRIMARY KEY (credential_issuance_id, credential_issuance_contact_id),
    CONSTRAINT contacts_per_credential_issuance_id_fk FOREIGN KEY (credential_issuance_id)
        REFERENCES credential_issuances (credential_issuance_id),
    CONSTRAINT contacts_per_credential_issuance_contact_id_fk FOREIGN KEY (credential_issuance_contact_id)
        REFERENCES credential_issuance_contacts (credential_issuance_contact_id)
);

CREATE INDEX contacts_per_credential_issuance_id_index
    ON contacts_per_credential_issuance USING BTREE (credential_issuance_id);
CREATE INDEX contacts_per_credential_issuance_contact_index
    ON contacts_per_credential_issuance USING BTREE (credential_issuance_contact_id);

-- Link back from a credential to the issuance
ALTER TABLE draft_credentials
    ADD COLUMN credential_issuance_contact_id UUID;
