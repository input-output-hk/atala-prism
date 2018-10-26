create table if not exists cef.ledger_block (
    id bigint auto_increment not null primary key,
    ledger_id varchar(512) not null,
    block_number bigint not null,
    previous_block_id bigint null,
    created_on timestamp not null,
    data varbinary(5096) not null,
    unique(block_number, ledger_id),
    foreign key(previous_block_id) references cef.ledger_block(id)
);
