-- We first add the new column with unstructured data
ALTER TABLE verifier_holders
  ADD COLUMN holder_data JSONB;
UPDATE verifier_holders
  SET holder_data = jsonb_build_object('full_name', full_name, 'email', email);
-- we add a NOT NULL constraint
ALTER TABLE verifier_holders
  ALTER COLUMN holder_data SET NOT NULL;
-- Finally, we remove the columns we replaced
ALTER TABLE verifier_holders
  DROP COLUMN full_name,
  DROP COLUMN email;

-- We now rename columns to make things more descriptive
ALTER TABLE verifier_holders
  RENAME user_id TO verifier_id;
ALTER TABLE verifier_holders
  RENAME individual_id TO holder_id;
ALTER TABLE verifier_holders
  RENAME status TO connection_status;

-- We rename constraints names
ALTER TABLE verifier_holders
  RENAME CONSTRAINT store_individuals_pk TO verifier_holders_pk;
ALTER TABLE verifier_holders
  RENAME CONSTRAINT store_individuals_token_per_user_unique TO verifier_holders_token_per_user_unique;
ALTER TABLE verifier_holders
  RENAME CONSTRAINT store_individuals_id_per_user_unique TO verifier_holders_id_per_user_unique;
ALTER TABLE verifier_holders
  RENAME CONSTRAINT store_individuals_user TO verifier_holders_verifier_fk;

-- We rename indexes names
ALTER INDEX store_individuals_connection_token_index RENAME TO verifier_holders_connection_token_index;
ALTER INDEX store_individuals_user_id_connection_id_index RENAME TO verifier_holders_user_id_connection_id_index;
ALTER INDEX store_individuals_user_id_connection_token_index RENAME TO verifier_holders_user_id_connection_token_index;
ALTER INDEX store_individuals_user_id_created_at_index RENAME TO verifier_holders_user_id_created_at_index;
