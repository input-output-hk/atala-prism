create table if not exists cef.data_table (
    id bigint autoincrement not null primary key,
    table_id varchar(1024) not null,
    data_item_id varchar(1024) not null,
    data_item varbinary(5096) not null,
    UNIQUE (table_id, data_item_id)
);

create table if not exists cef.data_item_signature (
  data_table_id bigint not null,
  signature varbinary(5096) not null,
  signing_public_key varbinary(5096) not null,
  foreign key (data_table_id) references cef.data_table(id)
);

create table if not exists cef.data_item_owner (
  data_table_id bigint not null,
  signing_public_key varbinary(5096) not null,
  foreign key (data_table_id) references cef.data_table(id)
);
