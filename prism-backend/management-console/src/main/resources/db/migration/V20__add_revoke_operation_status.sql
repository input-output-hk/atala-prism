-- this property will be never set in the db it will populated dynamically
ALTER TABLE published_credentials
    ADD COLUMN revoked_on_operation_status TEXT NULL;

