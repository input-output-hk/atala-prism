create table if not exists cef.data_table (
    data_item_id bigint auto_increment not null primary key,
    table_id varchar(512) not null,
    data_item varbinary(5096) not null,
    UNIQUE(table_id, data_item)
);

create table if not exists cef.data_item_signature (
  data_item_id bigint not null,
  signature varbinary(5096) not null,
  signing_public_key varbinary(5096) not null,
  foreign key (data_item_id) references cef.data_table(data_item_id)
);

create table if not exists cef.data_item_owner (
  data_item_id bigint not null,
  signing_public_key varbinary(5096) not null,
  foreign key (data_item_id) references cef.data_table(data_item_id)
);
