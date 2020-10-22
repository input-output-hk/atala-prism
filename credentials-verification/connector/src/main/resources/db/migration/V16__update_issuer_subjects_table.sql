-- We first add the new column with unstructured data
ALTER TABLE issuer_subjects
  ADD COLUMN subject_data JSONB;
UPDATE issuer_subjects
  SET subject_data = jsonb_build_object('university_assigned_id', university_assigned_id, 'full_name', full_name, 'email', email, 'admission_date', admission_date);
-- we add a NOT NULL constraint
ALTER TABLE issuer_subjects
  ALTER COLUMN subject_data SET NOT NULL;
-- Finally, we remove the columns we replaced
ALTER TABLE issuer_subjects
  DROP COLUMN university_assigned_id,
  DROP COLUMN full_name,
  DROP COLUMN email,
  DROP COLUMN admission_date;

-- We now rename columns to make things more descriptive
ALTER TABLE issuer_subjects
  RENAME student_id TO subject_id;
ALTER TABLE issuer_subjects
  RENAME created_on TO created_at;

-- We rename constraints names
ALTER TABLE issuer_subjects
  RENAME CONSTRAINT students_id TO issuer_subjects_pk;
ALTER TABLE issuer_subjects
  RENAME CONSTRAINT students_connection_token_unique TO issuer_subjects_connection_token_unique;
ALTER TABLE issuer_subjects
  RENAME CONSTRAINT students_group_id_fk TO issuer_subjects_group_id_fk;

-- We rename indexes names
ALTER INDEX students_group_id_index RENAME TO issuer_subjects_group_id_index;
ALTER INDEX subjects_connection_token_index RENAME TO issuer_subjects_connection_token_index;
ALTER INDEX subjects_created_on_index RENAME TO issuer_subjects_created_at_index;

-- For simplicity, we will rename in this migration the credential column `student_id`
ALTER TABLE credentials
  RENAME student_id TO subject_id;
