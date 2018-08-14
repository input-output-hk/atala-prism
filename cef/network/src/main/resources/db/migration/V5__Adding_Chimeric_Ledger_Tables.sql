create table cef.chimeric_ledger_state_entry(
    id bigint auto_increment not null primary key,
    string_id varchar(max) not null,
    unique(string_id)
);

create table cef.chimeric_ledger_state_address(
    id bigint not null primary key,
    address varchar(max) not null,
    foreign key (id) references cef.chimeric_ledger_state_entry(id)
);

create table cef.chimeric_ledger_state_utxo(
    id bigint not null primary key,
    tx_id varchar(max) not null,
    index int not null,
    unique (tx_id, index),
    foreign key (id) references cef.chimeric_ledger_state_entry(id)
);

create table cef.chimeric_ledger_state_currency(
    id bigint not null primary key,
    currency varchar(12) not null,
    unique (currency),
    foreign key (id) references cef.chimeric_ledger_state_entry(id)
);

create table cef.chimeric_value_entry(
    ledger_state_entry_id bigint not null,
    currency varchar(12) not null,
    amount decimal(25,5) not null,
    primary key(ledger_state_entry_id, currency),
    foreign key(ledger_state_entry_id) references cef.chimeric_ledger_state_entry(id)
);
