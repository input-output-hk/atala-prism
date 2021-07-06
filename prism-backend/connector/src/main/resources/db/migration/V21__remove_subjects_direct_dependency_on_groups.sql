-- Until this migration, we have some inconsistent situation in our constraints w.r.t. our requirements:
--  + Every issuer can create multiple groups
--  + Every subject must be assigned to a unique group
-- As part of ATA-2989, we would like to allow a subject to be assigned to zero or multiple groups related to the _same_
-- issuer.
-- For this, we need to:
--  + Create a new table "contacts_per_group" to capture the M:N relation
--  + Remove the group_id column in the issuer_subjects table (which restricts the relation to 1:N)
--  + Add an issuer_id column in the issuer_subjects table to keep track of subjects associated by issuer. This is
--    intended to be a 1:N relation, i.e. a subject can only be assigned to one issuer.
--  + Create constraints to ensure that all groups a subject belong to, match the issuer the subject is associated to.

-- Let us start by creating the M:N relation table
CREATE TABLE contacts_per_group (
  group_id UUID NOT NULL,
  subject_id UUID NOT NULL,
  added_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT contacts_per_group_pk PRIMARY KEY (group_id, subject_id),
  CONSTRAINT contacts_per_group_group_id_fk FOREIGN KEY (group_id) REFERENCES issuer_groups (group_id),
  CONSTRAINT contacts_per_group_subject_id_fk FOREIGN KEY (subject_id) REFERENCES issuer_subjects (subject_id)
);

CREATE INDEX contacts_per_group_group_id_index ON contacts_per_group USING BTREE (group_id);
CREATE INDEX contacts_per_group_subject_id_index ON contacts_per_group USING BTREE (subject_id);

-- We need to add all the current pairs to the contacts_per_group table
INSERT INTO contacts_per_group
SELECT issuer_groups.group_id, subject_id, now()
FROM issuer_groups JOIN issuer_subjects USING (group_id);

-- We now go to the updates in the issuer_subjects table

-- We create a new column to relate the subject to its corresponding issuer
ALTER TABLE issuer_subjects
  ADD COLUMN issuer_id UUID;

-- We populate the issuer_id column. Currently, every subject is associated to only one issuer_group
UPDATE issuer_subjects sub
  SET issuer_id = (
    SELECT DISTINCT issuer_groups.issuer_id
    FROM issuer_groups JOIN issuer_subjects isu USING (group_id)
    WHERE isu.subject_id = sub.subject_id
  );

-- We now update constraints related to issuer groups
ALTER TABLE issuer_subjects
   DROP CONSTRAINT issuer_subjects_group_id_fk,
   ADD CONSTRAINT issuer_subjects_issuer_id_fk FOREIGN KEY (issuer_id) REFERENCES issuers (issuer_id);

-- We update indexes
DROP INDEX issuer_subjects_group_id_index;
CREATE INDEX issuer_subjects_issuer_id_index ON issuer_subjects USING BTREE (issuer_id);

-- We add a NOT NULL constraint to issuer_id
ALTER TABLE issuer_subjects
  ALTER COLUMN issuer_id SET NOT NULL;

-- Given that we now remove the group_id from issuer_subjects
-- we need to add a constraint to restrict external_ids to be unique per issuer_id
ALTER TABLE issuer_subjects
  DROP CONSTRAINT issuer_subjects_external_id_unique_per_group,
  ADD CONSTRAINT issuer_subjects_external_id_unique_per_issuer_id UNIQUE (external_id, issuer_id);

-- Finally, we remove the group_id reference from the subjects table
ALTER TABLE issuer_subjects
  DROP COLUMN group_id;

-- Finally, we add a constraint that relates all tables
-- This is,
--  + Every subject has a unique issuer_id associated to it
--  + Every group also has a unique issuer_id associated to it
-- We would like to enforce that, if a subject is added to a group, then
-- both must be associated to the same issuer_id. If not, we reject the addition.
CREATE FUNCTION contacts_per_group_fun_check()
  RETURNS trigger AS
$func$
BEGIN
   IF (SELECT issuer_id FROM issuer_subjects WHERE subject_id = NEW.subject_id)
      <> (SELECT issuer_id FROM issuer_groups WHERE group_id = NEW.group_id) THEN
      RAISE EXCEPTION 'The group and subject do not belong to the same issuer';
   END IF;
   RETURN NEW;
END
$func$  LANGUAGE plpgsql;

CREATE TRIGGER contacts_per_group_tgr
  BEFORE INSERT ON contacts_per_group
  FOR EACH ROW EXECUTE PROCEDURE contacts_per_group_fun_check();
