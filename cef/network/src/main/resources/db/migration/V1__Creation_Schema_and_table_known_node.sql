create schema if not exists cef;


create table if not exists cef.known_node (
    id char(256) not null primary key,
    discovery_address binary not null,
    discovery_port int not null,
    server_address binary not null,
    server_port int not null,
    capabilities binary not null,
    discovered timestamp not null,
    last_seen timestamp not null
);
