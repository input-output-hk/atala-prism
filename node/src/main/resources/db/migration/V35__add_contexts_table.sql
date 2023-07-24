CREATE TABLE contexts
(
    context_id                ID_TYPE PRIMARY KEY      NOT NULL,
    did_suffix                ID_TYPE                  NOT NULL,
    context                   TEXT                     NOT NULL,
    added_on_transaction_id   TRANSACTION_ID           NOT NULL,
    added_on                  TIMESTAMP WITH TIME ZONE NOT NULL,
    added_on_absn             INTEGER                  NOT NULL,
    --^ Atala Block Sequence Number (absn) of the operation that added the context string
    added_on_osn              INTEGER                  NOT NULL,
    --^ Operation Sequence Number (osn) of the operation that added the context string

    revoked_on_transaction_id TRANSACTION_ID           NULL,
    revoked_on                TIMESTAMP WITH TIME ZONE NULL,
    revoked_on_absn           INTEGER                  NULL,
    revoked_on_osn            INTEGER                  NULL
);

CREATE UNIQUE INDEX unique_did_suffix_and_context_string_on_non_revoked
    ON contexts (did_suffix, context) WHERE (revoked_on is NULL);