CREATE EXTENSION IF NOT EXISTS CITEXT;

CREATE TABLE issuer_groups(
  group_id UUID NOT NULL,
  issuer_id UUID NOT NULL,
  name CITEXT NOT NULL,
  CONSTRAINT issuer_groups_pk PRIMARY KEY (group_id),
  CONSTRAINT issuer_groups_issuer_id_fk FOREIGN KEY (issuer_id) REFERENCES issuers (issuer_id),
  CONSTRAINT issuer_groups_name_non_empty CHECK (TRIM(name) <> ''),
  CONSTRAINT issuer_groups_unique_issuer_name UNIQUE (issuer_id, name)
);

-- this is the data for the demo
INSERT INTO issuer_groups (group_id, issuer_id, name) VALUES
  ('013b7225-3fe3-4968-b960-cf18ecb02721', 'c8834532-eade-11e9-a88d-d8f2ca059830', 'Bachelors Degree Mathematics and Science 2019'),
  ('70c5fda1-6c89-4645-9461-47ab07278d09', 'c8834532-eade-11e9-a88d-d8f2ca059830', 'Masters Degree Business Administration 2019');
