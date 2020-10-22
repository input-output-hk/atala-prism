CREATE TYPE ATALA_OBJECT_TRANSACTION_STATUS AS ENUM('PENDING', 'DELETED', 'IN_LEDGER');

-- Create table to record all transaction submissions
CREATE TABLE atala_object_tx_submissions (
    atala_object_id ATALA_OBJECT_ID NOT NULL,
    ledger VARCHAR(32) NOT NULL,
    transaction_id TRANSACTION_ID NOT NULL,
    submission_timestamp TIMESTAMPTZ NOT NULL,
    status ATALA_OBJECT_TRANSACTION_STATUS NOT NULL,

    CONSTRAINT atala_object_tx_submissions_pk PRIMARY KEY (ledger, transaction_id),
    CONSTRAINT atala_object_tx_submissions_atala_object_id_fk
        FOREIGN KEY (atala_object_id)
        REFERENCES atala_objects (atala_object_id)
);
