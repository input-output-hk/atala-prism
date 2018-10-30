create table if not exists cef.data_table (
    data_item_id varchar(1024) not null primary key,
    data_item varbinary(5096) not null
);

create table if not exists cef.data_item_signature (
  data_item_id varchar(1024) not null,
  signature varbinary(5096) not null,
  signing_public_key varbinary(5096) not null,
  foreign key (data_item_id) references cef.data_table(data_item_id)
);

create table if not exists cef.data_item_owner (
  data_item_id varchar(1024) not null,
  signing_public_key varbinary(5096) not null,
  foreign key (data_item_id) references cef.data_table(data_item_id)
);
