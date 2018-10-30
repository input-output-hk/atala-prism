create table if not exists cef.data_table (
    id bigint auto_increment not null primary key,
    data_table_id varchar(1024) not null,
    data_item_id varchar(1024) not null,
    data_item varbinary(5096) not null,
    UNIQUE(data_item_id, data_table_id)
);

create table if not exists cef.data_item_signature (
  data_item_unique_id bigint not null,
  signature varbinary(5096) not null,
  signing_public_key varbinary(5096) not null,
  foreign key (data_item_unique_id) references cef.data_table(id)
);

create table if not exists cef.data_item_owner (
  data_item_unique_id bigint not null,
  signing_public_key varbinary(5096) not null,
  foreign key (data_item_unique_id) references cef.data_table(id)
);
