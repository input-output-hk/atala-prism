create table if not exists cef.identity_ledger (
    id bigint auto_increment not null primary key,
    name varchar(128) not null
);

create table if not exists cef.identity_ledger_block (
    id bigint auto_increment not null primary key,
    ledger_id bigint not null,
    foreign key(ledger_id) references cef.identity_ledger(id)
);

create table if not exists cef.identity_ledger_transaction (
    id bigint auto_increment not null primary key,
    type int not null,
    name varchar(32) not null,
    identity varchar(256) not null,
    public_key varbinary(256) not null
);

create table if not exists cef.identity_ledger_state (
    id bigint auto_increment not null primary key,
    identity varchar(256) not null,
    public_key varbinary(256) not null,
    UNIQUE(identity, public_key)
);

--create table if not exists cef.identity_ledger_state_key (
--    id bigint auto_increment not null primary key,
--    identity_id bigint not null,
--    public_key varbinary(256) not null,
--    foreign key (identity_id) references cef.identity_ledger_state_id(id)
--);
