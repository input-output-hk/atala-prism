-- VDR storage entries
CREATE TYPE public.vdr_entry_status AS ENUM ('ACTIVE', 'DEACTIVATED');

CREATE TABLE public.vdr_entries
(
    event_hash           public.operation_hash        NOT NULL,
    did_suffix           public.id_type               NOT NULL,
    nonce                BYTEA                        NULL,
    data_type            varchar(16)                  NOT NULL,
    data_bytes           BYTEA                        NULL,
    data_ipfs            varchar(256)                 NULL,
    previous_event_hash  public.operation_hash        NULL,
    status               public.vdr_entry_status      NOT NULL DEFAULT 'ACTIVE',
    created_at           timestamptz                  NOT NULL,
    created_at_absn      int4                         NOT NULL,
    created_at_osn       int4                         NOT NULL,
    created_at_tx_id     public.transaction_id        NOT NULL,
    created_at_ledger    varchar(32)                  NOT NULL,
    CONSTRAINT vdr_entries_pk PRIMARY KEY (event_hash),
    CONSTRAINT vdr_entries_did_fk FOREIGN KEY (did_suffix) REFERENCES public.did_data (did_suffix)
);

CREATE INDEX vdr_entries_did_suffix_idx ON public.vdr_entries USING btree (did_suffix);
CREATE INDEX vdr_entries_prev_event_idx ON public.vdr_entries USING btree (previous_event_hash);
