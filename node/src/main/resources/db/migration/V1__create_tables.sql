CREATE SCHEMA IF NOT EXISTS PUBLIC
    AUTHORIZATION postgres;

CREATE DOMAIN public.atala_object_id AS BYTEA CONSTRAINT atala_object_id_check CHECK (length(VALUE) = 32);


CREATE TYPE public.atala_object_status AS ENUM ('SCHEDULED', 'PENDING', 'MERGED', 'PROCESSED');


CREATE TYPE public.atala_object_transaction_status AS ENUM ('PENDING', 'DELETED', 'IN_LEDGER');


CREATE DOMAIN public.atala_operation_id AS BYTEA CONSTRAINT atala_operation_id_check CHECK (length(VALUE) = 32);


CREATE TYPE public.atala_operation_status AS ENUM ('UNKNOWN', 'RECEIVED', 'APPLIED', 'REJECTED');


CREATE DOMAIN public.block_hash_type AS BYTEA CONSTRAINT block_hash_type_check CHECK (length(VALUE) = 32);


CREATE DOMAIN public.block_no AS integer CONSTRAINT block_no_check CHECK (VALUE >= 0);


CREATE DOMAIN public.blockhash_type AS BYTEA CONSTRAINT blockhash_type_check CHECK (length(VALUE) = 32);


CREATE DOMAIN public.content_hash AS BYTEA CONSTRAINT content_hash_check CHECK (length(VALUE) = 32);


CREATE DOMAIN public.did AS text COLLATE "default" CONSTRAINT did_check CHECK (VALUE ~ '^did:[a-z0-9]+:[a-zA-Z0-9._-]*(:[a-zA-Z0-9._-]*)*$'::text);


CREATE DOMAIN public.id_type AS text COLLATE "default" CONSTRAINT id_type_check CHECK (VALUE ~ '^[0-9a-f]{64}$'::text);


CREATE TYPE public.key_usage AS ENUM ('MASTER_KEY', 'ISSUING_KEY', 'KEY_AGREEMENT_KEY', 'AUTHENTICATION_KEY', 'REVOCATION_KEY', 'CAPABILITY_INVOCATION_KEY', 'CAPABILITY_DELEGATION_KEY');


CREATE DOMAIN public.non_negative_int_type AS integer CONSTRAINT non_negative_int_type_check CHECK (VALUE >= 0);


CREATE DOMAIN public.operation_hash AS BYTEA CONSTRAINT operation_hash_check CHECK (length(VALUE) = 32);


CREATE DOMAIN public.transaction_id AS BYTEA CONSTRAINT transaction_id_check CHECK (length(VALUE) = 32);


CREATE TABLE public.atala_objects
(
    atala_object_id     public.atala_object_id                                            NOT NULL,
    object_content      BYTEA                                                             NOT NULL,
    received_at         timestamptz                                                       NOT NULL,
    atala_object_status public.atala_object_status DEFAULT 'PENDING'::atala_object_status NULL,
    CONSTRAINT atala_objects_pk PRIMARY KEY (atala_object_id)
);


CREATE INDEX atala_objects_atala_object_status_index ON public.atala_objects USING btree (atala_object_status);


CREATE INDEX atala_objects_received_at ON public.atala_objects USING btree (received_at);


CREATE TABLE public.contexts
(
    context_id                public.id_type        NOT NULL,
    did_suffix                public.id_type        NOT NULL,
    context                   text                  NOT NULL,
    added_on_transaction_id   public.transaction_id NOT NULL,
    added_on                  timestamptz           NOT NULL,
    added_on_absn             int4                  NOT NULL,
    added_on_osn              int4                  NOT NULL,
    revoked_on_transaction_id public.transaction_id NULL,
    revoked_on                timestamptz           NULL,
    revoked_on_absn           int4                  NULL,
    revoked_on_osn            int4                  NULL,
    CONSTRAINT contexts_pkey PRIMARY KEY (context_id)
);


CREATE UNIQUE INDEX unique_did_suffix_and_context_string_on_non_revoked ON public.contexts USING btree (did_suffix, context)
    WHERE (revoked_on IS NULL);


CREATE TABLE public.did_data
(
    did_suffix        public.id_type        NOT NULL,
    last_operation    public.operation_hash NOT NULL,
    transaction_id    public.transaction_id NOT NULL,
    ledger            varchar(32)           NOT NULL,
    published_on      timestamptz           NOT NULL,
    published_on_absn int4                  NOT NULL,
    published_on_osn  int4                  NOT NULL,
    CONSTRAINT did_data_pk PRIMARY KEY (did_suffix)
);


CREATE TABLE public.did_request_nonces
(
    request_nonce BYTEA      NOT NULL,
    did           public.did NOT NULL,
    CONSTRAINT did_request_nonces_pk PRIMARY KEY (request_nonce,
                                                  did)
);


CREATE TABLE public.key_values
(
    "key" varchar(64) NOT NULL,
    value text        NULL,
    CONSTRAINT key_values_pkey PRIMARY KEY (KEY)
);


CREATE TABLE public.metrics_counters
(
    counter_name  varchar(256)                           NOT NULL,
    counter_value public.non_negative_int_type DEFAULT 0 NOT NULL,
    CONSTRAINT metrics_counters_pkey PRIMARY KEY (counter_name)
);


CREATE TABLE public.protocol_versions
(
    major_version   public.non_negative_int_type NOT NULL,
    minor_version   public.non_negative_int_type NOT NULL,
    version_name    varchar(256)                 NULL,
    effective_since public.block_no              NOT NULL,
    published_in    public.transaction_id        NOT NULL,
    is_effective    bool                         NOT NULL,
    proposer_did    public.id_type               NOT NULL,
    CONSTRAINT protocol_version_pk PRIMARY KEY (major_version,
                                                minor_version)
);


CREATE TABLE public.public_keys
(
    did_suffix                public.id_type        NOT NULL,
    key_id                    text                  NOT NULL,
    key_usage                 public.key_usage      NOT NULL,
    curve                     text                  NOT NULL,
    added_on                  timestamptz           NOT NULL,
    added_on_absn             int4                  NOT NULL,
    added_on_osn              int4                  NOT NULL,
    revoked_on                timestamptz           NULL,
    revoked_on_absn           int4                  NULL,
    revoked_on_osn            int4                  NULL,
    added_on_transaction_id   public.transaction_id NOT NULL,
    revoked_on_transaction_id public.transaction_id NULL,
    ledger                    varchar(32)           NOT NULL,
    compressed                BYTEA                 NOT NULL,
    CONSTRAINT public_keys_pk PRIMARY KEY (did_suffix,
                                           key_id),
    CONSTRAINT x_compressed_length CHECK ((length(compressed) = 33))
);


CREATE TABLE public.atala_object_tx_submissions
(
    atala_object_id      public.atala_object_id                 NOT NULL,
    ledger               varchar(32)                            NOT NULL,
    transaction_id       public.transaction_id                  NOT NULL,
    submission_timestamp timestamptz                            NOT NULL,
    status               public.atala_object_transaction_status NOT NULL,
    CONSTRAINT atala_object_tx_submissions_pk PRIMARY KEY (ledger,
                                                           transaction_id),
    CONSTRAINT atala_object_tx_submissions_atala_object_id_fk
        FOREIGN KEY (atala_object_id) REFERENCES public.atala_objects (atala_object_id)
);


CREATE INDEX atala_object_tx_submissions_atala_object_id_index ON public.atala_object_tx_submissions USING hash (atala_object_id);


CREATE INDEX atala_object_tx_submissions_filter_index ON public.atala_object_tx_submissions USING btree (submission_timestamp, status, ledger);


CREATE INDEX atala_object_tx_submissions_latest_index ON public.atala_object_tx_submissions USING btree (atala_object_id, submission_timestamp);


CREATE TABLE public.atala_object_txs
(
    atala_object_id public.atala_object_id NOT NULL,
    ledger          varchar(32)            NOT NULL,
    block_number    int4                   NOT NULL,
    block_timestamp timestamptz            NOT NULL,
    block_index     int4                   NOT NULL,
    transaction_id  public.transaction_id  NOT NULL,
    CONSTRAINT atala_object_txs_pk PRIMARY KEY (atala_object_id),
    CONSTRAINT atala_object_txs_atala_object_id_fk
        FOREIGN KEY (atala_object_id) REFERENCES public.atala_objects (atala_object_id)
);


CREATE INDEX atala_object_txs_atala_object_id_index ON public.atala_object_txs USING hash (atala_object_id);


CREATE TABLE public.atala_operations
(
    signed_atala_operation_id public.atala_operation_id                  NOT NULL,
    atala_object_id           public.atala_object_id                     NOT NULL,
    atala_operation_status    public.atala_operation_status              NOT NULL,
    status_details            varchar(256) DEFAULT ''::CHARACTER varying NULL,
    CONSTRAINT signed_atala_operation_id_pk PRIMARY KEY (signed_atala_operation_id),
    CONSTRAINT atala_object_id_fk
        FOREIGN KEY (atala_object_id) REFERENCES public.atala_objects (atala_object_id)
);


CREATE TABLE public.services
(
    service_id                public.id_type        NOT NULL,
    id                        text                  NOT NULL,
    did_suffix                public.id_type        NOT NULL,
    "type"                    text                  NOT NULL,
    added_on_transaction_id   public.transaction_id NOT NULL,
    added_on                  timestamptz           NOT NULL,
    added_on_absn             int4                  NOT NULL,
    added_on_osn              int4                  NOT NULL,
    revoked_on_transaction_id public.transaction_id NULL,
    revoked_on                timestamptz           NULL,
    revoked_on_absn           int4                  NULL,
    revoked_on_osn            int4                  NULL,
    ledger                    varchar(32)           NOT NULL,
    service_endpoints         text                  NOT NULL,
    CONSTRAINT services_pkey PRIMARY KEY (service_id),
    CONSTRAINT services_did_suffix_fk
        FOREIGN KEY (did_suffix) REFERENCES public.did_data (did_suffix)
);


CREATE UNIQUE INDEX unique_did_suffix_and_id_on_non_revoked ON public.services USING btree (did_suffix, id)
    WHERE (revoked_on IS NULL);


CREATE OR REPLACE FUNCTION public.random_bytea(p_length integer) RETURNS BYTEA
    LANGUAGE PLPGSQL AS
$function$
declare
    o bytea := '';
begin
    for i in 1..p_length
        loop
            o := o || decode(lpad(to_hex(width_bucket(random(), 0, 1, 256) - 1), 2, '0'), 'hex');
        end loop;
    return o;
end;
$function$;
