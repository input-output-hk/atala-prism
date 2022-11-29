CREATE TABLE services
(
    service_id                ID_TYPE PRIMARY KEY      NOT NULL,
    did_suffix                ID_TYPE                  NOT NULL,
    type                      TEXT                     NOT NULL,

    added_on_transaction_id   TRANSACTION_ID           NOT NULL,
    added_on                  TIMESTAMP WITH TIME ZONE NOT NULL,
    added_on_absn             INTEGER                  NOT NULL,
    added_on_ons              INTEGER                  NOT NULL,

    revoked_on_transaction_id TRANSACTION_ID           DEFAULT NULL,
    revoked_on                TIMESTAMP WITH TIME ZONE DEFAULT NULL,
    revoked_on_absn           INTEGER                  DEFAULT NULL,
    revoked_on_ons            INTEGER                  DEFAULT NULL,

    updated_on_transaction_id TRANSACTION_ID           DEFAULT NULL,
    updated_on                TIMESTAMP WITH TIME ZONE DEFAULT NULL,
    updated_on_absn           INTEGER                  DEFAULT NULL,
    updated_on_ons            INTEGER                  DEFAULT NULL,

    ledger                    VARCHAR(32)              NOT NULL,

    CONSTRAINT services_did_suffix_fk
        FOREIGN KEY (did_suffix) REFERENCES did_data (did_suffix)

);
