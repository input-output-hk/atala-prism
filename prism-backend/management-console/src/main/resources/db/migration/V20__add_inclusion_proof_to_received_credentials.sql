-- ATL-220
ALTER TABLE received_credentials
    ADD COLUMN inclusion_proof TEXT NULL; -- FIXME shoud we use TEXT NOT NULL DEFAULT '';

