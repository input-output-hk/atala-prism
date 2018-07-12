create table if not exists cef.identity_ledger_block (
    id bigint auto_increment not null primary key,
    created timestamp not null,
    hash varbinary(256) not null,
    UNIQUE(hash)
);

create table if not exists cef.identity_ledger_transaction (
    id bigint auto_increment not null primary key,
    block_id bigint not null,
    tx_type int not null,
    identity varchar(256) not null,
    public_key varbinary(256) not null,
    foreign key(block_id) references cef.identity_ledger_block(id)
);

create table if not exists cef.identity_ledger_state (
    id bigint auto_increment not null primary key,
    identity varchar(256) not null,
    public_key varbinary(256) not null,
    UNIQUE(identity, public_key)
);
