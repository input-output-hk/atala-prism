-- As part of ATA-4153, we intend to store in the database the information of the
-- underlying blockchain transaction that carried each operation. Before this story,
-- we were only storing timestamp information associated to the transaction. Now, we
-- will also add the transaction id and the ledger where the transaction belongs to.

-- For legacy purpose, we need to add mock data to existing tables
-- We will use:
--  · the InMemoryLedger, and
--  · a proper byte array for transaction id

-- + did_data table
ALTER TABLE did_data
    ADD COLUMN transaction_id TRANSACTION_ID NULL,
    ADD COLUMN ledger VARCHAR(32) NULL,
    -- we will additionally add timestamp information to this table
    ADD COLUMN published_on TIMESTAMPTZ NULL,
    ADD COLUMN published_on_absn INTEGER NULL,
    ADD COLUMN published_on_osn INTEGER NULL;

UPDATE did_data
  SET transaction_id = last_operation::TRANSACTION_ID, -- we can take the bytes from this column
      ledger = 'InMemory',
      published_on = now(),
      published_on_absn = 1,
      published_on_osn = 1;

ALTER TABLE did_data
    ALTER COLUMN transaction_id SET NOT NULL,
    ALTER COLUMN ledger SET NOT NULL,
    ALTER COLUMN published_on SET NOT NULL,
    ALTER COLUMN published_on_absn SET NOT NULL,
    ALTER COLUMN published_on_osn SET NOT NULL;


-- + credentials table
ALTER TABLE credentials
    ADD COLUMN issued_on_transaction_id TRANSACTION_ID NULL,
    ADD COLUMN revoked_on_transaction_id TRANSACTION_ID NULL,
    ADD COLUMN ledger VARCHAR(32) NULL; -- we assume the same ledger for issuance and revocation

UPDATE credentials
  SET issued_on_transaction_id = last_operation::TRANSACTION_ID,
      ledger = 'InMemory';

ALTER TABLE credentials
    ALTER COLUMN issued_on_transaction_id SET NOT NULL,
    ALTER COLUMN ledger SET NOT NULL;

-- + credential_batches table
ALTER TABLE credential_batches
    ADD COLUMN issued_on_transaction_id TRANSACTION_ID NULL,
    ADD COLUMN revoked_on_transaction_id TRANSACTION_ID NULL,
    ADD COLUMN ledger VARCHAR(32) NULL;

UPDATE credential_batches
  SET issued_on_transaction_id = last_operation::TRANSACTION_ID,
      ledger = 'InMemory';

ALTER TABLE credential_batches
    ALTER COLUMN issued_on_transaction_id SET NOT NULL,
    ALTER COLUMN ledger SET NOT NULL;

-- + revoked_credentials table
ALTER TABLE revoked_credentials
    ADD COLUMN transaction_id TRANSACTION_ID NULL,
    ADD COLUMN ledger VARCHAR(32) NULL;

UPDATE revoked_credentials
  SET transaction_id = credential_id::TRANSACTION_ID,
      ledger = 'InMemory';

ALTER TABLE revoked_credentials
    ALTER COLUMN transaction_id SET NOT NULL,
    ALTER COLUMN ledger SET NOT NULL;

-- + public_keys table
-- in this table, there is no BYTEA value we could use for default transaction_id,
-- we neither have a hex encoded BYTEA we could use, we will use the function found
-- here: https://dba.stackexchange.com/questions/22512/how-can-i-generate-a-random-bytea
create function random_bytea(p_length in integer) returns bytea language plpgsql as $$
declare
  o bytea := '';
begin
  for i in 1..p_length loop
    o := o||decode(lpad(to_hex(width_bucket(random(), 0, 1, 256)-1),2,'0'), 'hex');
  end loop;
  return o;
end;$$;
-- it is not the most efficient but it will be enough for our case

ALTER TABLE public_keys
    ADD COLUMN added_on_transaction_id TRANSACTION_ID NULL,
    ADD COLUMN revoked_on_transaction_id TRANSACTION_ID NULL,
    ADD COLUMN ledger VARCHAR(32) NULL; -- we assume the same ledger for key addition and revocation

UPDATE public_keys
  SET added_on_transaction_id = random_bytea(32),
      ledger = 'InMemory';

ALTER TABLE public_keys
    ALTER COLUMN added_on_transaction_id SET NOT NULL,
    ALTER COLUMN ledger SET NOT NULL;
