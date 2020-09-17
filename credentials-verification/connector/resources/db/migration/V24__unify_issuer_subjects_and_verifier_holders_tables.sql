-- ATA-2982: In order to give another step towards a role-less based management console,
-- we would like to unify the roles of subject and holder.
 --  + Subjects are user accounts onboarded by issuers
 --  + Holders are user accounts onboarded by verifiers
 -- We will unify the two concepts as `contacts` and we will replace the tables
 -- `issuer_subjects` and `verifier_holders` into a single `contacts` table.
 --
 -- The current state of both tables are:
 --
 -- geud_connector_db=# \d issuer_subjects
--                               Table "public.issuer_subjects"
--          Column       |              Type              | Collation | Nullable | Default
--    -------------------+--------------------------------+-----------+----------+---------
--     subject_id        | uuid                           |           | not null |
--     created_at        | timestamp with time zone       |           | not null |
--     connection_status | student_connection_status_type |           | not null |
--     connection_token  | text                           |           |          |
--     connection_id     | uuid                           |           |          |
--     subject_data      | jsonb                          |           | not null |
--     external_id       | text                           |           | not null |
--     issuer_id         | uuid                           |           | not null |
--    Indexes:
--        "issuer_subjects_pk" PRIMARY KEY, btree (subject_id)
--        "issuer_subjects_connection_token_unique" UNIQUE CONSTRAINT, btree (connection_token)
--        "issuer_subjects_external_id_unique_per_issuer_id" UNIQUE CONSTRAINT, btree (external_id, issuer_id)
--        "issuer_subjects_connection_token_index" btree (connection_token)
--        "issuer_subjects_created_at_index" btree (created_at)
--        "issuer_subjects_issuer_id_index" btree (issuer_id)
--    Check constraints:
--        "issuer_subjects_external_id_non_empty" CHECK (btrim(external_id) <> ''::text)
--    Foreign-key constraints:
--        "issuer_subjects_issuer_id_fk" FOREIGN KEY (issuer_id) REFERENCES participants(id)
--    Referenced by:
--        TABLE "contacts_per_group" CONSTRAINT "contacts_per_group_subject_id_fk" FOREIGN KEY (subject_id) REFERENCES issuer_subjects(subject_id)
--        TABLE "credentials" CONSTRAINT "credentials_subject_id_fk" FOREIGN KEY (subject_id) REFERENCES issuer_subjects(subject_id)
--
-- geud_connector_db=# \d verifier_holders
--                                                   Table "public.verifier_holders"
--          Column       |               Type                | Collation | Nullable |                   Default
--    -------------------+-----------------------------------+-----------+----------+----------------------------------------------
--     verifier_id       | uuid                              |           | not null |
--     holder_id         | uuid                              |           | not null |
--     connection_status | individual_connection_status_type |           | not null | 'CREATED'::individual_connection_status_type
--     connection_token  | text                              |           |          |
--     connection_id     | uuid                              |           |          |
--     created_at        | timestamp with time zone          |           | not null |
--     holder_data       | jsonb                             |           | not null |
--    Indexes:
--        "verifier_holders_pk" PRIMARY KEY, btree (holder_id)
--        "verifier_holders_connection_id_unique" UNIQUE CONSTRAINT, btree (connection_id)
--        "verifier_holders_connection_token_unique" UNIQUE CONSTRAINT, btree (connection_token)
--        "verifier_holders_connection_token_index" btree (connection_token, holder_id)
--        "verifier_holders_user_id_connection_id_index" btree (verifier_id, connection_id, holder_id)
--        "verifier_holders_user_id_connection_token_index" btree (verifier_id, connection_token, holder_id)
--        "verifier_holders_user_id_created_at_index" btree (verifier_id, created_at, holder_id)
--        "verifier_id_index" btree (verifier_id)
--    Foreign-key constraints:
--        "verifier_holders_participant_id_fk" FOREIGN KEY (verifier_id) REFERENCES participants(id)
--    Referenced by:
--        TABLE "stored_credentials" CONSTRAINT "stored_credentials_connection_id_fk" FOREIGN KEY (connection_id) REFERENCES verifier_holders(connection_id)
--
-- We will define the table:

--          Column       |               Type             | Collation | Nullable |                   Default
--    -------------------+--------------------------------+-----------+----------+----------------------------------------------
--     contact_id        | uuid                           |           | not null |
--     created_by        | uuid                           |           | not null |
--     connection_status | contact_connection_status_type |           | not null |
--     connection_token  | text                           |           |          |
--     connection_id     | uuid                           |           |          |
--     created_at        | timestamp with time zone       |           | not null |
--     contact_data      | jsonb                          |           | not null |
--
-- Note the creation of contact_connection_status_type


-- We start with the connection type. The enum values are the same as the ones in STUDENT_CONNECTION_STATUS_TYPE
CREATE TYPE CONTACT_CONNECTION_STATUS_TYPE AS ENUM(
    'INVITATION_MISSING', -- the contact data is stored but he hasn't been invited to connect on the app
    'CONNECTION_MISSING', -- the contact has been invited to use the app but he hasn't accepted yet
    'CONNECTION_ACCEPTED', -- the contact has accepted the connection
    'CONNECTION_REVOKED' -- the contact revoked the connection
);

CREATE TABLE contacts (
    contact_id UUID NOT NULL,
    external_id TEXT NOT NULL,
    created_by UUID NOT NULL,
    contact_data JSONB NOT NULL,
    connection_status CONTACT_CONNECTION_STATUS_TYPE NOT NULL,
    connection_token TEXT,
    connection_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT contacts_contact_id_pk PRIMARY KEY (contact_id),
    CONSTRAINT contacts_created_by_fk FOREIGN KEY (created_by) REFERENCES participants (id),
    CONSTRAINT contacts_external_id_non_empty_check CHECK (TRIM(external_id) <> ''::TEXT),
    CONSTRAINT contacts_external_id_unique_per_creator UNIQUE (external_id, created_by),
    CONSTRAINT contacts_connection_id_unique UNIQUE (connection_id),
    CONSTRAINT contacts_connection_token_unique UNIQUE (connection_token)
);

CREATE INDEX contacts_created_by_index ON contacts USING BTREE (created_by);
CREATE INDEX contacts_external_id_index ON contacts USING BTREE (external_id);
CREATE INDEX contacts_contact_connection_token_index ON contacts USING BTREE (created_by, connection_token, contact_id);
CREATE INDEX contacts_contact_connection_id_index ON contacts USING BTREE (created_by, connection_id, contact_id);
CREATE INDEX contacts_contact_created_at_index ON contacts USING BTREE (created_by, created_at, contact_id);

-- rename subject_id to contact_id in contacts_per_group
ALTER TABLE contacts_per_group
  RENAME subject_id TO contact_id;

-- We drop all functions and triggers and migrate them to the new table
DROP TRIGGER contacts_per_group_tgr ON contacts_per_group;
DROP FUNCTION contacts_per_group_fun_check;

CREATE FUNCTION contacts_per_group_fun_check()
  RETURNS trigger AS
$func$
BEGIN
   IF (SELECT created_by FROM contacts WHERE contact_id = NEW.contact_id)
      <> (SELECT issuer_id FROM issuer_groups WHERE group_id = NEW.group_id) THEN
      RAISE EXCEPTION 'The group and contact do not belong to the same issuer';
   END IF;
   RETURN NEW;
END
$func$  LANGUAGE plpgsql;

CREATE TRIGGER contacts_per_group_tgr
  BEFORE INSERT ON contacts_per_group
  FOR EACH ROW EXECUTE PROCEDURE contacts_per_group_fun_check();

-- We now populate the new table

-- We start with the values in issuer_subjects, given that the connection status enums share the same values, it is
-- easy to cast one to the other
-- We need to cast the connection status type
INSERT INTO contacts
SELECT subject_id AS contact_id, external_id, issuer_id AS created_by, subject_data AS contact_data, connection_status::TEXT::CONTACT_CONNECTION_STATUS_TYPE, connection_token, connection_id, created_at
FROM issuer_subjects;

-- We re-assign the constraints that pointed to the issuer_subjects table
ALTER TABLE contacts_per_group
  DROP CONSTRAINT contacts_per_group_subject_id_fk,
  ADD CONSTRAINT contacts_per_group_contact_id_fk FOREIGN KEY (contact_id) REFERENCES contacts(contact_id);

ALTER TABLE credentials
  DROP CONSTRAINT credentials_subject_id_fk,
  ADD CONSTRAINT credentials_contact_id_fk FOREIGN KEY (subject_id) REFERENCES contacts(contact_id);

-- For the verifier_holders side, it is a bit trickier because the enum values for connection status are:
--  'CREATED', -- the party data is stored but he hasn't been invited to connect on the app
--  'INVITED', -- the party has been invited to use the app but he hasn't accepted yet
--  'CONNECTED', -- the party has accepted the connection
--  'REVOKED' -- the party revoked the connection

-- We will first update the verifier_holders table and then simply insert the values in the new contacts table
-- We first add the external_id column
-- We first add the new column
ALTER TABLE verifier_holders
  ADD COLUMN external_id TEXT;

-- We now insert the missing values
-- Note that we generate UUID strings following this approach https://stackoverflow.com/a/21327318
-- in order to avoid enabling extensions
UPDATE verifier_holders
  SET external_id = md5(random()::text || clock_timestamp()::text)::uuid::text;

-- We now update the connection_status column
ALTER TABLE verifier_holders
  ALTER COLUMN connection_status DROP DEFAULT,
  ALTER COLUMN connection_status
    SET DATA TYPE CONTACT_CONNECTION_STATUS_TYPE
    USING (
      CASE connection_status::text
        WHEN 'CREATED' THEN 'INVITATION_MISSING'
        WHEN 'INVITED' THEN 'CONNECTION_MISSING'
        WHEN 'CONNECTED' THEN 'CONNECTION_ACCEPTED'
        WHEN 'REVOKED' THEN 'CONNECTION_REVOKED'
      END
    )::CONTACT_CONNECTION_STATUS_TYPE;

-- Finally, we insert the values in the contacts table
INSERT INTO contacts
SELECT holder_id AS contact_id, external_id, verifier_id AS created_by, holder_data AS contact_data, connection_status, connection_token, connection_id, created_at
FROM verifier_holders;

-- We re-assign the constraints that pointed to the verifier_holders table
ALTER TABLE stored_credentials
  DROP CONSTRAINT stored_credentials_connection_id_fk,
  ADD CONSTRAINT stored_credentials_connection_id_fk FOREIGN KEY (connection_id) REFERENCES contacts(connection_id);

-- and we delete the tables
DROP TABLE issuer_subjects;
DROP TABLE verifier_holders;