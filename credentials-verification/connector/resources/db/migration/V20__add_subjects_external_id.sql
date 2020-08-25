-- We first add the new column
ALTER TABLE issuer_subjects
  ADD COLUMN external_id TEXT;

-- We now insert the missing values
-- Note that we generate UUID strings following this approach https://stackoverflow.com/a/21327318
-- in order to avoid enabling extensions
UPDATE issuer_subjects
  SET external_id = md5(random()::text || clock_timestamp()::text)::uuid::text;

-- we add a NOT NULL constraint
ALTER TABLE issuer_subjects
  ALTER COLUMN external_id SET NOT NULL;

-- We want to avoid empty external ids too
ALTER TABLE issuer_subjects
  ADD CONSTRAINT issuer_subjects_external_id_non_empty CHECK (TRIM(external_id) <> ''::TEXT);

-- We temporarily add this UNIQUE constraint to avoid subject duplication within the same group
-- However, this constraint should actually be a UNIQUE per issuer. We should change this once
-- we correct the table structure (i.e. delete group_id and replace it with issuer_id)
ALTER TABLE issuer_subjects
  ADD CONSTRAINT issuer_subjects_external_id_unique_per_group UNIQUE (external_id, group_id);