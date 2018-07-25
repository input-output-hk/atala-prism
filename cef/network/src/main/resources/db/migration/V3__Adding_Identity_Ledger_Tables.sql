create table if not exists cef.identity_ledger_state (
    id bigint auto_increment not null primary key,
    identity varchar(256) not null,
    public_key varbinary(256) not null,
    UNIQUE(identity, public_key)
);
