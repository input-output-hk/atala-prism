-- ATA-4504
CREATE TABLE published_batches (
    batch_id TEXT NOT NULL,
    issued_on_transaction_id TRANSACTION_ID NOT NULL,
    ledger VARCHAR(32) NOT NULL,
    issuance_operation_hash TEXT NOT NULL,
    stored_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT published_batches_pk PRIMARY KEY (batch_id)
);

