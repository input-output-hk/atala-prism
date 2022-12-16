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
    added_on_osn              INTEGER                  NOT NULL,
    --^ Operation Sequence Number (osn) of the operation that added the key

    revoked_on_transaction_id TRANSACTION_ID           NULL,
    revoked_on                TIMESTAMP WITH TIME ZONE NULL,
    revoked_on_absn           INTEGER                  NULL,
    revoked_on_osn            INTEGER                  NULL,

    ledger                    VARCHAR(32)              NOT NULL,

    CONSTRAINT services_did_suffix_fk
        FOREIGN KEY (did_suffix) REFERENCES did_data (did_suffix)

);

CREATE UNIQUE INDEX unique_did_suffix_and_id_on_non_revoked
    ON services (did_suffix, id) WHERE (revoked_on is NULL);


CREATE TABLE service_endpoints
(
    service_endpoint_id ID_TYPE PRIMARY KEY NOT NULL,
    url_index           INTEGER             NOT NULL,
    service_id          ID_TYPE             NOT NULL,
    url                 TEXT                NOT NULL,

    CONSTRAINT service_endpoints_service_id_fk
        FOREIGN KEY (service_id) REFERENCES services (service_id)

);
