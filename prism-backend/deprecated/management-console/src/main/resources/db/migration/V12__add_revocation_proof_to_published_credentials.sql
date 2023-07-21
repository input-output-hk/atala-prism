
ALTER TAbLE published_credentials
    ADD COLUMN revoked_on_transaction_id TRANSACTION_ID NULL;
