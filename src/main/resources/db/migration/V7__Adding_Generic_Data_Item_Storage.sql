create table if not exists cef.data_table (
    table_id varchar(512) not null,
    data_item varbinary(5096) not null
);