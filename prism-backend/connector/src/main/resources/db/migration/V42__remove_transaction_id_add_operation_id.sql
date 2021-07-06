-- Include the corresponding operation id instead of a transaction id.
ALTER TABLE published_credentials
    DROP COLUMN revoked_on_transaction_id;

ALTER TABLE published_credentials
    ADD COLUMN revoked_on_operation_id OPERATION_ID NULL;

ALTER TABLE published_batches
    DROP COLUMN issued_on_transaction_id,
    DROP COLUMN ledger;

ALTER TABLE published_batches
    ADD COLUMN issuance_operation_id OPERATION_ID NULL;

ALTER TABLE participants
    DROP COLUMN transaction_id,
    DROP COLUMN ledger;

ALTER TABLE participants
    ADD COLUMN operation_id OPERATION_ID NULL;
