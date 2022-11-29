CREATE TABLE service_endpoints
(
    service_endpoint_id     ID_TYPE PRIMARY KEY      NOT NULL,
    service_id              ID_TYPE                  NOT NULL,
    url                     TEXT                     NOT NULL,

    added_on_transaction_id TRANSACTION_ID           NOT NULL,
    added_on                TIMESTAMP WITH TIME ZONE NOT NULL,
    added_on_absn           INTEGER                  NOT NULL,
    --^ Atala Block Sequence Number (absn) of the operation that added the key
    added_on_ons            INTEGER                  NOT NULL,
    --^ Operation Sequence Number (osn) of the operation that added the key
    deleted_by_update       BOOLEAN DEFAULT FALSE,
    deleted_by_revocation   BOOLEAN DEFAULT FALSE,

    deleted_on              TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_on_absn         INTEGER                  NOT NULL,
    deleted_on_ons          INTEGER                  NOT NULL,


    CONSTRAINT service_endpoints_service_id_fk
        FOREIGN KEY (service_id) REFERENCES services (service_id)

);
