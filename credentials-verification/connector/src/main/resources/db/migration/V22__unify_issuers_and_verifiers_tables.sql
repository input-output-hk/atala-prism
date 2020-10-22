-- As part of ATA-2977, we intend to unify issuers and verifiers tables.
-- The tables only have an id (UUID) in their content that refers (as foreign key) to the participants
-- table (the id column in that table).
-- Given that the connector should eventually be separated to a separate component and we still need
-- to access some data currently contained in the participants table to provide it to the management
-- console, we opted to remove the two tables (issuers and verifiers) and move all the dependencies on
-- them directly to the participants table.

-- We start by migrating the fk references to the issuers(issuer_id) column

ALTER TABLE credentials
  DROP CONSTRAINT credentials_issuer_id_fk,
  ADD CONSTRAINT credentials_issuer_id_fk FOREIGN KEY (issuer_id) REFERENCES participants (id);

-- We add a missing index
CREATE INDEX credentials_subject_id_index ON credentials USING BTREE (subject_id);

ALTER TABLE issuer_groups
  DROP CONSTRAINT issuer_groups_issuer_id_fk,
  ADD CONSTRAINT issuer_groups_issuer_id_fk FOREIGN KEY (issuer_id) REFERENCES participants (id);

-- we add a missing fk index
CREATE INDEX issuer_groups_issuer_id_index ON issuer_groups USING BTREE (issuer_id);

ALTER TABLE issuer_subjects
  DROP CONSTRAINT issuer_subjects_issuer_id_fk,
  ADD CONSTRAINT issuer_subjects_issuer_id_fk FOREIGN KEY (issuer_id) REFERENCES participants (id);


-- We now migrate the fk references to the verifiers(verifier_id) column

ALTER TABLE verifier_holders
  DROP CONSTRAINT verifier_holders_verifier_fk,
  ADD CONSTRAINT verifier_holders_participant_id_fk FOREIGN KEY (verifier_id) REFERENCES participants (id);

-- we add a missing index
CREATE INDEX verifier_id_index ON verifier_holders USING BTREE (verifier_id);

-- Finally, we remove the tables
DROP TABLE verifiers;
DROP TABLE issuers;