-- Create table to hold atala_objects' transaction info
CREATE TABLE atala_object_txs (
    atala_object_id ATALA_OBJECT_ID NOT NULL,
    ledger VARCHAR(32) NOT NULL,
    block_number INT NOT NULL,
    block_timestamp TIMESTAMPTZ NOT NULL,
    block_index INTEGER NOT NULL,
    transaction_id TRANSACTION_ID NOT NULL,

    CONSTRAINT atala_object_txs_pk PRIMARY KEY (atala_object_id),
    CONSTRAINT atala_object_txs_atala_object_id_fk
        FOREIGN KEY (atala_object_id)
        REFERENCES atala_objects (atala_object_id)
);

-- Migrate existing data (set a hard-coded block_number of 1)
INSERT INTO atala_object_txs
    SELECT atala_object_id,
           ledger,
           1 as block_number,
           object_timestamp AS block_timestamp,
           sequence_number AS block_index,
           transaction_id
    FROM atala_objects;

-- Drop old columns
ALTER TABLE atala_objects
    DROP COLUMN sequence_number,
    DROP COLUMN object_timestamp,
    DROP COLUMN transaction_id,
    DROP COLUMN ledger;
