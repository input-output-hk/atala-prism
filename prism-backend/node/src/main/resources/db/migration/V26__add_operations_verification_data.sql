CREATE TABLE operations_verification_data(
    previous_operation OPERATION_HASH,
    signed_with_did_id TEXT NOT NULL,
    signed_with_key_id TEXT NOT NULL,

    CONSTRAINT previous_operations_unique UNIQUE (previous_operation)
);

CREATE INDEX signed_with_index ON operations_verification_data USING BTREE(signed_with_did_id, signed_with_key_id);

CREATE TABLE revoked_keys(
    did_id TEXT NOT NULL,
    key_id TEXT NOT NULL,

    CONSTRAINT revoked_keys_pk PRIMARY KEY (did_id, key_id)
);
