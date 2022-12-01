CREATE TABLE services
(
    service_id                ID_TYPE PRIMARY KEY      NOT NULL,
    id                        TEXT                     NOT NULl,
    did_suffix                ID_TYPE                  NOT NULL,
    type                      TEXT                     NOT NULL,

    added_on_transaction_id   TRANSACTION_ID           NOT NULL,
    added_on                  TIMESTAMP WITH TIME ZONE NOT NULL,
    added_on_absn             INTEGER                  NOT NULL,
    --^ Atala Block Sequence Number (absn) of the operation that added the key
    added_on_ons              INTEGER                  NOT NULL,
    --^ Operation Sequence Number (osn) of the operation that added the key

    deleted_on_transaction_id TRANSACTION_ID           DEFAULT NULL,
    deleted_on                TIMESTAMP WITH TIME ZONE DEFAULT NULL,
    deleted_on_absn           INTEGER                  DEFAULT NULL,
    deleted_on_ons            INTEGER                  DEFAULT NULL,

    ledger                    VARCHAR(32)              NOT NULL,

    CONSTRAINT services_did_suffix_fk
        FOREIGN KEY (did_suffix) REFERENCES did_data (did_suffix),

    CONSTRAINT unique_id_and_did_suffix_on_services UNIQUE (did_suffix, id)

);

CREATE TABLE service_endpoints
(
    service_endpoint_id ID_TYPE PRIMARY KEY NOT NULL,
    index               INTEGER             NOT NULL,
    service_id          ID_TYPE             NOT NULL,
    url                 TEXT                NOT NULL,

    CONSTRAINT service_endpoints_service_id_fk
        FOREIGN KEY (service_id) REFERENCES services (service_id)


);
