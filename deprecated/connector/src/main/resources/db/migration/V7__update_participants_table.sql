
ALTER TABLE participants ADD COLUMN public_key BYTEA NULL; -- public_key is mandatory when participant is holder
CREATE UNIQUE INDEX participants_public_key_index ON participants USING BTREE (public_key);
