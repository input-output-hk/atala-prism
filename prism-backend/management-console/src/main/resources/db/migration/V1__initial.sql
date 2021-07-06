-- https://w3c.github.io/did-core/#did-syntax
CREATE DOMAIN DID AS TEXT CHECK(
    VALUE ~ '^did:[a-z0-9]+:[a-zA-Z0-9._-]*(:[a-zA-Z0-9._-]*)*$'
);

CREATE DOMAIN TRANSACTION_ID AS BYTEA
CHECK (
    LENGTH(VALUE) = 32
);

CREATE TABLE participants(
    participant_id UUID NOT NULL,
    name TEXT NOT NULL,
    did DID NOT NULL,
    logo BYTEA NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT participants_participant_id_pk PRIMARY KEY (participant_id),
    CONSTRAINT participants_did_unique UNIQUE (did)
);

-- As nonces are supposed to be random, it is likely that they will be unique
-- so, using the nonce as the first argument for the index speeds up the queries.
CREATE TABLE request_nonces (
    request_nonce BYTEA NOT NULL,
    participant_id UUID NOT NULL,
    CONSTRAINT request_nonces_pk PRIMARY KEY (request_nonce, participant_id),
    CONSTRAINT request_nonces_participant_id_fk FOREIGN KEY (participant_id) REFERENCES participants (participant_id)
);

CREATE TABLE contacts (
    contact_id UUID NOT NULL,
    external_id TEXT NOT NULL,
    created_by UUID NOT NULL,
    contact_data JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    name TEXT NOT NULL,
    CONSTRAINT contacts_contact_id_pk PRIMARY KEY (contact_id),
    CONSTRAINT contacts_created_by_fk FOREIGN KEY (created_by) REFERENCES participants (participant_id),
    CONSTRAINT contacts_external_id_non_empty_check CHECK (TRIM(external_id) <> ''::TEXT),
    CONSTRAINT contacts_external_id_unique_per_creator UNIQUE (external_id, created_by)
);

CREATE INDEX contacts_created_by_index ON contacts USING BTREE (created_by);
CREATE INDEX contacts_external_id_index ON contacts USING BTREE (external_id);
CREATE INDEX contacts_contact_created_at_index ON contacts USING BTREE (created_by, created_at, contact_id);
CREATE INDEX contacts_contact_name_index ON contacts USING BTREE (LOWER(name)); -- used to sort results by name
CREATE INDEX contacts_contact_name_gin_index ON contacts USING BTREE (name);
CREATE INDEX contacts_external_id_gin_index ON contacts USING BTREE (external_id);

CREATE TABLE institution_groups(
    group_id UUID NOT NULL,
    institution_id UUID NOT NULL,
    name TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT institution_groups_pk PRIMARY KEY (group_id),
    CONSTRAINT institution_groups_institution_id_fk FOREIGN KEY (institution_id) REFERENCES participants (participant_id),
    CONSTRAINT institution_groups_name_non_empty CHECK (TRIM(name) <> ''),
    CONSTRAINT institution_groups_unique_institution_name UNIQUE (institution_id, name)
);

CREATE INDEX institution_groups_institution_id_index ON institution_groups USING BTREE (institution_id);

CREATE TABLE contacts_per_group (
    group_id UUID NOT NULL,
    contact_id UUID NOT NULL,
    added_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT contacts_per_group_pk PRIMARY KEY (group_id, contact_id),
    CONSTRAINT contacts_per_group_group_id_fk FOREIGN KEY (group_id) REFERENCES institution_groups (group_id),
    CONSTRAINT contacts_per_group_contact_id_fk FOREIGN KEY (contact_id) REFERENCES contacts (contact_id)
);

CREATE INDEX contacts_per_group_group_id_index ON contacts_per_group USING BTREE (group_id);
CREATE INDEX contacts_per_group_contact_id_index ON contacts_per_group USING BTREE (contact_id);

-- We add a constraint that relates contacts and groups tables
-- This is,
--  + Every contact has a unique creator associated to it
--  + Every group also has a unique institution_id associated to it
-- We would like to enforce that, if a contact is added to a group, then
-- both must be associated to the same institution_id. If not, we reject the addition.
CREATE FUNCTION contacts_per_group_fun_check()
    RETURNS trigger AS
$func$
BEGIN
    IF (SELECT created_by FROM contacts WHERE contact_id = NEW.contact_id)
        <> (SELECT institution_id FROM institution_groups WHERE group_id = NEW.group_id) THEN
        RAISE EXCEPTION 'The group and contact do not belong to the same institution';
    END IF;
    RETURN NEW;
END
$func$  LANGUAGE plpgsql;

CREATE TRIGGER contacts_per_group_tgr
    BEFORE INSERT ON contacts_per_group
    FOR EACH ROW EXECUTE PROCEDURE contacts_per_group_fun_check();

CREATE TABLE draft_credentials(
    credential_id UUID NOT NULL,
    issuer_id UUID NOT NULL,
    contact_id UUID NOT NULL,
    created_on TIMESTAMPTZ NOT NULL,
    credential_data JSONB NOT NULL,
    CONSTRAINT draft_credentials_id_pk PRIMARY KEY (credential_id),
    CONSTRAINT draft_credentials_issuer_id_fk FOREIGN KEY (issuer_id) REFERENCES participants (participant_id),
    CONSTRAINT draft_credentials_contact_id_fk FOREIGN KEY (contact_id) REFERENCES contacts (contact_id)
);

CREATE INDEX draft_credentials_issuer_id_index ON draft_credentials USING BTREE (issuer_id);
CREATE INDEX draft_credentials_created_on_index ON draft_credentials USING BTREE (created_on);
CREATE INDEX draft_credentials_contact_id_index ON draft_credentials USING BTREE (contact_id);

CREATE TABLE published_credentials(
    -- the id that the cmanager assigns to the credential
    credential_id UUID NOT NULL,
    -- the id that the node assigns to the credential
    node_credential_id TEXT NOT NULL,
    -- the hex encoded hash of the AtalaOperation that is used to issue the credential
    operation_hash TEXT NOT NULL,
    -- the encoded signed credential (e.g. a compact JWS string)
    encoded_signed_credential TEXT NOT NULL,
    -- the timestamp when the credential was stored by management console
    stored_at TIMESTAMPTZ NOT NULL,
    -- the last time the credential was shared to the related contact which belongs
    -- to the published_credentials because a non-published credential shouldn't be shared
    shared_at TIMESTAMPTZ NULL DEFAULT NULL,
    transaction_id TRANSACTION_ID NOT NULL,
    ledger VARCHAR(32) NOT NULL,
    CONSTRAINT published_credentials_pk PRIMARY KEY (credential_id),
    CONSTRAINT published_credentials_credential_id_fk FOREIGN KEY (credential_id) references draft_credentials (credential_id)
);

CREATE TABLE received_credentials(
    received_id UUID NOT NULL,
    contact_id UUID NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    encoded_signed_credential TEXT NOT NULL,
    credential_external_id TEXT NOT NULL,
    CONSTRAINT received_credentials_pk PRIMARY KEY (received_id),
    CONSTRAINT received_credentials_credential_external_id_unique UNIQUE (credential_external_id)
);

CREATE INDEX received_credentials_contact_id_index ON received_credentials (contact_id);
