-- We first add the new column with unstructured data
ALTER TABLE credentials
  ADD COLUMN credential_data JSONB;
UPDATE credentials
  SET credential_data = jsonb_build_object('title', title, 'enrollment_date', enrollment_date, 'graduation_date', graduation_date);
-- we add a NOT NULL constraint

ALTER TABLE credentials
  ALTER COLUMN credential_data SET NOT NULL;
-- Finally, we remove the columns we replaced
ALTER TABLE credentials
  DROP COLUMN title,
  DROP COLUMN enrollment_date,
  DROP COLUMN graduation_date;

-- We rename constraints names
ALTER TABLE credentials
  RENAME CONSTRAINT credentials_issuer_by_fk TO credentials_issuer_id_fk;