CREATE DOMAIN OPERATION_ID AS BYTEA
CHECK (
    LENGTH(VALUE) = 32
);

ALTER TABLE published_batches
    ALTER COLUMN issuance_operation_hash TYPE OPERATION_ID USING decode(issuance_operation_hash, 'hex');
