
ALTER TABLE participants ADD COLUMN public_key BYTEA NULL;
CREATE UNIQUE INDEX participants_public_key_index ON participants USING BTREE (public_key);
