CREATE TABLE services
(
    service_id              ID_TYPE PRIMARY KEY      NOT NULL,
    id                      TEXT                     NOT NULl,
    did_suffix              ID_TYPE                  NOT NULL,
    type                    TEXT                     NOT NULL,

    added_on_transaction_id TRANSACTION_ID           NOT NULL,
    added_on                TIMESTAMP WITH TIME ZONE NOT NULL,
    added_on_absn           INTEGER                  NOT NULL,
    --^ Atala Block Sequence Number (absn) of the operation that added the key
    added_on_ons            INTEGER                  NOT NULL,
    --^ Operation Sequence Number (osn) of the operation that added the key

    deleted_by_update       BOOLEAN DEFAULT FALSE,
    deleted_by_deletion     BOOLEAN DEFAULT FALSE,

    deleted_on              TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_on_absn         INTEGER                  NOT NULL,
    deleted_on_ons          INTEGER                  NOT NULL,

    ledger                  VARCHAR(32)              NOT NULL,

    CONSTRAINT services_did_suffix_fk
        FOREIGN KEY (did_suffix) REFERENCES did_data (did_suffix),

    CONSTRAINT unique_id_and_did_suffix_on_services UNIQUE (did_suffix, id)

);
