-- ATL-220: Store the credentials proof
ALTER TABLE received_credentials
    ADD COLUMN inclusion_proof TEXT NULL;

