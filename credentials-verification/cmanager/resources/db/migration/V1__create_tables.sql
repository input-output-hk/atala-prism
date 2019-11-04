
CREATE TABLE issuers (
  issuer_id UUID NOT NULL,
  did TEXT NOT NULL,
  name TEXT NOT NULL,
  CONSTRAINT issuers_id_pk PRIMARY KEY (issuer_id),
  CONSTRAINT issuers_did_unique UNIQUE (did)
);

CREATE TABLE credentials (
  credential_id UUID NOT NULL,
  issued_by UUID NOT NULL,
  subject TEXT NOT NULL,
  title TEXT NOT NULL,
  enrollment_date DATE NOT NULL,
  graduation_date DATE NOT NULL,
  group_name TEXT NOT NULL,
  created_on TIMESTAMPTZ NOT NULL,
  CONSTRAINT credentials_id_pk PRIMARY KEY (credential_id),
  CONSTRAINT credentials_issuer_by_fk FOREIGN KEY (issued_by) REFERENCES issuers (issuer_id)
);

CREATE INDEX credentials_issued_by_index ON credentials USING BTREE (issued_by);
CREATE INDEX credentials_created_on_index ON credentials USING BTREE (created_on);

-- INSERT mock data
INSERT INTO issuers (issuer_id, did, name) VALUES ('c8834532-eade-11e9-a88d-d8f2ca059830', 'did:test:issuer-1', 'Issuer 1');
