
-- issuers
CREATE TABLE issuers (
  issuer_id UUID NOT NULL,
  did TEXT NOT NULL,
  name TEXT NOT NULL,
  CONSTRAINT issuers_id_pk PRIMARY KEY (issuer_id),
  CONSTRAINT issuers_did_unique UNIQUE (did)
);

-- students-
CREATE TYPE STUDENT_CONNECTION_STATUS_TYPE AS ENUM(
    'INVITATION_MISSING', -- the student data is stored but he hasn't been invited to connect on the app
    'CONNECTION_MISSING', -- the student has been invited to use the app but he hasn't accepted yet
    'CONNECTION_ACCEPTED', -- the student has accepted the connection
    'CONNECTION_REVOKED' -- the student revoked the connection
);

CREATE TABLE students (
  student_id UUID NOT NULL,
  issuer_id UUID NOT NULL,
  university_assigned_id TEXT NOT NULL,
  full_name TEXT NOT NULL,
  email TEXT NOT NULL,
  admission_date DATE NOT NULL,
  created_on TIMESTAMPTZ NOT NULL,
  connection_status STUDENT_CONNECTION_STATUS_TYPE NOT NULL,
  connection_token TEXT NULL, -- non-empty when the status is CONNECTION_MISSING
  connection_id UUID NULL, -- non-empty when the status is CONNECTION_ACCEPTED
  CONSTRAINT students_id PRIMARY KEY (student_id),
  CONSTRAINT students_id_fk FOREIGN KEY (issuer_id) REFERENCES issuers (issuer_id),
  CONSTRAINT students_connection_token_unique UNIQUE (connection_token)
);

CREATE INDEX students_issuer_index ON students USING BTREE (student_id);
CREATE INDEX students_created_on_index ON students USING BTREE (created_on);
CREATE INDEX students_connection_token_index ON students USING BTREE (connection_token);

-- credentials
CREATE TABLE credentials (
  credential_id UUID NOT NULL,
  issuer_id UUID NOT NULL,
  student_id UUID NOT NULL,
  title TEXT NOT NULL,
  enrollment_date DATE NOT NULL,
  graduation_date DATE NOT NULL,
  group_name TEXT NOT NULL,
  created_on TIMESTAMPTZ NOT NULL,
  CONSTRAINT credentials_id_pk PRIMARY KEY (credential_id),
  CONSTRAINT credentials_issuer_by_fk FOREIGN KEY (issuer_id) REFERENCES issuers (issuer_id),
  CONSTRAINT credentials_student_id_fk FOREIGN KEY (student_id) REFERENCES students (student_id)
);

CREATE INDEX credentials_issuer_id_index ON credentials USING BTREE (issuer_id);
CREATE INDEX credentials_created_on_index ON credentials USING BTREE (created_on);

-- INSERT mock data
INSERT INTO issuers (issuer_id, did, name) VALUES ('c8834532-eade-11e9-a88d-d8f2ca059830', 'did:test:issuer-1', 'Issuer 1');
