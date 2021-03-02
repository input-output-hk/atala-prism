-- Include the transaction id when a credential is revoked
-- ledger's data is already stored on the published_batches table.
ALTER TAbLE published_credentials
    ADD COLUMN revoked_on_transaction_id TRANSACTION_ID NULL;
