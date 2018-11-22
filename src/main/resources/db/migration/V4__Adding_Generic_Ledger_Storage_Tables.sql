create table if not exists cef.ledger_state_entry (
    ledger_state_id varchar(512) not null,
    partition_id varchar(max) not null,
    data varbinary(5096) not null,
    primary key(partition_id, ledger_state_id)
);
